# Tasks: Mes produits Chronodrive (US1 à US3)

**Input**: Design documents from `/specs/003-chrono-panier-favoris/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (`favorites-file.md`, `chrono-link.md`), quickstart.md

**Périmètre** : User Stories US1 (P1), US2 (P1) et US3 (P2). US4 (connexion, bouton « Panier », « Mettre à jour mes favoris » depuis le compte) est une phase ultérieure : voir « Phase ultérieure : US4 » en fin de fichier. FR-001b, FR-008, FR-009, FR-010, SC-004 et les scénarios 4 et 5 de US1 relèvent d'US4 et n'ont donc volontairement aucune tâche ici.

**Tests**: Inclus. Le projet écrit toujours des tests JVM et des tests Compose/Room sur le téléphone ; la règle permanente de l'utilisatrice impose `/verifie-tests` avant de déclarer le travail terminé ; la spec fixe des critères mesurables (SC-001 à SC-003, SC-005).

**Organization**: Tâches groupées par user story, dans l'ordre des priorités (US1, US2 : P1 ; US3 : P2).

Abréviations de chemins :
- `APP` = `app/src/main/java/com/example/recipelibrary`
- `UT` = `app/src/test/java/com/example/recipelibrary`
- `IT` = `app/src/androidTest/java/com/example/recipelibrary`
- `SPEC` = `specs/003-chrono-panier-favoris`

Décisions prises (à appliquer, pas à rediscuter) :
- Base Room en **version 9** (`MIGRATION_8_9`) : la version 8 est prise par la feature 001 (T047, aliment de remplacement).
- Les noms de produits viennent des slugs, sans accents ni majuscules : accepté pour cette phase.
- L'écran « Mes produits Chronodrive » s'ouvre depuis le menu de l'onglet Courses.
- Les ~5 favoris manquants sont ajoutés par l'utilisatrice à `SPEC/favoris-releve-2026-11-22.txt` ; un réimport les ajoute.

Règles pour toutes les tâches : tests sur le téléphone par `am instrument` uniquement (jamais `connectedAndroidTest`), `url_import.xml` sauvegardé (taille vérifiée) puis restauré, `adb install -r` ; aucune écriture dans les tables existantes (`recipes`, `pending_imports`, `menu_entries`, `shopping_checks`, `shopping_extras`) ; jamais de `fallbackToDestructiveMigration` ; aucun test existant affaibli (seules modifications permises des tests existants : ajout de `MIGRATION_8_9` à la liste de `DatabaseMigrationTest`, à signaler à l'utilisatrice) ; rien de commité sans demande ; les commentaires et messages sont en français.

## Format: `[ID] [P?] [Story] Description`

- **[P]** : peut être fait en parallèle (fichiers différents, pas de dépendance sur une tâche en cours)
- **[Story]** : user story concernée (US1 à US3)

---

## Phase 1: Setup

**Purpose**: Contrôle du point de départ et liens Chronodrive réutilisables.

- [ ] T001 Vérifier le point de départ avant tout code : `APP/data/AppDatabase.kt` est en `version = 8`, `APP/data/Migrations.kt` contient `MIGRATION_7_8` (feature 001 T047) et `APP/data/DatabaseProvider.kt` l'enregistre. Si ce n'est pas le cas (T047 pas encore faite), s'arrêter et le signaler à l'utilisatrice : ne jamais prendre la version 8 ; en cas de conflit sur `AppDatabase.kt`, `Migrations.kt`, `DatabaseProvider.kt`, `DatabaseMigrationTest.kt`, `MenuScreen.kt` ou `MainActivity.kt` (fichiers aussi touchés par 001 et 002), fusionner à la main sans rien retirer
- [ ] T002 [P] Ajouter dans `APP/data/shopping/ChronodriveSearch.kt` (contracts/chrono-link.md) : `productAppUri(id: Long): String?` → `chronodrive://product/<id>` et `productWebUrl(id: Long): String?` → `https://www.chronodrive.com/product/<id>`, tous deux `null` si `id` ≤ 0 (« numéro non numérique refusé » : la saisie texte est convertie par `toLongOrNull()` en amont) ; `choiceKey(item: ShoppingItem): String` (= `item.key`, `food:<code>` ou `txt:<texte normalisé>`) et `choiceKeyForExtra(text: String): String` = `"txt:" + Normalize.text(termForExtra(text))` (research R4 : jamais `extra:<id>`) ; ne rien changer au comportement existant de `term`, `termForExtra`, `searchUrl`, `FAVORITES_URL`, `PACKAGE`
- [ ] T003 [P] Tests JVM dans `UT/chronodrive/ChronodriveLinksTest.kt` : `productAppUri(122974)` = `chronodrive://product/122974` ; `productWebUrl(122974)` = `https://www.chronodrive.com/product/122974` ; 0 et négatif → `null` ; `choiceKey` d'un article `food:` et d'un article `txt:` inchangé ; `choiceKeyForExtra("2 papier toilette")` = `choiceKey` de l'article calculé « papier toilette » ; `choiceKeyForExtra("5 bananes")` ne contient pas la quantité ; les tests existants de `ChronodriveSearch` restent inchangés et verts

