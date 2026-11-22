# Tasks: Menus et liste de courses

**Input**: Design documents from `/specs/002-menus-liste-courses/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Inclus. La règle permanente de l'utilisatrice impose `/verifie-tests` avant de déclarer le travail terminé, et la spec fixe des critères mesurables (SC-001 à SC-006).

**Organization**: Tâches groupées par user story, dans l'ordre des priorités (P1 : US1, US2 ; P2 : US3 ; P3 : US4).

Abréviations de chemins :
- `APP` = `app/src/main/java/com/example/recipelibrary`
- `UT` = `app/src/test/java/com/example/recipelibrary`
- `IT` = `app/src/androidTest/java/com/example/recipelibrary`

Règles pour toutes les tâches : tests sur le téléphone par `am instrument` uniquement (jamais `connectedAndroidTest`), `url_import.xml` sauvegardé (taille vérifiée) puis restauré, `adb install -r` ; aucune écriture dans la table `recipes` sauf la suppression demandée par l'utilisatrice et « Indiquer les parts de la recette » ; rien de commité sans demande.

## Format: `[ID] [P?] [Story] Description`

- **[P]** : peut être fait en parallèle (fichiers différents, pas de dépendance sur une tâche en cours)
- **[Story]** : user story concernée (US1 à US4)

---

## Phase 1: Setup

**Purpose**: Lecture des quantités réutilisable par la liste de courses.

- [X] T001 Ajouter à `ParsedIngredient` (`APP/data/nutrition/IngredientParser.kt`) un champ `amount: Amount? = null`, avec `data class Amount(val kind: AmountKind, val value: Double)` et `enum class AmountKind { MASS, VOLUME, PIECE, SACHET }` (`value` en g, ml, pièces ou sachets, quantité × facteur de l'unité ; cuillères, verres, tasses et bols en VOLUME ; `null` pour une quantité vague ou absente) ; ne changer ni `grams` ni `issue` ; ajouter aussi `looseQuantity: String? = null` : pour une quantité vague, quantité et unité telles qu'écrites (« 1 boîte », « 1 botte », « 2 brins »), null sans quantité ; une boîte ou un sachet avec un poids écrit (« 1 boîte de 400 g ») donne `amount` MASS 400 et pas de `looseQuantity`
- [X] T002 [P] Tests JVM de `amount` dans `UT/nutrition/IngredientAmountTest.kt` : « 250 g de farine » → MASS 250 ; « 1,5 kg » → MASS 1500 ; « 50 cl de lait » → VOLUME 500 ; « 2 c. à soupe d'huile » → VOLUME 30 ; « 3 œufs » → PIECE 3 ; « 2 à 3 pommes » → PIECE 2,5 ; « 1/2 citron » → PIECE 0,5 ; « 1 sachet de levure » → SACHET 1 ; « 1 pincée de sel » et « du sucre » → `amount` null ; « 1 boîte de tomates » → `amount` null et `looseQuantity` « 1 boîte » ; « 1 boîte de 400 g de tomates » → MASS 400, `looseQuantity` null ; « 1 botte de radis » → `looseQuantity` « 1 botte » ; « du sucre » → `looseQuantity` null ; les tests existants de `IngredientParserTest` restent inchangés et verts
- [X] T003 [P] Rendre `isHeader(line)` de `APP/data/nutrition/NutritionCalculator.kt` réutilisable (fonction de niveau fichier ou `object`, même règle : ligne vide, ou finissant par « : » sans chiffre) sans changer son comportement

---

## Phase 2: Foundational (bloquant pour toutes les stories)

**Purpose**: Stockage du menu, des coches et des ajouts à la main, sans perte de données.

**⚠️ CRITICAL**: aucune story ne démarre avant la fin de cette phase.

- [X] T004 Créer les entités Room dans `APP/data/menu/` : `MenuEntry` (table `menu_entries` : `id` TEXT PK UUID, `recipe_id` TEXT NOT NULL indexé `index_menu_entries_recipe_id`, `servings` INTEGER nullable 1 à 100, `times` INTEGER NOT NULL DEFAULT 1 (1 à 10, nombre de fois la recette quand `servings` est null), `day` INTEGER nullable 1 = lundi … 7 = dimanche, `position` INTEGER NOT NULL, `created_at` INTEGER NOT NULL), `ShoppingCheck` (table `shopping_checks` : `item_key` TEXT PK, `checked_at` INTEGER NOT NULL), `ShoppingExtra` (table `shopping_extras` : `id` TEXT PK UUID, `text` TEXT NOT NULL 1 à 200 caractères, `checked` INTEGER NOT NULL DEFAULT 0, `position` INTEGER NOT NULL, `created_at` INTEGER NOT NULL) ; pas de clé étrangère
- [X] T005 Créer `APP/data/menu/MenuDao.kt` : entrées (flux ordonné par `position`, insertion en fin, mise à jour des parts et du jour, suppression d'une entrée, vidage du menu avec suppression des coches, `countForRecipe(recipeId)`, `deleteForRecipe(recipeId)`) ; coches (flux des clés, cocher, décocher) ; ajouts à la main (flux, ajout, modification du texte, coche, suppression)
- [X] T006 Ajouter `MIGRATION_6_7` dans `APP/data/Migrations.kt` (les trois `CREATE TABLE` et l'index, exactement le schéma généré par Room, rien d'autre), passer `version = 7` et ajouter les trois entités et `menuDao()` dans `APP/data/AppDatabase.kt`, enregistrer la migration dans `APP/data/DatabaseProvider.kt`
- [X] T007 Étendre `IT/DatabaseMigrationTest.kt` avec un test 6 → 7 : recettes insérées en v6 (tous les champs remplis, dont `servings` et `ingredient_weights`) relues à l'identique en v7, trois tables nouvelles et vides ; ajouter `MIGRATION_6_7` à la liste des migrations des tests existants qui ouvrent la base avec Room (seule modification permise des tests existants, à signaler à l'utilisatrice)
- [X] T008 Créer `APP/data/menu/MenuRepository.kt` et ajouter à `APP/data/RecipeRepository.kt` une suppression de recette qui supprime aussi ses entrées de menu dans une seule transaction (`withTransaction`) ; brancher `APP/ui/RecipeListViewModel.kt` dessus
- [X] T009 [P] Tests sur le téléphone de `MenuDao` et de la suppression transactionnelle dans `IT/MenuDaoTest.kt` (base en mémoire) : ordre par position, deux entrées pour la même recette, vidage du menu sans toucher aux ajouts à la main, suppression d'une recette → ses entrées disparaissent, les autres restent, la table `recipes` des autres recettes est inchangée

**Checkpoint**: base en version 7, recettes intactes, menu stockable.

---

## Phase 3: User Story 1 - Choisir les recettes à cuisiner (Priority: P1) 🎯 MVP

**Goal**: ajouter des recettes au menu (fiche et recherche), avec parts et jour facultatif ; le retrouver après redémarrage.

**Independent Test**: ajouter « Pâte à crêpes » (4 parts) et « Quiche lorraine » (6 parts), fermer et rouvrir l'app : les deux sont au menu avec leurs parts.

### Tests for User Story 1

- [X] T010 [P] [US1] Tests JVM dans `UT/menu/MenuLogicTest.kt` : parts par défaut à l'ajout (parts de la recette si connues, sinon estimation de `ServingsEstimator`, sinon `null` avec `times` = 1) ; facteur d'une entrée (`servings ÷ parts de la recette`, sinon `times`) ; `times` hors 1 à 10 refusé ; regroupement d'affichage par jour (lundi → dimanche puis « Sans jour », ordre par position dans chaque groupe) ; parts saisies hors 1 à 100 refusées
- [X] T011 [P] [US1] Compose UI tests dans `IT/MenuScreenTest.kt` : ajout par recherche (« quiche ») ; changement des parts ; recette sans parts affichée « × 1 recette », passage à × 2 ; lien « Indiquer les parts de la recette » → l'entrée passe en parts et le callback d'enregistrement de la recette reçoit `servings` et `servingsSource = USER` (seule écriture permise) ; choix puis retrait d'un jour ; regroupement par jour ; même recette ajoutée deux fois → deux entrées ; retrait d'une entrée ; vider le menu demande confirmation ; menu vide → message d'invitation ; la recette n'est jamais modifiée par les autres actions (callback d'enregistrement jamais appelé)

### Implementation for User Story 1

- [X] T012 [US1] Créer `APP/ui/MenuViewModel.kt` (menu à partir de `MenuDao` et des recettes, actions ajouter, changer parts, changer nombre de fois, indiquer les parts de la recette (par `RecipeEdits.withServings`), changer jour, retirer, vider) et la logique pure de T010 dans `APP/data/menu/MenuLogic.kt`
- [X] T013 [US1] Créer `APP/ui/MenuScreen.kt` : onglets internes « Menu » / « Courses » (Courses vide pour l'instant) ; vue Menu groupée par jour, chaque entrée avec titre, parts modifiables (1 à 100) ou, pour une recette sans parts, « × N recette » (1 à 10) avec le lien « Indiquer les parts de la recette », choix du jour (lundi à dimanche, « Sans jour »), retrait ; bouton « Ajouter une recette » ouvrant une recherche par titre parmi les recettes ; « Vider le menu » avec confirmation ; tags de test utilisés par T011
- [X] T014 [US1] Dans `APP/MainActivity.kt`, ajouter l'onglet « Menu » à la barre du bas (route `menu`, ajoutée à `MAIN_ROUTES`, icône dans `res/drawable`) et la destination `composable("menu")`
- [X] T015 [US1] Dans `APP/ui/RecipeDetailScreen.kt`, ajouter « Ajouter au menu » (message de confirmation « Ajoutée au menu ») ; brancher dans `APP/MainActivity.kt`
- [X] T016 [US1] Avertissement à la suppression (FR-005) dans `APP/ui/RecipeDetailScreen.kt` et `APP/ui/HomeScreen.kt` : si `countForRecipe > 0`, le texte de confirmation ajoute « Elle est au menu (n fois) et en sera retirée. » ; suppression par la méthode transactionnelle de T008 ; tests ajoutés dans `IT/RecipeDetailScreenTest.kt` (nouveau test, sans modifier les existants)

**Checkpoint**: menu utilisable seul.

---

## Phase 4: User Story 2 - Obtenir la liste de courses du menu (Priority: P1)

**Goal**: liste calculée du menu, parts appliquées, aliments regroupés, quantités additionnées, rien de perdu.

**Independent Test**: menu crêpes (recette 4 parts prévue pour 8 : « 250 g de farine, 4 œufs ») + gâteau (« 200 g de farine, 3 œufs ») → « farine : 700 g », « œufs : 11 ».

### Tests for User Story 2

- [X] T017 [P] [US2] Tests JVM dans `UT/shopping/QuantityFormatTest.kt` : g jusqu'à 999 puis kg à 0,1 près (« 1,5 kg ») ; ml < 100 en ml, sinon cl jusqu'à 99 cl, puis l à 0,1 près ; pièces et sachets arrondis au supérieur (1,5 → 2) ; virgule décimale, pas de zéros inutiles ; plusieurs natures jointes par « + »
- [X] T018 [P] [US2] Tests JVM dans `UT/shopping/ShoppingListBuilderTest.kt` (vraie table `foods.json` via `TestFoods`) : facteur de parts (4 → 8 = ×2 ; recette sans parts : `times` ×1 et ×3) ; regroupement par code d'aliment (« œufs » / « oeuf », « tomates » / « tomate ») ; masses additionnées ; volumes additionnés (« 50 cl de lait » + « 200 ml de lait » = 70 cl) ; natures différentes côte à côte (« 400 g + 1 boîte ») ; « 1 boîte » + « 1 boîte » → « 2 boîtes » ; lignes vagues et non reconnues gardées telles qu'écrites ; titres ignorés ; assaisonnements sans quantité, une seule fois ; origine (recettes d'où vient chaque article, deux entrées de la même recette comptées deux fois) ; entrée dont la recette est introuvable ignorée
- [X] T019 [US2] 10 menus de référence (SC-002) dans `UT/shopping/ReferenceMenusTest.kt`, construits à la main à partir de recettes réalistes avec la liste attendue écrite à la main : 100 % des lignes d'ingrédients (hors titres) présentes dans la liste ; au moins 90 % des aliments communs à plusieurs recettes regroupés avec la bonne somme
- [X] T020 [P] [US2] Test de durée JVM dans `UT/shopping/ShoppingListPerfTest.kt` : menu de 30 recettes de 12 ingrédients calculé en moins de 300 ms (marge pour SC-003)

### Implementation for User Story 2

- [X] T021 [P] [US2] Créer `APP/data/shopping/QuantityFormat.kt` (règles de T017)
- [X] T022 [US2] Créer `APP/data/shopping/ShoppingListBuilder.kt` : `build(entries, recipesById, parser, servingsOf: (Recipe) -> Int?): List<ShoppingItem>` (`servingsOf` = parts indiquées, sinon estimées par le calcul des calories, sinon null ; calculées une fois par recette dans l'app, fournies directement en test) selon research R3 et data-model (`key` `food:<code>` ou `txt:<texte normalisé>`, `label` = premier alias de l'aliment sinon texte sans quantité, `totals` par nature, `loose`, `seasoning`, `sources`) ; Kotlin pur
- [X] T023 [US2] Créer `APP/ui/ShoppingListView.kt` dans la vue « Courses » de `MenuScreen` : un article par ligne avec ses quantités ; toucher un article affiche les recettes d'origine ; menu vide → « Ajoute d'abord des recettes au menu » ; calcul hors du fil principal, recalculé quand le menu ou une recette change
- [X] T024 [P] [US2] Compose UI tests dans `IT/ShoppingListScreenTest.kt` : articles regroupés et sommes affichées pour le menu du test indépendant ; origine visible ; ligne non reconnue affichée telle qu'écrite ; menu vide → message ; ajout d'une recette au menu → liste mise à jour

**Checkpoint**: liste de courses utilisable (US1 + US2 = MVP).

---

## Phase 5: User Story 3 - Préparer la commande (Priority: P2)

**Goal**: cocher, ajouter à la main, partager en texte, passer par Chronodrive article par article.

**Independent Test**: cocher « œufs », ajouter « papier toilette », partager : le texte contient farine et papier toilette, pas les œufs ; « Chronodrive » sur « farine » copie « farine » et ouvre l'application Chronodrive.

### Tests for User Story 3

- [X] T025 [P] [US3] Tests JVM dans `UT/shopping/ShoppingTextTest.kt` selon `contracts/shopping-text.md` : en-tête « Liste de courses (n recettes) » ; seuls les non cochés ; ajouts à la main inclus ; quantités formatées ; assaisonnements sur une ligne « (à vérifier) » ; liste vide → `null` (bouton désactivé)
- [X] T026 [P] [US3] Tests JVM dans `UT/shopping/ChronodriveSearchTest.kt` selon `contracts/chronodrive-search.md` : terme = premier alias pour un aliment reconnu ; texte sans quantité, unité ni article de tête pour un non reconnu (« 1 c. à café de piment d'Espelette » → « piment d'Espelette ») ; texte sans quantité de tête pour un ajout à la main ; 1 à 60 caractères ; adresse de secours `https://www.chronodrive.com/search/` + terme encodé (`creme%20fraiche`)

