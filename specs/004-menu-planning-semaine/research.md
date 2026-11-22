# Research: Menu en planning à la semaine

Aucun point « NEEDS CLARIFICATION » n'est resté ouvert dans le spec ; ce document fixe les choix techniques.

## R1 - Représentation du repas

**Decision**: colonne `meal INTEGER` nullable dans `menu_entries` ; 1 = petit-déjeuner, 2 = midi, 3 = soir ; `null` = sans repas. Constantes dans `MenuEntry.Companion` (`MEAL_BREAKFAST`, `MEAL_LUNCH`, `MEAL_DINNER`), noms français dans `MenuLogic.mealName`.

**Rationale**: même convention que `day` (entier 1..7, null = aucun) ; pas de converter Room ; tri naturel par valeur.

**Alternatives**: enum + `TypeConverter` (plus de code) ; texte (« lunch ») : tri et validation plus lourds.

## R2 - Règle « pas de repas sans jour » (FR-002)

**Decision**: garantie à deux niveaux. (1) Le DAO écrit toujours le couple : `setPlacement(id, day, meal)` met `meal = NULL` si `day IS NULL` ; `setDay(id, null)` efface aussi le repas. (2) `MenuLogic.normalizePlacement(day, meal)` (fonction pure) est appelée par le repository (valide 1..7 et 1..3). En lecture, une ligne `day = NULL, meal != NULL` (impossible normalement) est traitée comme « À placer ».

**Rationale**: une seule requête SQL atomique pour déplacer ; aucune fenêtre où le jour est changé et le repas pas.

**Alternatives**: contrainte `CHECK` SQL : impossible à ajouter par `ALTER TABLE` sans recréer la table, donc risque pour les données.

## R3 - Migration Room

**Decision**: `ALTER TABLE menu_entries ADD COLUMN meal INTEGER` (nullable, sans `DEFAULT`). Les lignes existantes reçoivent `NULL` ; rien d'autre n'est touché. Le schéma attendu par Room pour `val meal: Int? = null` est `` `meal` INTEGER `` : pas de `defaultValue` dans l'annotation, sinon Room rejette la base (schéma différent). Le projet n'exporte pas de schémas : le test de migration crée la base à l'ancienne version en SQL brut, comme `migration_6_to_7`.

**Numérotation**: voir plan.md, « Ordre des versions » : prise dans l'ordre de réalisation, 003 (étapes 1-2) d'abord.

**Alternatives**: `fallbackToDestructiveMigration` : exclu (perte de données) ; recréer la table : inutile et risqué.

## R4 - Mode « choix » de la page Recherche

**Decision**: réutiliser `SearchScreen` avec un paramètre facultatif `pick: SearchPick? = null` (`SearchPick(label: String, alreadyInMenu: Set<String>)`). Quand `pick != null` : bandeau en haut avec le libellé et « Annuler » ; un clic sur une recette appelle `onRecipeClick` (qui, dans la route de choix, ajoute au menu et revient) au lieu d'ouvrir la fiche ; pastille « déjà au menu » sur les recettes de `alreadyInMenu`. Sans `pick`, aucun changement (US5 sc. 3).

Navigation : nouvelle route `search_pick?day={day}&meal={meal}` (arguments entiers, défaut -1 = absent). Le clic appelle `menuVm.add(recipe, day, meal)` puis `navController.popBackStack()` ; Retour système et « Annuler » font `popBackStack()` seulement. La route n'est pas dans `MAIN_ROUTES` : la barre du bas est masquée pendant le choix, ce qui évite de quitter le mode par un autre onglet (à confirmer à l'implémentation en lisant l'usage de `MAIN_ROUTES`).

**Rationale**: pas d'état partagé entre écrans (ni `savedStateHandle`) : `menuVm` est déjà accessible à `MainActivity`. L'écran de recherche est le même code : photos, suggestions orthographiques, filtres, recherche titre/ingrédient/tag inchangés.

**Alternatives**: second composable de recherche dédié (duplication) ; résultat via `savedStateHandle` (plus de code).

**Point d'attention**: `SearchScreen` garde `query` et `recentSearches` en `remember` simple : perdus à la rotation comme aujourd'hui. Le libellé du bandeau vient des arguments de route, donc survit à la rotation. Les résultats actuels de la recherche affichent déjà la photo (`AsyncImage`) ; la page « parcourir » (catégories, filtres) ne liste pas de recettes, c'est le comportement actuel conservé.