---

## Phase 2: Foundational (bloquant pour toutes les stories)

**Purpose**: Stockage des favoris et des choix sans perte de données, analyse du fichier, correspondance et fusion en Kotlin pur.

**⚠️ CRITICAL**: aucune story ne démarre avant la fin de cette phase.

- [ ] T004 Créer les entités Room dans `APP/data/chronodrive/` : `ChronodriveProduct.kt` (table `chronodrive_products` : `product_id` INTEGER clé primaire, `name` TEXT NOT NULL « sans la marque, déduit du slug », `brand` TEXT nullable, `aisle` TEXT NOT NULL, `still_favorite` INTEGER NOT NULL DEFAULT 1, `imported_at` INTEGER NOT NULL en millisecondes) et `ProductChoice.kt` (table `product_choices` : `item_key` TEXT clé primaire `food:<code Ciqual>` ou `txt:<texte normalisé>`, `product_id` INTEGER NOT NULL sans clé étrangère, `chosen_at` INTEGER NOT NULL en millisecondes, index `index_product_choices_product_id` sur `product_id`) ; mêmes conventions que `APP/data/menu/MenuEntities.kt`
- [ ] T005 Créer `APP/data/chronodrive/ChronodriveDao.kt` : produits (liste pour la correspondance et l'écran, `getById`, insertion/mise à jour en lot, passage de `still_favorite` à 0 pour une liste de numéros), choix (flux de tous les choix, `getChoice(itemKey)`, remplacer par `itemKey`, supprimer par `itemKey` pour « oublier », `countForProduct(productId)`) ; aucune requête de suppression sur `chronodrive_products` (jamais supprimé par un import, FR-001) ; aucune requête ne touche les tables existantes
- [ ] T006 Ajouter `MIGRATION_8_9` dans `APP/data/Migrations.kt` (les deux `CREATE TABLE IF NOT EXISTS` et le `CREATE INDEX IF NOT EXISTS`, exactement le schéma généré par Room d'après data-model.md, rien d'autre : aucune table existante touchée), passer `version = 9` et ajouter les deux entités et `chronodriveDao()` dans `APP/data/AppDatabase.kt` (en conservant les entités et DAO ajoutés par 001 en version 8), enregistrer la migration à la suite de `MIGRATION_7_8` dans `APP/data/DatabaseProvider.kt`
- [ ] T007 Étendre `IT/DatabaseMigrationTest.kt` avec un test 8 → 9 (research R5, pas de `MigrationTestHelper`) : base Room en mémoire avec recette (tous les champs remplis dont `servings` et `ingredient_weights`), entrée de menu, coche et ajout à la main insérés, relevé des colonnes des deux nouvelles tables (`PRAGMA table_info`), suppression des deux tables, exécution de `MIGRATION_8_9.migrate(...)`, puis vérification : (a) les anciennes lignes sont relues à l'identique, (b) les nouvelles tables existent avec les mêmes colonnes (nom, type, NOT NULL, valeur par défaut) qu'avant suppression et l'index `index_product_choices_product_id` existe, (c) le DAO écrit et relit un produit et un choix, (d) les nouvelles tables sont vides après migration ; ajouter `MIGRATION_8_9` à la liste des migrations des tests existants qui ouvrent la base avec Room (seule modification permise des tests existants, à signaler à l'utilisatrice) ; ne rien affaiblir
- [ ] T008 [P] Copier `SPEC/favoris-releve-2026-11-22.txt` (dans l'état où il est au moment de la tâche, ajouts de l'utilisatrice compris) vers `app/src/test/resources/chronodrive/favoris-releve.txt` : copie figée servant de jeu de test JVM ; relever dans un commentaire en tête du test les chiffres de cette copie (nombre de produits, de rayons, premier et dernier numéro) et ne jamais coder en dur ceux du contrat (120 produits, 12 rayons, 189100, 500297) si la copie est plus récente que le relevé initial
- [ ] T009 [P] Tests JVM d'abord, dans `UT/chronodrive/FavoritesFileParserTest.kt` (contracts/favorites-file.md) : relevé figé de T008 (nombre de produits et de rayons de la copie, `banane` = 365920 et 305254, `emmental rape` = 368958 et 82295) ; slug → marque et nom (`365920 banane` → pas de marque, `banane` ; `55785 lactel--matin-leger---lait-sans-lactose-12-mg-uht` → `lactel`, `matin leger - lait sans lactose 12 mg uht` ; `173555 happy-bio--graines-de-chia-bio` → `happy bio`, `graines de chia bio`) ; titre de rayon sans parenthèse finale (`## Restent ~5 : …`) sans produit valide : n'ajoute rien ; compteurs `(20 sur 21)` jamais contrôlés ; ligne `# commentaire` et ligne vide ignorées ; produit avant le premier `##` → rayon « Autre » ; fins de ligne `\r\n` ; BOM ; élément mal formé au milieu → ignoré et compté (« 2 éléments ignorés ») ; même numéro deux fois → le premier gagne ; fichier vide, sans aucun produit valide, ou de plus de 1 Mo → résultat « invalide » avec le message « Ce fichier ne contient aucun favori Chronodrive reconnu »
- [ ] T010 Créer `APP/data/chronodrive/FavoritesFileParser.kt` (Kotlin pur, sans Android) : texte → `ParsedFavorites(products, ignored)` ou invalide, règles exactes de contracts/favorites-file.md (numéro entier positif, espaces, slug `[a-z0-9-]+` ; `---` → ` - ` puis `-` → espace, espaces multiples réduites ; marque = avant le premier `--` avec `-` → espaces) ; fait passer T009
- [ ] T011 [P] Tests JVM d'abord, dans `UT/chronodrive/ProductMatcherTest.kt` (FR-002, research R3) : « eau » ne correspond pas à « gateau » ; « oeuf » correspond à « oeufs de poules » ; accents et majuscules ignorés (« Crème fraîche » = « creme fraiche epaisse legere ») ; pluriel simple (« bananes » = « banane ») ; tous les mots significatifs du terme doivent être dans le nom, en mots entiers ; mots vides et mots d'un caractère ignorés (`de`, `du`, `des`, `la`, `le`, `les`, `un`, `une`, `et`, `au`, `aux`, `en`, `a`) ; un terme sans mot significatif → aucun produit ; la marque n'entre pas dans la correspondance (le terme « auchan » ne retrouve pas un produit dont seule la marque est `auchan`) ; « banane » → les 2 produits du relevé figé ; « emmental rape » → 2 produits ; tri alphabétique sur le nom puis numéro ; `still_favorite = 0` toujours candidat
- [ ] T012 Créer `APP/data/chronodrive/ProductMatcher.kt` (Kotlin pur, réutilise `Normalize.words` de `APP/data/nutrition/Normalize.kt`) : `candidates(term, products): List<ChronodriveProduct>` selon research R3 ; fait passer T011
- [ ] T013 [P] Tests JVM d'abord, dans `UT/chronodrive/FavoritesMergeTest.kt` (research R6, data-model.md « Règles ») : premier import → tous « ajoutés » ; réimport identique → 0 ajouté, N déjà connus ; 3 nouveaux → « 3 ajoutés, N déjà connus » ; numéro connu → `name`, `brand`, `aisle`, `imported_at` mis à jour et `still_favorite = 1` ; numéro absent du fichier → conservé avec `still_favorite = 0` et compté `markedGone` ; un produit marqué disparu qui revient repasse à 1 ; jamais de suppression ; messages exacts : « 120 produits Chronodrive importés » (premier import, avec le nombre réel), « 3 ajoutés, 120 déjà connus », + « 1 plus en favori » si `markedGone > 0`, + « 2 éléments ignorés » si `ignored > 0`
- [ ] T014 Créer `APP/data/chronodrive/FavoritesMerge.kt` (Kotlin pur) : `merge(existing, parsed, now): MergePlan` avec `ImportReport(added, known, markedGone, ignored)` et le texte du message ; fait passer T013
- [ ] T015 Créer `APP/data/chronodrive/ChronodriveRepository.kt` : `importFavorites(text)` = analyse complète avant toute écriture (T010), fusion (T014), écriture en une seule transaction `withTransaction` (fichier invalide : rien d'écrit) ; flux des produits et des choix ; `setChoice(itemKey, productId)`, `forgetChoice(itemKey)` ; ne touche aucune table existante
- [ ] T016 [P] Tests sur le téléphone de `ChronodriveDao` et du dépôt dans `IT/ChronodriveDaoTest.kt` (base en mémoire) : import puis réimport (nouveaux ajoutés, connus mis à jour, absents marqués `still_favorite = 0` sans suppression) ; fichier invalide → base inchangée ; un choix survit à un réimport où son produit a disparu ; remplacer un choix ; oublier un choix ; `countForProduct` ; une exception en milieu d'écriture annule toute la transaction ; les tables `recipes`, `menu_entries`, `shopping_checks`, `shopping_extras` sont inchangées après toutes ces opérations

**Checkpoint**: base en version 9, données existantes intactes, favoris et choix stockables, analyse et correspondance testées en JVM.

---

## Phase 3: User Story 1 - Importer mes favoris Chronodrive (Priority: P1) 🎯 MVP

**Goal**: importer le fichier de favoris, voir « 120 produits Chronodrive importés » (nombre réel du fichier), réimporter sans rien perdre ; écran « Mes produits Chronodrive » ouvert depuis le menu de l'onglet Courses.

**Independent Test**: importer le relevé : « N produits Chronodrive importés » et la liste par rayon ; réimporter un fichier avec 3 produits de plus : « 3 ajoutés, N déjà connus », rien de perdu ; fichier vide : message, rien de modifié.

### Tests for User Story 1

- [ ] T017 [P] [US1] Compose UI tests dans `IT/ChronodriveProductsScreenTest.kt` (écran piloté par callbacks, sans accès réel au sélecteur de fichiers) : liste vide → invitation à « Importer mes favoris » ; produits affichés groupés par rayon avec nom, marque et numéro ; produit `still_favorite = 0` affiché « plus en favori » ; le message de résultat d'import (« N produits Chronodrive importés », « 3 ajoutés, N déjà connus », fichier invalide) s'affiche ; toucher « Importer mes favoris » appelle le callback de sélection de fichier ; aucune action ne modifie les recettes
- [ ] T018 [P] [US1] Test Compose additif dans `IT/ShoppingListScreenTest.kt` (aucun test existant modifié) : le menu de l'onglet Courses contient « Mes produits Chronodrive » ; le toucher appelle `actions.onOpenProducts` ; les autres éléments de l'écran (Partager, ajout à la main, liste) sont inchangés

### Implementation for User Story 1

- [ ] T019 [US1] Créer `APP/ui/ChronodriveViewModel.kt` (ou compléter `APP/ui/MenuViewModel.kt` si plus simple, sans casser son contrat) : expose produits et choix du dépôt ; `import(uri)` lit le fichier UTF-8 via `ContentResolver` avec garde-fou de 1 Mo (au-delà : invalide), appelle `importFavorites`, expose le message de résultat ; toute erreur de lecture → message « Ce fichier ne contient aucun favori Chronodrive reconnu », rien de modifié ; Factory comme `MenuViewModel.Factory`
- [ ] T020 [US1] Créer `APP/ui/ChronodriveProductsScreen.kt` (partie US1) : bouton « Importer mes favoris » (`ActivityResultContracts.OpenDocument`, types `text/*`, même mécanisme que `APP/ui/FileImportScreen.kt`), message de résultat, liste des produits par rayon (nom, marque, numéro, mention « plus en favori ») ; Composable sans état pour être testable (T017) ; style cohérent avec `MenuScreen.kt` (couleurs `Violet`, `Grey`)
- [ ] T021 [US1] Ajouter dans `APP/MainActivity.kt` la destination `chronodrive_products` (retour arrière vers l'onglet Courses, même `containerColor`/`ScreenBg` que les autres écrans) et la création du `ChronodriveViewModel`
- [ ] T022 [US1] Ajouter dans `APP/ui/MenuScreen.kt` (vue `ShoppingListView`, rangée du haut à côté de « Partager ») un bouton « ⋮ » ouvrant un `DropdownMenu` avec l'entrée « Mes produits Chronodrive », et dans `ShoppingActions` le callback `onOpenProducts: () -> Unit = {}` (valeur par défaut : les tests et appels existants restent valides) ; relier à la navigation de T021 ; l'onglet Menu et les autres boutons ne changent pas

**Checkpoint**: US1 fonctionnelle et testable seule : import, réimport, liste par rayon, aucune perte.

---

## Phase 4: User Story 2 - Chrono ouvre directement la fiche de mon produit (Priority: P1)

**Goal**: « Chrono » ouvre la fiche du produit retenu ; un seul candidat : retenu ; plusieurs : l'utilisatrice choisit une fois ; aucun : comportement 002 ; le nom reste copié.

**Independent Test**: avec les favoris importés et « banane » dans la liste, « Chrono » propose les deux bananes (365920 et 305254), le choix ouvre la fiche, le second « Chrono » ouvre directement la même fiche sans question.

### Tests for User Story 2

- [ ] T023 [P] [US2] Tests JVM d'abord, dans `UT/chronodrive/ChronodriveResolutionTest.kt` (contracts/chrono-link.md, data-model.md « Résolution ») : choix existant → `Retained(produit)` (même si le produit est `still_favorite = 0`) ; pas de choix + 1 candidat → `Single` ; pas de choix + plusieurs → `Several` (triés), jamais de choix automatique entre plusieurs (FR-004) ; aucun candidat ou aucun favori importé → `None` ; un choix dont le numéro n'existe plus dans `chronodrive_products` est ignoré (la résolution continue comme sans choix, le choix n'est pas supprimé) ; article avec quantité (« 5 bananes » : terme « banane ») ; article ajouté à la main (« 2 papier toilette ») résolu sur la clé `txt:papier toilette` de T002
- [ ] T024 [P] [US2] Tests Compose additifs dans `IT/ShoppingListScreenTest.kt` (aucun test existant modifié ; valeurs par défaut des nouveaux callbacks = comportement 002) : « Chrono » avec produit retenu déclenche `onOpenProduct` avec le bon numéro et ne montre pas de boîte ; plusieurs candidats : la boîte de choix s'affiche (nom, marque, rayon, numéro), toucher un produit l'enregistre et ouvre sa fiche, « Aucun de ceux-là » appelle `onChronodrive(term)` (comportement actuel) sans rien enregistrer ; aucun candidat : `onChronodrive(term)` seul ; l'article envoyé reste mis en évidence au retour et se décoche de la mise en évidence à la coche (comportement 002) ; article ajouté à la main : même résolution
- [ ] T025 [P] [US2] Test Compose dans `IT/ChronodriveProductPickerTest.kt` : la boîte liste nom, marque, rayon et numéro de chaque candidat (deux « banane » distinguées par leur numéro), le titre nomme l'article, « Aucun de ceux-là » et « Annuler » appellent leurs callbacks, rien n'est choisi au hasard
- [ ] T026 [P] [US2] Test sur le téléphone dans `IT/ChronodriveLauncherTest.kt` (contexte factice `ContextWrapper` qui enregistre les `startActivity` et peut lever `ActivityNotFoundException`) : `openProduct` lance d'abord `chronodrive://product/<id>` avec `setPackage("com.ikomobi.chronodrive")` ; si l'application est absente, lance `https://www.chronodrive.com/product/<id>` ; si rien ne s'ouvre, retombe sur le comportement actuel (favoris, accueil, recherche du site) ; aucun intent ne porte d'action sur un panier ni sur un compte (FR-005)

### Implementation for User Story 2

- [ ] T027 [US2] Ajouter à `APP/data/chronodrive/ProductMatcher.kt` la résolution `resolve(itemKey, term, choice, products): ChronodriveResolution` (`Retained` | `Single` | `Several` | `None`) et dans `ChronodriveRepository` `resolveChrono(itemKey, term)` : pour `Single`, enregistre le choix avant de répondre ; pour `Several`, n'enregistre rien ; fait passer T023
- [ ] T028 [US2] Étendre `APP/ui/ChronodriveLauncher.kt` avec `openProduct(context, product, itemName)` (contracts/chrono-link.md) : 1) copier le nom de l'article (libellé « Chronodrive », FR-006), 2) `Intent(ACTION_VIEW, productAppUri(id))` + `setPackage(ChronodriveSearch.PACKAGE)` + `FLAG_ACTIVITY_NEW_TASK`, 3) `ActivityNotFoundException` → `productWebUrl(id)` dans le navigateur, 4) si rien ne s'ouvre → comportement actuel `open(context, term)` puis « Impossible d'ouvrir Chronodrive » ; message « Fiche de « <nom du produit> » : appuie sur + » (jamais de formulation laissant croire à un ajout au panier) ; `open(context, term)` existant inchangé
- [ ] T029 [P] [US2] Créer `APP/ui/ProductPickerDialog.kt` : boîte de choix (titre = nom de l'article ; une ligne par candidat : nom, marque, rayon, numéro ; boutons « Aucun de ceux-là » et « Annuler ») ; Composable sans état, réutilisable par US3 ; fait passer T025
- [ ] T030 [US2] Câbler « Chrono » dans `APP/ui/MenuScreen.kt` (`ShoppingActions` et `ShoppingListView`) : nouveaux callbacks avec valeurs par défaut sûres (`resolveChrono: suspend (key: String, term: String) -> ChronodriveResolution = { ChronodriveResolution.None }`, `onOpenProduct: (product: ChronodriveProduct, itemName: String) -> Unit = { _, _ -> }`) ; le bouton « Chrono » calcule la clé (`choiceKey` / `choiceKeyForExtra`, T002), mémorise `sentKey` comme aujourd'hui, appelle `resolveChrono` : `Retained`/`Single` → `onOpenProduct`, `Several` → `ProductPickerDialog` (choix → `setChoice` puis `onOpenProduct` ; « Aucun de ceux-là » → `onChronodrive(term)`), `None` → `onChronodrive(term)` ; le callback `onChronodrive(term)` garde sa signature (tests existants intacts) ; fait passer T024
- [ ] T031 [US2] Brancher dans `APP/MainActivity.kt` et `APP/ui/MenuViewModel.kt` (ou `ChronodriveViewModel`) : `resolveChrono` → dépôt, choix d'un candidat → `setChoice`, `onOpenProduct` → `ChronodriveLauncher.openProduct` ; sans rien changer au reste de `ShoppingActions`

**Checkpoint**: US1 et US2 fonctionnent : import puis « Chrono » ouvre la fiche, choix mémorisé.

---

## Phase 5: User Story 3 - Voir et corriger mes choix (Priority: P2)

**Goal**: voir les correspondances article → produit, changer, oublier, en attribuer une à la main depuis les favoris importés.

**Independent Test**: changer « banane » du 365920 au 305254 : le prochain « Chrono » ouvre la fiche du 305254 ; oublier « banane » : « Chrono » repropose les deux.

### Tests for User Story 3

- [ ] T032 [P] [US3] Compose UI tests additifs dans `IT/ChronodriveProductsScreenTest.kt` : section « Mes choix » listant chaque article avec le produit retenu (libellé lisible de l'article, nom du produit, numéro) ; « Changer » ouvre la liste de tous les favoris par rayon avec recherche par texte, le produit touché remplace le choix ; « Oublier » retire la ligne et appelle le callback ; choix dont le produit est `still_favorite = 0` : mention « plus en favori » ; aucun choix : message vide ; la section « Mes favoris » (par rayon, US1) reste affichée
- [ ] T033 [P] [US3] Tests Compose additifs dans `IT/ShoppingListScreenTest.kt` : appui long sur un article calculé ou ajouté à la main → entrée « Choisir mon produit Chronodrive » → liste de tous les favoris importés, par rayon, avec recherche par texte ; le produit touché remplace le choix de l'article et l'application Chronodrive n'est pas ouverte (`onOpenProduct` non appelé) ; le tap court (détail des recettes sources, modification d'un ajout à la main) et la coche restent inchangés ; sans favoris importés : message invitant à importer
- [ ] T034 [P] [US3] Tests JVM dans `UT/chronodrive/ChoiceLabelTest.kt` : libellé lisible d'une clé de choix : `food:<code>` → nom de l'aliment trouvé dans `FoodTable.foods` par code, `txt:<texte>` → le texte, clé inconnue → la clé telle quelle ; l'article de la liste courante utilise son `label` quand il existe

### Implementation for User Story 3

- [ ] T035 [US3] Compléter `APP/data/chronodrive/ChronodriveRepository.kt` : flux « choix + produit retenu » (jointure en mémoire sur `product_id`, produit inconnu toléré), `setChoice` à la main sur n'importe quel favori (remplace la ligne), `forgetChoice` (supprime uniquement la ligne du choix) ; fonction pure de libellé de choix de T034 (dans `APP/data/chronodrive/ChoiceLabel.kt`) qui lit la table d'aliments chargée (`FoodTable.foods`, lecture seule ; ajouter un accesseur par code à `APP/data/nutrition/FoodTable.kt` seulement si utile, sans changer le reste) ; fait passer T034
- [ ] T036 [US3] Compléter `APP/ui/ChronodriveProductsScreen.kt` (US3) : section « Mes choix » (article → produit retenu, « Changer », « Oublier »), section « Mes favoris » par rayon (US1) ; réutiliser un sélecteur de favoris à recherche par texte groupé par rayon (`APP/ui/ProductPickerDialog.kt` ou composable frère dans le même fichier) ; fait passer T032
- [ ] T037 [US3] Ajouter dans `APP/ui/MenuScreen.kt` l'appui long sur une ligne d'article (`combinedClickable`, sans retirer le tap court ni la coche) ouvrant « Choisir mon produit Chronodrive » (contracts/chrono-link.md « Appui long ») ; callbacks `onChooseProduct(key, productId)` et `favorites` avec valeurs par défaut sûres dans `ShoppingActions` ; brancher dans `APP/MainActivity.kt` ; fait passer T033

**Checkpoint**: les trois stories fonctionnent ; les choix se voient, se changent et s'oublient.

---

## Phase 6: Polish & Cross-Cutting Concerns (vérification finale)

**Purpose**: preuves que rien n'est cassé ni perdu.

- [ ] T038 Lancer tous les tests JVM : `.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Java\jdk-17" :app:testDebugUnitTest` ; attendu tout vert, dont `FavoritesFileParserTest`, `ProductMatcherTest`, `FavoritesMergeTest`, `ChronodriveResolutionTest`, `ChoiceLabelTest`, `ChronodriveLinksTest` et les tests des features 001 et 002 ; une panne dans un test existant se corrige dans le code, jamais en changeant le test
- [ ] T039 Construire les APK : `:app:assembleDebug :app:assembleDebugAndroidTest` (même commande, même JDK 17)
- [ ] T040 Tests sur le téléphone sans perte de données (quickstart §3) : 1) sauvegarder `url_import.xml` (`adb shell "run-as com.example.recipelibrary cat shared_prefs/url_import.xml > /data/local/tmp/url_import.xml; wc -c /data/local/tmp/url_import.xml"`) et vérifier qu'il n'est pas vide ; 2) couper le volume média (`adb shell cmd media_session volume --stream 3 --set 0`, valeur d'origine relevée avant) ; 3) `adb install -r` des deux APK (jamais de désinstallation) ; 4) `adb shell svc power stayon usb` ; 5) `adb shell am instrument -w com.example.recipelibrary.test/androidx.test.runner.AndroidJUnitRunner` (jamais `connectedAndroidTest`) ; 6) `adb shell svc power stayon false`, restaurer `url_import.xml` et vérifier sa taille, rétablir le volume média ; attendu tout vert, dont `DatabaseMigrationTest` (8 → 9 ET 7 → 8 de la feature 001), `ChronodriveDaoTest`, `ChronodriveProductsScreenTest`, `ChronodriveProductPickerTest`, `ShoppingListScreenTest` ; consigner les résultats
- [ ] T041 Vérification de la migration sur une copie (quickstart §4, SC-005) : copier la base du téléphone avant installation (`run-as … cat databases/recipes.db* > /data/local/tmp/…`, puis `adb pull`), installer, ouvrir l'app, recopier et comparer : même nombre de recettes, d'entrées de menu, de coches et d'ajouts à la main, contenu identique, version 9 ; ne jamais travailler sur la base du téléphone elle-même ; vérifier aussi que `url_import.xml` est inchangé
- [ ] T042 Parcours à la main avec l'utilisatrice (quickstart §5) : placer le fichier de favoris à jour (`SPEC/favoris-releve-2026-11-22.txt`, avec les ~5 produits ajoutés) sur le téléphone ; US1 : import (« N produits Chronodrive importés »), réimport du même fichier (« 0 ajouté, N déjà connus »), fichier modifié, fichier vide ; US2 : « banane » → deux candidats, choix, fiche ouverte dans l'application Chronodrive, nom de l'article copié (le coller pour vérifier, sur chaque chemin : FR-006), second « Chrono » direct, article à un seul favori (« ciboulette »), article sans favori (comportement 002), application absente ou lien refusé (fiche dans le navigateur) ; US3 : changer, oublier, appui long ; SC-001 (fiche ouverte en moins de 3 secondes), SC-002 (au moins 50 % des articles de sa liste réelle ont un candidat, aucun rattachement absurde), SC-003 (second passage sans question) ; recettes, menu, coches et réglages d'import identiques (SC-005)
- [ ] T043 Lancer `/verifie-tests` (suite complète + relecture du diff : aucun test affaibli, désactivé ou réaligné ; seule modification d'un test existant : ajout de `MIGRATION_8_9` à `DatabaseMigrationTest`, à signaler explicitement à l'utilisatrice) avant de déclarer le travail terminé ou de commiter ; ne rien commiter sans demande

---

## Phase ultérieure : US4 (hors périmètre de ce fichier)

US4 (connexion à Chronodrive, bouton « Panier », « Mettre à jour mes favoris » depuis le compte : FR-001b, FR-008 à FR-010, SC-004, scénarios 4 et 5 de US1) n'est pas planifiée ici. Étape 0 : **étude de faisabilité** (research.md R8) : API d'ajout au panier et de lecture des favoris observée depuis le site, connexion et durée de vie du jeton sans conserver le mot de passe, conditions d'utilisation, stabilité des réponses. Si le résultat est favorable : `/speckit-plan` puis `/speckit-tasks` pour cette phase (nouvelle version de la base, table à part pour le jeton) ; sinon US4 est abandonnée et US2 reste la solution. Rien dans les tâches ci-dessus ne bloque cette suite : les choix de produit servent tels quels au bouton « Panier ».

---

## Dependencies & Execution Order

- **Setup** (T001 → T002, T003 en parallèle) puis **Foundational** (T004 → T005 → T006 → T007 ; T008 → T009 → T010 ; T011 → T012 ; T013 → T014 ; T015 après T005, T010 et T014 ; T016 après T015). Les chaînes parser, correspondance et fusion sont indépendantes de la chaîne base.
- **US1** après Foundational ; **US2** après Foundational (indépendante d'US1 pour la logique, mais testée avec des favoris importés par US1 : à livrer après) ; **US3** après US2 (réutilise `ProductPickerDialog` et `setChoice`) et US1 (écran).
- À l'intérieur d'une story : tests d'abord (doivent échouer), puis dépôt, puis UI, puis câblage.
- **Polish** en dernier ; T043 après tout le reste.
- Fichiers partagés à séquencer : `MenuScreen.kt` (T022, T030, T037), `MainActivity.kt` (T021, T031, T037), `ChronodriveProductsScreen.kt` (T020, T036), `ChronodriveRepository.kt` (T015, T027, T035), `ShoppingListScreenTest.kt` (T018, T024, T033), `ChronodriveProductsScreenTest.kt` (T017, T032).

## Parallel Execution Examples

- Setup : T002 et T003 ensemble.
- Foundational : T008, T009, T011, T013 (tests et fixture, fichiers différents) ensemble ; T016 en parallèle de la fin de la phase une fois T015 faite.
- US1 : T017 et T018 ensemble.
- US2 : T023, T024, T025, T026 ensemble ; T029 en parallèle de T028.
- US3 : T032, T033, T034 ensemble.

## Implementation Strategy

- **MVP** : Setup + Foundational + US1 + US2 (le gain demandé : « Chrono » ouvre la fiche du produit habituel). US1 seule livre déjà l'import et la liste.
- **Incrément** : US3 (correction des choix) ensuite ; l'usage courant fonctionne sans elle.
- Chaque phase se termine sur un point de contrôle testable seul ; ne passer à la suivante qu'avec les tests de la précédente verts.
- Données : migration par `CREATE TABLE` seulement, copie de base vérifiée avant installation, aucune désinstallation.