### Implementation for User Story 3

- [X] T027 [US3] Coches dans `APP/ui/ShoppingListView.kt` et `APP/ui/MenuViewModel.kt` : cocher / décocher par clé (`shopping_checks`) ; cochés en bas, barrés ; état conservé au recalcul (FR-014)
- [X] T028 [US3] Ajouts à la main : champ « Ajouter un article », modification et suppression ; conservés quand le menu change ou est vidé (FR-013)
- [X] T029 [P] [US3] Créer `APP/data/shopping/ShoppingText.kt` (T025) et le bouton « Partager » (`ACTION_SEND` `text/plain`, sujet « Liste de courses », désactivé si rien à partager)
- [X] T030 [P] [US3] Créer `APP/data/shopping/ChronodriveSearch.kt` (terme et adresse de secours, T026)
- [X] T031 [US3] Bouton « Chronodrive » sur chaque article non coché, selon `contracts/chronodrive-search.md` : copie du terme, message « « <terme> » copié, colle-le dans la recherche Chronodrive », ouverture par `getLaunchIntentForPackage("com.ikomobi.chronodrive")`, secours navigateur, message si rien ne s'ouvre ; ajouter `<queries><package android:name="com.ikomobi.chronodrive"/></queries>` dans `app/src/main/AndroidManifest.xml` (visibilité des paquets, Android 11+) ; mémoriser la clé de l'article (`rememberSaveable`) et, au retour, le mettre en évidence avec un bouton « Coché » ; conserver aussi la position de défilement de la liste (`rememberLazyListState` sauvegardé) au retour
- [X] T032 [US3] Compléter `IT/ShoppingListScreenTest.kt` : cocher puis décocher ; coche conservée après ajout d'une recette ; ajout à la main conservé après « Vider le menu » ; bouton Partager désactivé quand tout est coché ; bouton « Chronodrive » : le presse-papiers contient le terme l'article est mis en évidence et la position de défilement est conservée après recréation de l'écran (lancement de l'application vérifié par un callback injecté, sans ouvrir Chronodrive pendant les tests)