## R5 - Vue Semaine

**Decision**: composable `WeekView` sur la même liste d'entrées que la vue Liste, regroupée par `MenuLogic.groupByWeek(entries)` -> `WeekPlan` (voir data-model.md). Rien n'est dupliqué en base : la vue est une projection. Grille en `Column` de 7 lignes (`Row` : nom du jour + 3 cases de même poids) dans un `verticalScroll`, « À placer » en dessous. Pas de `LazyVerticalGrid` : 21 cases fixes. Case : 2 titres au plus puis « +N » ; clic sur case vide ou sur « + » = ajouter ; clic sur un titre = feuille d'entrée.

**Édition d'une entrée** : `AlertDialog` (comme le reste de l'écran) avec titre complet, stepper de parts/fois (réutilise `Stepper`), sélecteurs jour et repas, « Ouvrir la recette », « Retirer du menu ». Déplacer = choisir jour + repas puis valider (3 gestes : titre, case, valider ; SC-003).

**Pas de glisser-déposer** (Assumptions du spec).

**Largeur** : 1 colonne de jour (~48 dp) + 3 colonnes `weight(1f)` ; titres sur 2 lignes max, `TextOverflow.Ellipsis`, 11-12 sp.

## R6 - Remplacement de `AddRecipeDialog`

**Decision**: la fenêtre est retirée de `MenuView` ; `MenuActions.onAdd: (Recipe) -> Unit` est remplacé par `onPickRecipe: (day: Int?, meal: Int?) -> Unit` (ouvre la route de choix). L'ajout lui-même est fait par `MainActivity` (route `search_pick`). `AddRecipeDialog` est supprimée ; `groupByCategory` (fonction publique de `MenuScreen.kt`) est conservée car `GroupByCategoryTest` (JVM) la couvre : la supprimer avec son test serait une décision à part, à signaler.

**Impact tests**: `MenuScreenTest` passait par `add_recipe` puis `add_search` / `add_result_*` dans la fenêtre : ces cas sont réécrits (`add_recipe` déclenche `onPickRecipe`), et le parcours de choix est couvert par `SearchPickModeTest` (composant `SearchScreen` seul) et l'essai téléphone. À signaler dans le compte rendu de `/verifie-tests` : comportement remplacé voulu, pas un test affaibli.

## R7 - Préférence de vue

**Decision**: `SharedPreferences` `menu_prefs`, clé `menu_view` (`"list"` | `"week"`, défaut `"list"`), lue par `MainActivity` (état Compose) et passée à `MenuScreen` (`viewMode`, `onViewMode`). Pas dans Room.

**Rationale**: préférence d'affichage, pas une donnée ; évite une migration ; `rememberSaveable` seul ne survivrait pas à la fermeture de l'app (US1 sc. 4).

## R8 - Retirer les jours

**Decision**: `@Query("UPDATE menu_entries SET day = NULL, meal = NULL")`, une seule requête (atomique) ; confirmation par `AlertDialog` (`clear_days_confirm`). Aucune ligne supprimée ; `shopping_checks` et `shopping_extras` non touchés. « Vider le menu » inchangé.

## R9 - Liste de courses inchangée

**Decision**: aucune modification de `ShoppingListBuilder`, `ShoppingItem` ni de `MenuViewModel.shopping`. Garde-fou : un test JVM construit les mêmes `MenuEntry` avec et sans jour/repas et compare `ShoppingListBuilder.build` (égalité des `ShoppingItem`). Le flux `shopping` se recalcule quand `entries` change (déjà `combine`), le résultat est identique ; les coches sont des clés d'article, indépendantes du placement.

## R10 - Tri dans la vue Liste (FR-005)

**Decision**: `MenuLogic.groupByDay` trie à l'intérieur d'un jour par repas (1, 2, 3, puis `null`), puis `position`, puis `createdAt`. Libellé : `MenuLogic.dayMealLabel(day, meal)` -> « Lundi · Midi ». Les groupes restent lundi -> dimanche puis « Sans jour » ; le test existant `groupByDay_mondayToSundayThenNoDay` doit rester vert sans modification.

## R11 - Rotation et retour arrière

Vue active : préférence (R7). Onglet Menu/Courses : `rememberSaveable` (déjà en place). Bandeau du mode choix : arguments de route. Feuille d'entrée : identifiant d'entrée en `rememberSaveable`. Après un ajout, `popBackStack()` revient sur l'écran Menu, onglet Menu et vue d'origine conservés.
