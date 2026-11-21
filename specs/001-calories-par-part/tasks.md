# Tasks: Calories par part

**Input**: Design documents from `/specs/001-calories-par-part/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Inclus. La règle permanente de l'utilisatrice impose `/verifie-tests` avant de déclarer le travail terminé, et la spec fixe des critères mesurables (SC-001 à SC-006).

**Organization**: Tâches groupées par user story, dans l'ordre des priorités (P1 : US1, US5 ; P2 : US2 ; P3 : US3, US4).

Abréviations de chemins :
- `APP` = `app/src/main/java/com/example/recipelibrary`
- `UT` = `app/src/test/java/com/example/recipelibrary`
- `IT` = `app/src/androidTest/java/com/example/recipelibrary`
- `HERMES` = `C:\Users\Kikine\AppData\Local\hermes`

## Format: `[ID] [P?] [Story] Description`

- **[P]** : peut être fait en parallèle (fichiers différents, pas de dépendance sur une tâche en cours)
- **[Story]** : user story concernée (US1 à US5)

---

## Phase 1: Setup

**Purpose**: Table de référence des aliments et outillage.

- [X] T001 Trouver et noter dans `tools/ciqual/README.md` l'URL officielle du fichier Ciqual 2020 (XLSX) sur ciqual.anses.fr, sa licence (Licence Ouverte) et la mention de source à afficher ; demander à l'utilisatrice avant tout téléchargement ou installation de paquet (`openpyxl` dans le venv de Hermes)
- [X] T002 Écrire `tools/ciqual/selection.csv` (format `code;aliases (séparés par |);pieceG;density;kind`) avec au moins 300 aliments courants de cuisine familiale française : alias en minuscules, sans accents, sans `œ`, chaque alias unique ; `kind` ∈ `NORMAL`, `SEASONING` (sel, poivre, épices, herbes sèches), `FRYING` (huile de friture) ; `pieceG` pour les aliments comptés à la pièce (œuf 55, oignon 100, gousse d'ail 5, pomme 150, citron 120…) ; `density` en g/ml pour les aliments mesurés en volume (huile 0,92, farine 0,55, sucre 0,85, lait 1,03, miel 1,4…)
- [X] T003 Écrire `tools/ciqual/build_foods.py` (lancé avec `python -I`, fichier Ciqual passé en argument, jamais exécuté depuis le dossier du fichier téléchargé) qui lit le XLSX Ciqual et `selection.csv` et produit `app/src/main/assets/nutrition/foods.json` au format de `contracts/foods-json.md` (kcal = « Energie, Règlement UE N° 1169/2011 (kcal/100 g) », protéines, glucides, lipides ; valeurs « traces » ou « < x » → 0 ; échec explicite si un code de la sélection est introuvable)
- [X] T004 Générer `app/src/main/assets/nutrition/foods.json` avec T003 et le versionner

---

## Phase 2: Foundational (bloquant pour toutes les stories)

**Purpose**: Stockage des nouvelles données sans perte, table chargée en mémoire, analyse des lignes.

**⚠️ CRITICAL**: aucune story ne démarre avant la fin de cette phase.

- [X] T005 [P] Test JVM du fichier `foods.json` dans `UT/nutrition/FoodsJsonTest.kt` : au moins 300 aliments ; `code` unique ; alias uniques dans tout le fichier ; alias en minuscules, sans accents ni `œ` ; `kcal` entre 0 et 910, nutriments entre 0 et 100 ; `pieceG` > 0 si présent ; `density` entre 0,1 et 2 si présente ; `kind` ∈ `NORMAL`, `SEASONING`, `FRYING` (lecture du fichier depuis `app/src/main/assets`)
- [X] T006 Ajouter à `APP/data/Recipe.kt` : `servings: Int? = null` (colonne `servings`, 1 à 100, `NULL` = inconnu), `servingsSource: String? = null` (colonne `servings_source`, `USER` ou `SOURCE`, `NULL` si `servings` est `NULL`), `ingredientWeights: Map<String, IngredientWeight> = emptyMap()` (colonne `ingredient_weights`) ; créer `data class IngredientWeight(val g: Double?, val src: String)` (`g` > 0 et ≤ 10 000, `null` autorisé seulement pour `src = PC` ; `src` ∈ `PC`, `USER`) dans `APP/data/IngredientWeight.kt`
- [X] T007 Ajouter à `APP/data/Converters.kt` les convertisseurs `Map<String, IngredientWeight>` ⇄ JSON (`{ "<texte exact de la ligne>": { "g": <nombre ou null>, "src": "PC" | "USER" } }`, `NULL`/JSON invalide → carte vide, jamais d'exception)
- [X] T008 Ajouter `MIGRATION_5_6` dans `APP/data/Migrations.kt` (`ALTER TABLE recipes ADD COLUMN servings INTEGER`, `servings_source TEXT`, `ingredient_weights TEXT NOT NULL DEFAULT '{}'`, rien d'autre), passer `version = 6` dans `APP/data/AppDatabase.kt`, enregistrer la migration dans `APP/data/DatabaseProvider.kt`
- [X] T009 Étendre `IT/DatabaseMigrationTest.kt` avec un test 5 → 6 : recettes insérées en v5 (tous les champs remplis) relues à l'identique en v6, nouvelles colonnes à `NULL` (`ingredient_weights` à `{}`) ; ne modifier aucun test existant
- [X] T010 [P] Créer `APP/data/nutrition/Normalize.kt` : minuscules, accents retirés, `œ`→`oe`, `æ`→`ae`, apostrophes unifiées, pluriels simples (`s`, `x`, `aux`→`al`) mot par mot ; tests dans `UT/nutrition/NormalizeTest.kt`
- [X] T011 [P] Créer `APP/data/nutrition/Food.kt` (champs de `data-model.md` : `code`, `name`, `aliases`, `kcal`, `protein`, `carbs`, `fat` pour 100 g, `pieceG?`, `density?` défaut 1,0, `kind`) et `APP/data/nutrition/FoodTable.kt` : construction depuis un texte JSON (testable en JVM), chargement unique et paresseux depuis les assets, appariement par la plus longue variante contenue dans le texte, en mots entiers (R3)
- [X] T012 [P] Tests d'appariement dans `UT/nutrition/FoodTableTest.kt` : « beurre demi-sel » → beurre demi-sel avant beurre ; « Oeufs » / « œuf » → œuf ; « riz » ne trouve pas « ris de veau » ; texte inconnu → `null`
- [X] T013 Créer `APP/data/nutrition/IngredientParser.kt` (R2) : quantité (entiers, décimales à virgule, `1/2`, `½ ¼ ¾`, fourchettes `2 à 3` et `100-150` → moyenne, « un/une/deux… douze »), unité (g, kg, mg, ml, cl, dl, l, c. à soupe/càs/cs = 15 ml, c. à café/càc/cc = 5 ml, verre = 200 ml, tasse = 250 ml, sachet de levure 11 g, sachet de sucre vanillé 7,5 g, pincée, noix, gousse, tranche, pièce implicite), aliment (reste nettoyé : « de », « d' », parenthèses, adjectifs courants) ; conversion en grammes (masse directe, volume × `density`, pièce × `pieceG` sinon `NO_PIECE_WEIGHT`) ; problèmes `VAGUE` (pincée, « selon le goût », « un peu », sans quantité), `UNKNOWN_FOOD`, `TO_COMPLETE` (ligne « À compléter »)
- [X] T014 Tests de l'analyseur dans `UT/nutrition/IngredientParserTest.kt` : au moins 40 lignes réelles couvrant chaque forme de T013 (« 200 g de farine », « 25 cl de lait », « 2 c. à soupe d'huile », « 3 œufs », « 1/2 litre de crème », « 1,5 kg de pommes de terre », « 2 à 3 pommes », « une pincée de sel », « sel, poivre », « 1 noix de beurre », « À compléter »)

**Checkpoint**: données stockées sans perte, lignes analysées ; les stories peuvent commencer.

---

## Phase 3: User Story 1 - Voir les calories par part (Priority: P1) 🎯 MVP

**Goal**: La fiche affiche « ≈ X kcal / part » (ou le total), y compris pour les recettes existantes, hors ligne.

**Independent Test**: Recette « Pâte à crêpes », 4 parts, « 250 g de farine, 4 œufs, 50 cl de lait, 50 g de beurre » → valeur par part à ±15 % du calcul à la main.

### Tests for User Story 1

- [X] T015 [P] [US1] Tests du calcul dans `UT/nutrition/NutritionCalculatorTest.kt` : priorités de poids `USER` > écrit > `PC` ; total et par part (kcal arrondis à la dizaine, nutriments au gramme) ; lignes `SEASONING` ignorées sans avertissement ; `FRYING` compté avec avertissement ; `PARTIAL` si plus de la moitié des lignes d'ingrédients (hors `SEASONING` : sel, poivre, épices) ne sont pas comptées (FR-013) ; `CHECK_VALUE` au-delà de 3 000 kcal/part ou 30 parts estimées ; aucune ligne comptée → pas de valeur ; entrées de `ingredientWeights` orphelines ignorées
- [X] T016 [P] [US1] Tests de l'estimation des parts dans `UT/nutrition/ServingsEstimatorTest.kt` : chaque catégorie de FR-016 ; arrondi à l'entier, minimum 1 ; pas d'estimation sans catégorie ou quand plus de la moitié des lignes (hors sel, poivre, épices) ne sont pas comptées (FR-017) ; priorité `USER` > `SOURCE` > estimé ; « Gâteaux » 600 g → 6 parts
- [X] T017 [P] [US1] Jeu de 20 recettes de référence dans `UT/nutrition/ReferenceRecipesTest.kt` (quantités précises, valeur attendue calculée à la main avec la table Ciqual et notée en commentaire) : au moins 18 sur 20 à ±15 % (SC-001)
- [X] T018 [P] [US1] Test Compose dans `IT/RecipeDetailNutritionTest.kt` : temps entre l'affichage de la fiche et l'apparition du bandeau < 500 ms pour une recette de 20 ingrédients, table déjà chargée (SC-004) ; bandeau « ≈ X kcal / part » avec 4 parts ; « ≈ 6 parts (estimé) » pour un gâteau de 600 g sans parts ; « ≈ X kcal pour la recette » et invitation à indiquer les parts quand l'estimation est impossible ; message « estimation impossible » si aucune ligne n'est comptée

### Implementation for User Story 1

- [X] T019 [US1] Créer `APP/data/nutrition/ServingsEstimator.kt` avec la table de FR-016 (Plats principaux 350 g ; Pâtes et riz 350 g ; Soupes 300 g ; Boissons 250 g ; Entrées 200 g ; Salades 200 g ; Accompagnements 200 g ; Sandwichs et wraps 200 g ; Tartes et quiches 150 g ; Petits-déjeuners 150 g ; Desserts 120 g ; Gâteaux 100 g ; Goûters 80 g ; Apéritifs 50 g ; Sauces 50 g ; Biscuits et petits gâteaux 25 g) et les règles FR-017/FR-018
- [X] T020 [US1] Créer `APP/data/nutrition/NutritionCalculator.kt` : `estimate(recipe, table): NutritionEstimate` (champs de `data-model.md` : `counted`, `ignored`, `total`, `servings`/`servingsOrigin`, `perServing`, `warnings`), fonction pure
- [X] T021 [US1] Ajouter dans `APP/ui/RecipeDetailScreen.kt` un bandeau sous le titre : « ≈ X kcal / part » + « ≈ N parts (estimé) » ou « N parts », ou « ≈ X kcal pour la recette » + « Indiquer le nombre de parts », ou « Estimation impossible » ; mention « estimation » ; avertissements `PARTIAL`/`CHECK_VALUE` ; calcul mémorisé (`remember(recipe)`) pour rester sous 0,5 s (SC-004)
- [X] T022 [US1] Charger la table au démarrage hors du fil principal dans `APP/MainActivity.kt` (après `LanNetwork.init`) et la passer à la fiche

**Checkpoint**: US1 utilisable seule (MVP) : toutes les recettes affichent une estimation.

---

## Phase 4: User Story 5 - Saisir et modifier le nombre de parts (Priority: P1)

**Goal**: Champ « Nombre de parts » facultatif dans le formulaire ; correction rapide depuis la fiche.

**Independent Test**: Modifier une recette, saisir 6, enregistrer → la fiche divise par 6.

- [X] T023 [P] [US5] Tests Compose dans `IT/AddEditRecipeScreenTest.kt` (ajouts uniquement) : 6 → enregistré avec `servings = 6`, `servingsSource = USER` ; 0, -1, « abc », 101 refusés avec message et bouton Enregistrer désactivé ; vide → `servings = null` ; recette existante → champ pré-rempli ; **modifier le titre d'une recette qui a des `ingredientWeights` (`USER` et `PC`) et un `servingsSource = SOURCE` → tout est conservé à l'identique** ; modifier le texte d'une ligne → seule l'entrée de cette ligne disparaît
- [X] T024 [US5] Ajouter le champ « Nombre de parts » (clavier numérique, 1 à 100, facultatif, tag `form_servings`) dans `APP/ui/AddEditRecipeScreen.kt`. **Le formulaire recrée aujourd'hui la recette de zéro (`Recipe(id = …)`, ligne ~314) : reprendre de `existing` `ingredientWeights` et `servingsSource`** (ce dernier passe à `USER` seulement si la valeur des parts a changé, et à `null` si le champ est vidé) ; à l'enregistrement, retirer de `ingredientWeights` les seules clés qui ne correspondent plus à aucune ligne
- [X] T025 [US5] Rendre « Indiquer le nombre de parts » et « ≈ N parts (estimé) » cliquables dans `APP/ui/RecipeDetailScreen.kt` : petite boîte de saisie, enregistrement via le `RecipeListViewModel.upsert` existant avec `servingsSource = USER`

**Checkpoint**: US1 + US5 = première livraison complète (P1).

---

## Phase 5: User Story 2 - Comprendre et corriger l'estimation (Priority: P2)

**Goal**: Détail par ingrédient, nutriments, lignes non comptées, correction d'un poids conservée.

**Independent Test**: « 1 noix de beurre » non comptée → saisir 10 g → beurre compté, total + ~75 kcal, valeur conservée après réouverture.

- [X] T026 [P] [US2] Tests Compose dans `IT/NutritionDetailSheetTest.kt` : le nouveau total apparaît dès la validation de la saisie, sans autre écran (SC-005) ; lignes comptées avec poids, origine (« écrit », « estimé », « corrigé ») et kcal ; protéines, glucides, lipides par part ; « Non comptés » avec raison lisible ; saisie de 10 g → `onWeightChange` appelé avec `IngredientWeight(10.0, "USER")` et total mis à jour ; mention de source Ciqual présente
- [X] T027 [P] [US2] Tests JVM dans `UT/nutrition/WeightCorrectionTest.kt` : correction conservée si la recette est modifiée sans toucher la ligne ; supprimée si le texte de la ligne change ; jamais écrasée par une réponse `PC`
- [X] T028 [US2] Créer `APP/ui/NutritionDetailSheet.kt` (feuille ouverte depuis le bandeau de T021) : liste comptés / non comptés, nutriments par part (FR-015), saisie ou correction d'un poids (grammes, > 0 et ≤ 10 000), mention « Source : Anses, table Ciqual 2020 »
- [X] T029 [US2] Brancher la correction dans `APP/ui/RecipeDetailScreen.kt` et `APP/MainActivity.kt` : mise à jour de `ingredientWeights` (`src = USER`) puis `upsert`, recalcul immédiat (SC-005)

**Checkpoint**: estimation vérifiable et corrigeable.

---

## Phase 6: User Story 3 - Quantités vagues estimées à l'import par lien (Priority: P3)

**Goal**: Hermes renvoie `servings` et `ingredientWeights` ; l'app les utilise sans jamais faire échouer l'import.

**Independent Test**: Lien « 2 pommes », « pour 6 personnes » → 6 parts, pommes comptées « estimé ».

- [ ] T030 [US3] Sauvegarder `HERMES\recipe_extractor.py` en `.bak.<date>` puis y ajouter `estimate_weights(title, category, ingredients)` : prompt court à Llama 3.1 (`"format": "json"`, `temperature: 0`), nettoyage de la sortie selon `contracts/hermes-estimate.md` (même ordre et même nombre, `grams` > 0 et ≤ 10 000 sinon `null`, `servings` entier 1 à 100 sinon `null`, sortie illisible → tout à `null`)
- [ ] T031 [US3] Ajouter `POST /estimate` dans `handle()` de `HERMES\recipe_extractor.py` (400 si corps invalide ou `ingredients` hors 1 à 60 ; 503 si Ollama ne répond pas ; `reset_idle_timer`, `keep_awake`, `extraction_started/finished` comme `/extract`)
- [ ] T032 [US3] Dans `extract_recipe()` de `HERMES\recipe_extractor.py` : ajouter `"servings"` au prompt d'extraction (« pour 4 personnes » → 4, sinon `null`), appeler `estimate_weights` après l'extraction et ajouter `ingredientWeights` ; toute erreur d'estimation est journalisée et l'extraction réussit quand même (FR-010)
- [ ] T033 [P] [US3] Tests pytest dans `HERMES\test_recipe_extractor.py` (ajouts uniquement, Llama simulé) : nettoyage des sorties (bornes, ordre, valeurs non numériques, JSON illisible), 400/503 de `/estimate`, `/extract` qui réussit quand l'estimation échoue ; les 42 tests existants restent verts
- [ ] T034 [US3] Lire `servings` (1 à 100, sinon ignoré, `servingsSource = SOURCE`) et `ingredientWeights` (→ entrées `src = PC` pour les seules lignes que `IngredientParser` ne sait pas convertir) dans `APP/RecipeJsonParser.kt`
- [ ] T035 [P] [US3] Tests dans `IT/RecipeJsonParserTest.kt` (ajouts uniquement) : `servings` lu ; poids repris pour « 2 pommes » mais pas pour « 200 g de farine » (la quantité écrite fait foi) ; réponse sans ces champs → recette identique à avant ; valeurs hors bornes ignorées
- [ ] T036 [US3] Redémarrer Hermes (arrêt de `recipe_extractor.py`, relancé par `hermes_proxy_launcher.py`) et vérifier dans `HERMES\logs\extractor.log` qu'une extraction réelle renvoie `servings` et `ingredientWeights` ; mesurer SC-003 sur au moins 5 vrais liens fournis par l'utilisatrice (pourcentage de lignes comptées sans saisie, objectif ≥ 85 %), résultat noté dans `specs/001-calories-par-part/quickstart.md`

**Checkpoint**: imports par lien enrichis, rétrocompatibles.

---

## Phase 7: User Story 4 - Rattrapage automatique par le PC (Priority: P3)

**Goal**: Recettes existantes traitées une fois, puis au fil de l'eau ; tout de suite à l'import texte/fichier si le PC répond, sinon par la file.

**Independent Test**: Recette enregistrée avec « 1 pomme » non comptée ; PC joignable → peu après, la pomme est comptée « estimé ».

- [ ] T037 [P] [US4] Ajouter `HermesNetwork.estimate(server, title, category, lines): Result<EstimateResponse>` dans `APP/data/HermesClient.kt` (via `LanNetwork.open`, délai 45 s, format de `contracts/hermes-estimate.md`)
- [ ] T038 [US4] Créer `APP/data/nutrition/WeightEstimationProcessor.kt` : liste des recettes ayant au moins une ligne `VAGUE` ou `NO_PIECE_WEIGHT` **dont l'aliment est reconnu dans la table** (une ligne `UNKNOWN_FOOD` n'est jamais envoyée : sans aliment, un poids ne donne pas de calories) et sans entrée dans `ingredientWeights` ; envoi des seules lignes concernées ; écriture d'une entrée `src = PC` par ligne envoyée, même avec `g = null` (marqueur « déjà envoyé ») ; jamais d'écrasement d'une entrée `USER` ; `servings` repris seulement si `servings` est `NULL` ; PC injoignable → arrêt et « réessayer plus tard » ; ajouter à `APP/data/RecipeDao.kt` les requêtes nécessaires (lecture de toutes les recettes, mise à jour des seules colonnes `ingredient_weights`, `servings`, `servings_source`)
- [ ] T039 [P] [US4] Tests JVM dans `UT/WeightEstimationProcessorTest.kt` (DAO et réseau simulés) : rattrapage de toutes les recettes concernées ; recette entièrement lisible jamais envoyée ; recette déjà traitée non renvoyée ; ligne modifiée seule renvoyée ; entrée `USER` conservée ; PC injoignable → `retryLater` ; progression n / N correcte
- [ ] T040 [US4] Dans `APP/data/ImportQueue.kt`, faire enchaîner au `ImportQueueWorker` le traitement des liens puis `WeightEstimationProcessor` (même réveil, même contrainte Wi-Fi, même nouvel essai) ; exposer la progression (n / N) ; relancer le worker (`kick`) à l'enregistrement d'une recette ayant une ligne vague sans entrée, et au démarrage de l'app s'il reste des recettes à traiter
- [ ] T041 [US4] Afficher « Estimation des quantités : n / N » dans `APP/ui/ImportQueueScreen.kt` tant qu'il reste des recettes à traiter (sans notification) ; test Compose ajouté dans `IT/ImportQueueScreenTest.kt`
- [ ] T042 [US4] Estimation immédiate à l'import texte et fichier dans `APP/ui/TextImportScreen.kt` et `APP/ui/FileImportScreen.kt` : si une ligne est vague, vérifier `/health` (2 s, sans réveil) puis appeler `/estimate` avec l'indicateur « Estimation des quantités… » ; PC absent ou erreur → import sans attendre (la file prendra le relais) ; tests Compose ajoutés dans `IT/TextImportScreenTest.kt` (PC simulé présent, absent, en erreur)

**Checkpoint**: toutes les stories fonctionnent.

---

## Phase 8: Polish & validation

- [ ] T043 Mesurer SC-002 et SC-003 sur une copie de la sauvegarde des recettes (jamais sur la base du téléphone) avec un test JVM ou un script dans `tools/ciqual/measure_coverage.py` : pourcentage de lignes comptées avant et après rattrapage ; compléter `selection.csv` si besoin et regénérer `foods.json`
- [ ] T044 Exécuter `quickstart.md` en entier : tests unitaires, tests Hermes, sauvegarde vérifiée (non vide) de `url_import.xml`, `adb install -r` des deux APK, `am instrument`, restauration des réglages, vérification manuelle §4
- [ ] T045 Lancer `/verifie-tests` et rendre compte à l'utilisatrice ; ne rien committer sans sa demande

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** : T001 → T002 → T003 → T004.
- **Foundational (Phase 2)** : dépend de T004 pour T005 ; T006 → T007 → T008 → T009 ; T010, T011 en parallèle, puis T012, T013 → T014.
- **US1 (Phase 3)** : dépend de la Phase 2.
- **US5 (Phase 4)** : dépend de US1 (bandeau de la fiche).
- **US2 (Phase 5)** : dépend de US1.
- **US3 (Phase 6)** : Hermes (T030–T033) indépendant de l'app, peut démarrer dès la Phase 2 ; T034–T035 dépendent de la Phase 2.
- **US4 (Phase 7)** : dépend de T031 (`/estimate`) et de la Phase 2.
- **Polish (Phase 8)** : après toutes les stories voulues.

### Parallel Opportunities

- Phase 2 : T005, T010, T011 ensemble.
- US1 : T015, T016, T017, T018 (tests) ensemble.
- US3 : côté Hermes (T030–T033) en parallèle de US1/US2 côté app.
- US4 : T037 et T039 ensemble.

### Parallel Example: User Story 1

```text
T015 NutritionCalculatorTest.kt
T016 ServingsEstimatorTest.kt
T017 ReferenceRecipesTest.kt
T018 RecipeDetailNutritionTest.kt
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 + Phase 2.
2. Phase 3 (US1) → les ~80 recettes affichent une estimation, hors ligne.
3. Installer (`adb install -r`), montrer à l'utilisatrice.

### Incremental Delivery

1. US1 + US5 (P1) → livraison 1.
2. US2 (P2) → livraison 2 (détail et corrections).
3. US3 + US4 (P3) → livraison 3 (Hermes et rattrapage).
4. Chaque livraison : tests verts, `/verifie-tests`, installation sans perte de données.