**Checkpoint**: commande possible depuis la liste.

---

## Phase 6: User Story 4 - Liste rangée par rayon (Priority: P3)

**Goal**: articles groupés par rayon, à l'écran et dans le texte partagé.

**Independent Test**: liste avec farine, lait, pommes, poulet → quatre rayons différents.

- [X] T033 [US4] Dans `tools/ciqual/build_foods.py`, lire le groupe et le sous-groupe Ciqual et calculer `aisle` ∈ « Fruits et légumes », « Crèmerie », « Boucherie et poisson », « Épicerie salée », « Épicerie sucrée », « Surgelés », « Boissons », « Autres » ; colonne facultative de forçage du rayon dans `tools/ciqual/selection.csv` ; mettre à jour `tools/ciqual/README.md` et `specs/001-calories-par-part/contracts/foods-json.md` (champ facultatif) ; regénérer `app/src/main/assets/nutrition/foods.json` (mêmes kcal qu'avant, vérifié par un diff limité au champ `aisle`)
- [X] T034 [P] [US4] Ajouter `aisle: String? = null` à `APP/data/nutrition/Food.kt` ; étendre `UT/nutrition/FoodsJsonTest.kt` par un nouveau test (valeur de `aisle` dans la liste fermée si présente ; farine, lait, pomme, poulet dans les rayons attendus) sans modifier les tests existants
- [X] T035 [US4] `aisle` dans `ShoppingItem` (« Autres » si absent ou ajout à la main) ; groupement par rayon dans l'ordre fixe dans `APP/ui/ShoppingListView.kt` et titres de rayon dans `APP/data/shopping/ShoppingText.kt` ; tests ajoutés à `UT/shopping/ShoppingTextTest.kt` et `IT/ShoppingListScreenTest.kt`

**Checkpoint**: toutes les stories livrées.

---

## Phase 7: Polish & Cross-Cutting Concerns

- [X] T035b (ajout du 2026-11-22) Coches expirées au bout de 3 jours (FR-016b) : `MenuDao.deleteChecksBefore`, `MenuRepository.expireChecks`, `MenuLogic.CHECK_TTL_MS`, appel à l'ouverture de l'onglet Menu ; tests `UT/menu/CheckExpiryTest.kt` et `IT/MenuDaoTest.checksOlderThanThreeDays_expire_recentKept`
- [X] T036 Lancer tous les tests JVM et sur le téléphone (quickstart §1 à §3) ; tout vert
- [X] T037 (2026-11-22 : copie de la base du téléphone en version 8, 103 recettes identiques à la sauvegarde + 1 nouvelle, intégrité ok) Vérifier la migration sur une copie de la base du téléphone (quickstart §4, SC-004) : même nombre de recettes, contenu identique à la sauvegarde, version 7
- [X] T038 (validé par l'utilisatrice le 2026-11-22 ; Boursin ajouté à la table) Parcours à la main avec l'utilisatrice (quickstart §5 : SC-001, SC-006) et noter les résultats ; SC-006 : bon type de produit dans les 5 premiers résultats
- [X] T039 Lancer `/verifie-tests` et rendre compte à l'utilisatrice (tests existants modifiés : seulement l'ajout de `MIGRATION_6_7` à T007)

---

## Dependencies & Execution Order

- **Phase 1** → **Phase 2** → stories.
- **US1** (Phase 3) ne dépend que de la Phase 2.
- **US2** (Phase 4) dépend de T001 à T003 et de la Phase 2 ; elle peut avancer en parallèle de US1 pour la logique pure (T017 à T022), l'écran (T023, T024) attend T013.
- **US3** (Phase 5) dépend de US2.
- **US4** (Phase 6) dépend de US2 ; T033 et T034 peuvent être faits à tout moment après la Phase 1.
- **Polish** en dernier.

## Parallel Opportunities

- Phase 1 : T002 et T003 en parallèle après T001.
- Phase 2 : T009 en parallèle de T008 une fois T004 à T006 faits.
- US1 : T010 et T011 en parallèle.
- US2 : T017, T018, T020, T021 en parallèle ; T024 en parallèle de T019.
- US3 : T025, T026, T029, T030 en parallèle.

## Implementation Strategy

1. **MVP** : Phases 1 à 4 (menu + liste de courses), puis démonstration sur le téléphone.
2. **Commande** : Phase 5 (coches, ajouts, partage, Chronodrive).
3. **Confort** : Phase 6 (rayons).
4. **Validation** : Phase 7, `/verifie-tests`.
