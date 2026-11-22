# Tasks: Menu en planning à la semaine

**Input**: Design documents from `/specs/004-menu-planning-semaine/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (week-view-ui.md, search-pick-mode.md, test-tags.md), quickstart.md

**Tests**: Inclus. La règle permanente de l'utilisatrice impose `/verifie-tests` avant de déclarer le travail terminé ; la spec fixe des critères mesurables (SC-001 à SC-005) et le plan exige un test de migration.

**Organization**: Tâches groupées par user story. Ordre de réalisation (les priorités P1 passent avant P2, mais US5 précède US2 car la case vide ouvre la page Recherche) : Foundational (repas en base) → US1 (voir) → US5 (choisir avec la Recherche) → US2 (placer, déplacer, retirer) → US3 (liste de courses inchangée, garde-fous) → US4 (retirer les jours, P2).

Abréviations de chemins :
- `APP` = `app/src/main/java/com/example/recipelibrary`
- `UT` = `app/src/test/java/com/example/recipelibrary`
- `IT` = `app/src/androidTest/java/com/example/recipelibrary`

Règles pour toutes les tâches : tests sur le téléphone par `am instrument` uniquement (jamais `connectedAndroidTest`), `adb install -r` (garde les données), `url_import.xml` sauvegardé (taille vérifiée) puis restauré, volume média du téléphone coupé pendant les tests puis remis ; aucune écriture dans la table `recipes` ; liste de courses (`ShoppingListBuilder`, `ShoppingItem`, `MenuViewModel.shopping`) non modifiée ; aucun test existant affaibli, désactivé ou réaligné sans le signaler ; rien de commité sans demande.

## Modifications de tests existants à SIGNALER à l'utilisatrice (jamais en silence)

1. **`IT/MenuScreenTest.kt` : 6 cas sur 7 réécrits parce que la petite fenêtre d'ajout est remplacée par le mode « choix » de la page Recherche** (changement de comportement VOULU, FR-007b ; ce n'est pas un affaiblissement). Cas concernés (tous passaient par `addBySearch` : `add_recipe` puis `add_search` puis `add_result_<id>`) : `addBySearch_thenChangeServings`, `recipeWithoutServings_timesThenSetRecipeServings`, `setRecipeServings_outOfRangeRefused`, `chooseDay_groupsByDay_thenRemoveDay`, `sameRecipeTwice_twoEntries_thenRemoveOne`, `clearMenu_asksConfirmation`. Seul `emptyMenu_showsInvitation` est inchangé. Règle de réécriture : on ne change que le harnais (`show()` : `onAdd` devient `onPickRecipe`) et l'assistant `addBySearch` (devient `addViaPick`) ; **les assertions de chaque cas restent identiques**. Le parcours « fenêtre → résultat » disparu est couvert par `SearchPickModeTest` (écran de recherche seul) et par l'essai téléphone ; la jonction route `search_pick` → `menuVm.add` n'est couverte que par l'essai téléphone (T048).
2. **`IT/DatabaseMigrationTest.kt`** : seule modification des tests existants = ajout de `MIGRATION_9_10` à chaque `addMigrations(...)`.
3. Tags de test retirés avec la fenêtre d'ajout : `add_search`, `add_list`, `add_cat_<cat>`, `add_result_<id>`.

## Format: `[ID] [P?] [Story] Description`

- **[P]** : parallélisable (fichiers différents, pas de dépendance sur une tâche en cours)
- **[Story]** : user story concernée (US1 à US5)

---

## Phase 1: Setup

**Purpose**: vérifier les prérequis et protéger les données avant de toucher au schéma.

- [ ] T001 Vérifier que `APP/data/AppDatabase.kt` est en `version = 9` (8 = feature 001 T046, 9 = feature 003 étapes 1 et 2). **Si ce n'est pas 9, STOP** : les migrations précédentes ne sont pas livrées, la 004 attend. Vérifier aussi dans `APP/MainActivity.kt` que `MAIN_ROUTES` pilote la barre du bas (ligne `currentRoute in MAIN_ROUTES`) : la route `search_pick` la masquera en n'y figurant pas
- [ ] T002 Baseline : lancer `./gradlew testDebugUnitTest` avant tout changement et noter le nombre de tests verts (référence pour T038)
- [ ] T003 Sauvegarder sur le téléphone, avant toute réinstallation : `url_import.xml` (copie + taille vérifiée par `adb shell ls -l`) et la base `recipes.db` (copie `adb`) pour l'essai de migration réelle de T047 ; noter le volume média actuel

---

## Phase 2: Foundational (bloquant pour toutes les stories)

**Purpose**: colonne `meal`, migration additive, DAO, repository, logique pure. Rien d'autre ne peut avancer sans ceci. Tests écrits d'abord.

- [ ] T004 [P] Tests JVM dans `UT/menu/MenuLogicTest.kt` (cas ajoutés, existants inchangés dont `groupByDay_mondayToSundayThenNoDay` qui doit rester vert tel quel) : `mealName` (1 « Petit-déjeuner », 2 « Midi », 3 « Soir ») ; `dayMealLabel` (« Lundi · Midi », « Lundi » sans repas, « Sans jour ») ; `normalizePlacement(day, meal)` : jour hors 1..7 refusé, repas hors 1..3 refusé, `day = null` force `meal = null` (FR-002 : « un repas sans jour n'est pas permis ») ; `groupByDay` trie dans un jour par repas 1, 2, 3 puis `null`, puis `position`, puis `createdAt` ; `groupByWeek` : toute entrée dans exactement une des trois parties de `WeekPlan` (aucune absente, aucune en double), jour sans repas dans `noMeal`, sans jour dans `unplaced`, état impossible `day = null, meal != null` traité comme « À placer », ordre `position` puis `createdAt`, même recette deux fois dans une case = 2 entrées
- [ ] T005 [P] Test de migration écrit AVANT la migration, dans `IT/DatabaseMigrationTest.kt` : `migration_9_to_10_keepsMenuEntries_mealNull`, sur le modèle de `migration_6_to_7_...`. Créer à la main une base en version 9 (tables `recipes`, `pending_imports`, `menu_entries` sans `meal`, `shopping_checks`, `shopping_extras`, plus colonnes et tables ajoutées en versions 8 et 9) ; insérer 2 recettes, 3 entrées (une avec parts + jour, une avec `times` sans jour, une même recette en double), 2 coches, 2 articles manuels dont un coché ; ouvrir avec Room et `addMigrations(... MIGRATION_9_10)` ; vérifier recettes identiques (égalité complète), 3 entrées avec mêmes `servings`, `times`, `day`, `position`, `created_at` et `meal == null` (SC-004, FR-008), coches et articles manuels identiques, liste de courses calculée sur ces entrées identique à celle d'avant ; puis `setPlacement` suivi d'une relecture. Ajouter `MIGRATION_9_10` à chaque `addMigrations(...)` existant du fichier (modification à signaler, voir plus haut)
- [ ] T006 [P] Tests dans `IT/MenuDaoTest.kt` (cas ajoutés) : `setPlacement(id, d, m)` écrit jour et repas ; `setPlacement(id, null, m)` n'écrit pas de repas ; `setPlacement(id, d, null)` garde le jour sans repas ; `setDay(id, null)` efface le repas, `setDay(id, d)` le garde ; `clearPlacements()` met `day` et `meal` à NULL pour toutes les lignes, ne supprime aucune ligne, ne touche ni coche ni article manuel et ne change ni `servings`, `times`, `position` ni `created_at` ; un placement ne change ni parts ni `times`
- [ ] T007 Dans `APP/data/menu/MenuEntities.kt` : ajouter à `MenuEntry` le champ `val meal: Int? = null` (colonne `meal`, nullable, **pas de `defaultValue`** : Room rejetterait la base) et `companion object { const val MEAL_BREAKFAST = 1; const val MEAL_LUNCH = 2; const val MEAL_DINNER = 3 }` (1 = petit-déjeuner, 2 = midi, 3 = soir ; null = sans repas)
- [ ] T008 Dans `APP/data/Migrations.kt` ajouter `MIGRATION_9_10` (`ALTER TABLE menu_entries ADD COLUMN meal INTEGER`, nullable, sans `DEFAULT`, aucune donnée modifiée) ; passer `APP/data/AppDatabase.kt` à `version = 10` ; ajouter `MIGRATION_9_10` à la liste `addMigrations(...)` de `APP/data/DatabaseProvider.kt` ; ne renuméroter ni réécrire aucune migration existante
- [ ] T009 Dans `APP/data/menu/MenuDao.kt` : `@Query("UPDATE menu_entries SET day = :day, meal = CASE WHEN :day IS NULL THEN NULL ELSE :meal END WHERE id = :id") suspend fun setPlacement(id: String, day: Int?, meal: Int?)` ; remplacer `setDay` par `UPDATE menu_entries SET day = :day, meal = CASE WHEN :day IS NULL THEN NULL ELSE meal END WHERE id = :id` ; `@Query("UPDATE menu_entries SET day = NULL, meal = NULL") suspend fun clearPlacements()` (une seule requête atomique chacune)
- [ ] T010 [P] Dans `APP/data/menu/MenuLogic.kt` (tests de T004) : `mealName`, `dayMealLabel`, `normalizePlacement`, tri par repas dans `groupByDay`, `groupByWeek` -> `WeekPlan(cells, noMeal, unplaced)` (data-model.md) ; Kotlin pur ; `groupByCategory` conservée (décision : `GroupByCategoryTest` reste)
- [ ] T011 Dans `APP/data/menu/MenuRepository.kt` : `add(recipe, servings, day = null, meal = null)` (parts par défaut inchangées, `day`/`meal` passés par `normalizePlacement`, position en fin de menu), `setPlacement(id, day, meal)` (refuse jour hors 1..7 et repas hors 1..3 comme `setDay`), `clearPlacements()`
- [ ] T012 Dans `APP/ui/MenuViewModel.kt` : `add(recipe, day: Int? = null, meal: Int? = null)` (les appels existants `menuVm.add(it)` et `menuVm.add(recipe)` de `RecipeDetailScreen` continuent de compiler), `setPlacement(id, day, meal)`, `clearPlacements()` ; le flux `shopping` reste inchangé

**Checkpoint**: base en version 10, tests JVM T004 verts, `MenuDaoTest` et `DatabaseMigrationTest` prêts à passer sur le téléphone en phase 8.

---

## Phase 3: User Story 1 - Voir mon menu en semaine (Priority: P1)

**Goal**: sélecteur Liste / Semaine, grille lundi à dimanche × 3 repas, « Sans repas » par jour, « À placer », repas dans la vue Liste, dernière vue retrouvée.

**Independent Test**: menu « Quiche » (lundi, midi), « Soupe » (lundi, soir), « Crêpes » (mardi, sans repas), « Salade » (sans jour) : Semaine montre Quiche en lundi/midi, Soupe en lundi/soir, Crêpes sur la ligne mardi en « Sans repas », Salade dans « À placer » ; Liste montre les mêmes 4 recettes.

### Tests for User Story 1

- [ ] T013 [P] [US1] Compose UI tests dans `IT/MenuScreenTest.kt` (cas ajoutés) : scénario d'exemple ci-dessus (`week_cell_1_2`, `week_cell_1_3`, `week_nomeal_2`, `week_unplaced`) ; bascule `view_list` / `view_week` montrant les mêmes 4 recettes ; entrées « d'avant » (jour sans repas / sans jour) correctement rangées (US1 sc. 2) ; invariant « toute entrée visible quelque part » ; case avec plusieurs recettes, et case de plus de 2 entrées : 2 titres puis `week_more_<jour>_<repas>` « +N » dont le toucher ouvre la liste de la case (US1 sc. 3) ; semaine vide : message `week_empty` « Touche une case pour y ajouter une recette » ; entrée dont la recette est supprimée affichée « Recette supprimée » ; vue Liste : bouton `day_<id>` affiche « Lundi · Midi » / « Lundi » / « Sans jour » et ordre petit-déjeuner, midi, soir, sans repas dans un jour (US1 sc. 5, FR-005) ; SC-001 : la largeur de `week_grid` ne dépasse pas la largeur de l'écran et les 21 cases existent sans défilement horizontal ; titre long abrégé (ellipsis) ; recréation de l'activité / rotation : la vue choisie est conservée (`StateRestorationTester` ou recréation du contenu)
- [ ] T014 [P] [US1] Test instrumenté `IT/MenuViewPrefTest.kt` : valeur par défaut `LIST` ; écrire `WEEK` puis relire avec une nouvelle instance (simule fermeture et réouverture de l'app) ; valeur inconnue dans le fichier -> `LIST` ; fichier de préférences de test séparé (ne touche pas les réglages réels)

### Implementation for User Story 1

- [ ] T015 [P] [US1] Créer `APP/data/menu/MenuViewPref.kt` : lecture et écriture de `SharedPreferences` `menu_prefs`, clé `menu_view` (`"list"` par défaut, `"week"`)
- [ ] T016 [US1] Dans `APP/ui/MenuScreen.kt` : `enum class MenuViewMode { LIST, WEEK }` ; paramètres `viewMode = MenuViewMode.LIST` et `onViewMode = {}` de `MenuScreen` ; sélecteur segmenté `view_list` / `view_week` sous le TabRow Menu / Courses (onglet Courses inchangé)
- [ ] T017 [US1] Dans `APP/ui/MenuScreen.kt`, vue Liste : le bouton `day_<id>` affiche `MenuLogic.dayMealLabel` ; groupes triés par `MenuLogic.groupByDay` ; titre de groupe `MenuLogic.dayName(day)` ; `day_option_<1..7|0>` conservés
- [ ] T018 [US1] Créer `WeekView` (dans `APP/ui/MenuScreen.kt`, ou `APP/ui/WeekView.kt` si le fichier devient trop long) selon `contracts/week-view-ui.md` : `Column` de 7 lignes (`week_day_<n>` : nom court + 3 cases de même poids) dans un `verticalScroll` (« À placer » sous la grille, décision prise : défilement vertical accepté, grille sans défilement horizontal), `week_grid`, `week_cell_<jour>_<repas>`, `week_entry_<entryId>` (2 titres max sur 2 lignes, ellipsis, 11 à 12 sp), `week_more_<jour>_<repas>` + `AlertDialog` de la case, `week_add_<jour>_<repas>` (« + » d'une case vide ou occupée), `week_nomeal_<jour>` (« Sans repas : titre, titre », seulement s'il y en a), `week_unplaced` (cartes avec titre, seulement s'il y a des entrées sans jour), `week_empty`, « Recette supprimée » ; données de `MenuLogic.groupByWeek` ; les clics sont des lambdas (`onAddCell(day, meal)`, `onEntryClick(id)`) branchées en US5 et US2
- [ ] T019 [US1] Dans `APP/MainActivity.kt` : lire `menu_view` dans l'état Compose, le passer à `MenuScreen` (`viewMode`, `onViewMode` qui écrit la préférence via `MenuViewPref`)

**Checkpoint**: Liste et Semaine montrent les mêmes recettes (lecture seule pour la grille) ; la vue est retrouvée.

---

## Phase 4: User Story 5 - Choisir la recette avec la page Recherche (Priority: P1)

**Goal**: « + » de la vue Liste et cases de la Semaine ouvrent la Recherche en mode « choix » (photos, titre, ingrédient, tag) ; toucher une recette l'ajoute et revient ; la fenêtre `AddRecipeDialog` disparaît.

**Independent Test**: toucher mercredi/soir vide : Recherche avec le bandeau « Choisis une recette pour Mercredi · Soir » et les photos ; taper « quiche », toucher « Quiche lorraine » : retour à la Semaine, quiche dans mercredi/soir. « Annuler » : rien ajouté.

### Tests for User Story 5

- [ ] T020 [P] [US5] Test JVM dans `UT/menu/MenuLogicTest.kt` : libellé du bandeau `MenuLogic.pickBannerLabel(day, meal)` : sans case « Choisis une recette pour le menu » ; jour et repas « Choisis une recette pour Mercredi · Soir » ; jour seul « Choisis une recette pour Mercredi » ; `day`/`meal` invalides (hors 1..7 / 1..3) ou -1 ignorés (libellé sans case)
- [ ] T021 [P] [US5] Créer `IT/SearchPickModeTest.kt` (composant `SearchScreen` seul) : `pick` fourni -> `pick_banner` avec le libellé et `pick_cancel` ; clic sur `search_result_<id>` -> `onRecipeClick(recipe)` et aucune fiche ouverte (US5 sc. 1) ; `pick_cancel` -> `onCancel` sans ajout (sc. 2) ; recette de `alreadyInMenu` : pastille `pick_in_menu_<id>` et clic encore possible (sc. 4) ; `pick == null` : aucun bandeau, comportement identique à aujourd'hui (sc. 3) ; recherche par titre, ingrédient et tag fonctionne en mode choix ; photos affichées. `IT/SearchScreenTest.kt` existant non modifié et vert
- [ ] T022 [US5] Réécrire dans `IT/MenuScreenTest.kt` les 6 cas listés en tête de ce fichier (**à signaler, comportement remplacé voulu, jamais affaibli**) : dans `show()`, `onAdd` devient `onPickRecipe` qui enregistre `(day, meal)` reçus ; `addBySearch` devient `addViaPick(recipeId)` : clic sur `add_recipe`, vérifie `onPickRecipe(null, null)`, puis applique l'ajout comme le fait la route (`entries += MenuEntry(...)` avec les parts par défaut) ; **assertions des 6 cas inchangées** ; ajouter un test que `add_recipe` n'ouvre plus de fenêtre (`add_search` absent). Dire dans le compte rendu que le chemin fenêtre -> résultat n'existe plus dans ce fichier

### Implementation for User Story 5

- [ ] T023 [US5] Dans `APP/data/menu/MenuLogic.kt` ajouter `pickBannerLabel(day, meal)` (tests de T020)
- [ ] T024 [US5] Dans `APP/ui/SearchScreen.kt` : `data class SearchPick(val label: String, val alreadyInMenu: Set<String>)` ; paramètres facultatifs `pick: SearchPick? = null` et `onCancel: () -> Unit = {}` ; si `pick != null` : bandeau `pick_banner` + bouton « Annuler » `pick_cancel` au-dessus de la barre de recherche, pastille « déjà au menu » `pick_in_menu_<id>` ; recherche, suggestions, filtres rapides, catégories, recherches récentes et photos inchangés ; ajouter `Modifier.testTag("search_result_<id>")` sur les lignes de résultat (utile aussi hors mode choix) ; avec `pick == null`, aucun changement de comportement
- [ ] T025 [US5] Dans `APP/ui/MenuScreen.kt` : remplacer `MenuActions.onAdd: (Recipe) -> Unit` par `onPickRecipe: (day: Int?, meal: Int?) -> Unit` ; `add_recipe` appelle `onPickRecipe(null, null)` ; `week_add_<jour>_<repas>` appelle `onPickRecipe(jour, repas)` ; supprimer `AddRecipeDialog` (et ses tags `add_search`, `add_list`, `add_cat_<cat>`, `add_result_<id>`) ; **garder `groupByCategory`** (décision : `GroupByCategoryTest` JVM reste vert, non modifié)
- [ ] T026 [US5] Dans `APP/MainActivity.kt` : nouvelle route `search_pick?day={day}&meal={meal}` (arguments entiers, défaut -1), **hors `MAIN_ROUTES`** (pas de barre du bas pendant le choix) ; elle affiche `SearchScreen(pick = SearchPick(label = MenuLogic.pickBannerLabel(...), alreadyInMenu = <ids des recettes au menu>))` ; clic sur une recette : `menuVm.add(recipe, day, meal)` (valeurs -1 ou invalides -> null) puis `navController.popBackStack()` ; « Annuler » et Retour système : `popBackStack()` seul ; brancher `onPickRecipe = { d, m -> navController.navigate("search_pick?day=${d ?: -1}&meal=${m ?: -1}") }` dans la destination `menu` ; la route `search` normale (barre du bas) reste inchangée et ouvre toujours la fiche

**Checkpoint**: ajout par la Recherche en mode choix de bout en bout ; plus de fenêtre d'ajout.

---

## Phase 5: User Story 2 - Placer, déplacer, retirer une recette (Priority: P1)

**Goal**: depuis la Semaine (et la Liste) : placer dans une case, déplacer, « Sans jour », changer les parts, ouvrir la recette, retirer.

**Independent Test**: case mercredi/soir vide -> « Quiche » : présente dans mercredi/soir et dans la liste de courses ; la déplacer en jeudi/midi : plus dans mercredi/soir ; la retirer : disparue du menu et de la liste de courses.

### Tests for User Story 2

- [ ] T027 [P] [US2] Compose UI tests dans `IT/MenuScreenTest.kt` (cas ajoutés) : case vide -> `onPickRecipe(jour, repas)` ; « + » d'une case occupée -> `onPickRecipe` (US2 sc. 2) ; toucher `week_entry_<id>` ouvre `entry_sheet` (titre complet, steppers `servings_<id>_*` / `times_<id>_*`, `sheet_day_*`, `sheet_meal_*`, `sheet_apply`, `sheet_open`, `sheet_remove`) ; déplacer via `sheet_day_4` + `sheet_meal_2` + `sheet_apply` -> `onPlace(id, 4, 2)`, entrée dans la nouvelle case et plus dans l'ancienne, parts inchangées (sc. 3) ; entrée de « À placer » placée dans la grille (sc. 4) ; `sheet_day_0` (« Sans jour ») efface le repas et envoie l'entrée dans « À placer » (sc. 5) ; `sheet_open` -> `onOpenRecipe` ; `sheet_remove` retire l'entrée (sc. 6) ; recette supprimée : feuille avec « Retirer du menu » seulement ; vue Liste : `day_option_<d>` applique le jour tout de suite (comme en 002, repas conservé), `meal_option_<1..3|0>` applique le repas, repas désactivés si « Sans jour » (sc. 7, FR-005) ; SC-002 : ajout dans une case en 3 gestes (case, recherche, recette) vérifié par le nombre de clics du scénario ; SC-003 : déplacement compté dans le test (titre, jour, repas, valider) ; la recette n'est jamais modifiée (callback d'enregistrement de recette jamais appelé)
- [ ] T028 [P] [US2] Cas supplémentaires dans `IT/MenuDaoTest.kt` : après ajout avec `day` et `meal` (`add(..., day = 3, meal = 3)`), l'entrée est lue avec ces valeurs, parts par défaut et position en fin de menu ; ajout sans case : `day` et `meal` null ; même recette ajoutée dans deux cases : 2 entrées

### Implementation for User Story 2

- [ ] T029 [US2] Dans `APP/ui/MenuScreen.kt` : `MenuActions.onPlace: (entryId: String, day: Int?, meal: Int?) -> Unit` ; feuille d'entrée `entry_sheet` (`AlertDialog`, selon `contracts/week-view-ui.md`) avec titre complet, `Stepper` de parts ou de fois réutilisé, rangées `sheet_day_<1..7|0>` et `sheet_meal_<1..3|0>` (« Sans jour » désactive et efface le repas), `sheet_apply`, `sheet_open`, `sheet_remove`, « Fermer » ; identifiant de l'entrée ouverte en `rememberSaveable` ; changer de case n'altère pas les parts ; cartes de « À placer » avec bouton « Placer » ouvrant la feuille ; `onEntryClick` de `WeekView` ouvre la feuille
- [ ] T030 [US2] Dans `APP/ui/MenuScreen.kt`, vue Liste : `day_<id>` ouvre le sélecteur avec `day_option_<1..7|0>` (applique `onDay`, repas conservé) et `meal_option_<1..3|0>` (applique `onPlace(id, jour courant, repas)`) ; `meal_option_*` inactifs si l'entrée n'a pas de jour
- [ ] T031 [US2] Dans `APP/MainActivity.kt` : brancher `onPlace = { id, d, m -> menuVm.setPlacement(id, d, m) }` et `onOpenRecipe` de la feuille (comme la vue Liste) ; vérifier que `onServings`, `onTimes`, `onRemove` sont partagés avec la feuille

**Checkpoint**: la Semaine est pleinement utilisable (US1 + US5 + US2 = MVP du planning).

---

## Phase 6: User Story 3 - Liste de courses inchangée (Priority: P1)

**Goal**: la liste de courses ne dépend ni du placement ni de la vue ; coches et articles manuels conservés.

**Independent Test**: même menu, même liste de courses avant et après avoir placé ses recettes, en Liste et en Semaine.

### Tests for User Story 3

- [ ] T032 [P] [US3] Créer `UT/menu/MenuPlacementShoppingTest.kt` (JVM, vraie table `foods.json` via `TestFoods`) : mêmes `MenuEntry` avec et sans `day`/`meal` -> `ShoppingListBuilder.build` donne des `ShoppingItem` identiques (égalité complète, SC-005, FR-006) ; avec plusieurs répartitions (tout en « À placer », tout placé, mélange) ; même recette dans deux cases comptée deux fois comme avec deux entrées sans jour
- [ ] T033 [P] [US3] Compose UI tests dans `IT/MenuScreenTest.kt` (cas ajoutés) : placer, déplacer, « Sans jour » et changer de vue n'appellent aucun des callbacks de la liste de courses (`shoppingActions`) et ne changent pas les articles ajoutés à la main (US3 sc. 2) ; `IT/ShoppingListScreenTest.kt` existant non modifié et vert
- [ ] T034 [US3] Garde-fou : vérifier par `git diff --stat` que `APP/data/shopping/ShoppingListBuilder.kt`, `APP/data/shopping/ShoppingItem.kt`, `APP/ui/ShoppingListView.kt` et le flux `shopping` de `APP/ui/MenuViewModel.kt` n'ont aucune modification liée à cette feature ; noter le résultat dans le compte rendu

**Checkpoint**: liste de courses prouvée indépendante du placement.

---

## Phase 7: User Story 4 - Vider la semaine (Priority: P2)

**Goal**: « Retirer les jours » remet toutes les recettes dans « À placer » sans les retirer du menu ; « Vider le menu » inchangé.

**Independent Test**: 5 recettes placées, « Retirer les jours » puis confirmer : 5 recettes dans « À placer », liste de courses inchangée.

### Tests for User Story 4

- [ ] T035 [P] [US4] Compose UI tests dans `IT/MenuScreenTest.kt` (cas ajoutés) : `clear_days` visible seulement si au moins une entrée a un jour ; sa confirmation `AlertDialog` « Retirer les jours ? » / « Les recettes restent au menu, sans jour ni repas. La liste de courses ne change pas. » ; `clear_days_confirm` -> `onClearDays()` : toutes les entrées passent dans « À placer », aucune retirée (US4 sc. 1) ; annuler la confirmation ne change rien ; la liste de courses (articles affichés) est identique avant et après ; `clear_menu` / `clear_confirm` se comportent comme en 002 (US4 sc. 2, couvert par `clearMenu_asksConfirmation` réécrit en T022 et par `IT/MenuDaoTest.kt`)

### Implementation for User Story 4

- [ ] T036 [US4] Dans `APP/ui/MenuScreen.kt` : `MenuActions.onClearDays: () -> Unit` ; bouton « Retirer les jours » `clear_days` (menu « ... » ou bouton texte avant « Vider le menu », visible seulement s'il y a un jour) et confirmation ; « Vider le menu » inchangé
- [ ] T037 [US4] Dans `APP/MainActivity.kt` : brancher `onClearDays = { menuVm.clearPlacements() }`

**Checkpoint**: toutes les stories livrées.

---

## Phase 8: Polish & Vérification finale

**Purpose**: tout vérifier, sur JVM puis sur le téléphone, avec les données réelles protégées, puis `/verifie-tests`.

- [ ] T038 Lancer tous les tests JVM : `./gradlew testDebugUnitTest` ; tout vert, nombre de tests >= celui de T002 (aucun test supprimé ; `GroupByCategoryTest` et `groupByDay_mondayToSundayThenNoDay` présents et verts)
- [ ] T039 `./gradlew assembleDebug assembleDebugAndroidTest` ; corriger les erreurs et avertissements de compilation introduits par la feature
- [ ] T040 Avant d'instrumenter : re-sauvegarder `url_import.xml` (taille vérifiée), couper le volume média du téléphone (noter la valeur d'origine)
- [ ] T041 `adb install -r app/build/outputs/apk/debug/app-debug.apk` puis `adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (`-r` : garde les données de l'app)
- [ ] T042 `adb shell am instrument -w -e class <classe> com.example.recipelibrary.test/androidx.test.runner.AndroidJUnitRunner` pour : `MenuScreenTest`, `MenuDaoTest`, `DatabaseMigrationTest`, `SearchPickModeTest`, `SearchScreenTest`, `MenuViewPrefTest`, `ShoppingListScreenTest`, `MenuDeleteWarningTest` ; tout vert (jamais `connectedAndroidTest`)
- [ ] T043 Lancer ensuite toute la suite instrumentée sans filtre de classe (`am instrument -w` sans `-e class`) pour détecter une régression ailleurs ; tout vert
- [ ] T044 Restaurer `url_import.xml` (taille comparée à la sauvegarde de T040) et remettre le volume média à sa valeur d'origine ; vérifier que les réglages d'import sont intacts
- [ ] T045 [P] Vérifier qu'aucune écriture dans `recipes` n'a été ajoutée par la feature (relecture de `git diff` sur `MenuRepository`, `MenuViewModel`, `MainActivity`)
- [ ] T046 [P] Vérifier qu'aucune migration existante (`MIGRATION_1_2` à `MIGRATION_8_9`) n'a été modifiée (`git diff` de `APP/data/Migrations.kt` : seulement l'ajout de `MIGRATION_9_10`)
- [ ] T047 Vérifier SC-004 sur le téléphone : base `recipes.db` en version 10 après `adb install -r` ; même nombre de recettes et d'entrées de menu qu'avant (comparer à la sauvegarde de T003), parts et jours identiques, `meal` NULL pour toutes
- [ ] T048 Essai téléphone sur les données réelles (quickstart §4 points 1 à 8) : entrées d'avant présentes et liste de courses identique ; repas en vue Liste et tri ; « Semaine » (sans repas / à placer) et vue retrouvée après fermeture de l'app ; case vide mercredi/soir -> bandeau « Choisis une recette pour Mercredi · Soir » et photos -> recette ajoutée et présente dans les courses ; « Annuler » et Retour système n'ajoutent rien ; déplacer, « Sans jour », retirer ; « Retirer les jours » ; onglet Recherche de la barre du bas ouvre toujours la fiche ; barre du bas masquée pendant le choix ; vérifier SC-001 (21 cases + « À placer » sans défilement horizontal) et SC-002 / SC-003 (nombre de gestes)
- [ ] T049 Lancer `/verifie-tests` et rendre compte à l'utilisatrice en **signalant explicitement** : (a) les 6 cas de `MenuScreenTest` réécrits pour le mode choix (comportement remplacé voulu, assertions inchangées, seuls `show()` et l'assistant d'ajout changent), (b) l'ajout de `MIGRATION_9_10` aux listes `addMigrations` de `DatabaseMigrationTest`, (c) les tags `add_search`, `add_list`, `add_cat_<cat>`, `add_result_<id>` retirés, (d) la jonction route `search_pick` -> `menuVm.add` vérifiée seulement par l'essai téléphone ; aucun commit sans demande

---

## Dependencies & Execution Order

- **Phase 1** (T001 bloquant : version 9 requise) → **Phase 2** → stories.
- **US1** (Phase 3) ne dépend que de la Phase 2.
- **US5** (Phase 4) dépend de la Phase 2 ; elle câble aussi les « + » de la grille créés en US1 (T025 après T018).
- **US2** (Phase 5) dépend de US1 (grille) et de US5 (`onPickRecipe`, `MenuActions` modifié).
- **US3** (Phase 6) : T032 dépend seulement de la Phase 2 (peut avancer en parallèle de tout) ; T033 attend US2 ; T034 en dernier.
- **US4** (Phase 7) dépend de la Phase 2 et de US1 (entêtes de `MenuScreen`) ; indépendante de US2 pour la logique.
- **Phase 8** en dernier ; T044 obligatoire même si un test échoue.
- Au sein d'un fichier unique (`MenuScreen.kt`, `MenuScreenTest.kt`, `MainActivity.kt`) les tâches sont séquentielles.

## Parallel Opportunities

- Phase 2 : T004, T005, T006 (tests, fichiers différents) en parallèle ; T010 en parallèle de T007 à T009.
- US1 : T013 et T014 en parallèle ; T015 en parallèle de T016.
- US5 : T020 et T021 en parallèle.
- US2 : T027 et T028 en parallèle.
- US3 : T032 peut être fait dès la Phase 2 ; T033 en parallèle de T032.
- Phase 8 : T045 et T046 en parallèle.

## Implementation Strategy

1. **Données d'abord** (Phases 1 et 2) : colonne `meal`, migration testée, DAO, logique pure ; arrêt si la base n'est pas en version 9.
2. **MVP du planning** : US1 + US5 + US2 (voir, choisir, placer), démonstration sur le téléphone.
3. **Garde-fou** : US3 (liste de courses prouvée inchangée), puis US4 (« Retirer les jours »).
4. **Validation** : Phase 8 sur le téléphone avec sauvegarde et restauration de `url_import.xml`, puis `/verifie-tests`.
