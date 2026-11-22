# Tasks: Onglet « Ajouter » unique

**Input**: Design documents from `/specs/005-onglet-ajouter/`
**Prerequisites**: plan.md, spec.md, research.md, contracts/ (navigation-ui.md, share.md), quickstart.md (pas de data-model.md : aucune donnée touchée)

**Tests**: Inclus. Nouveau test Compose `AddTabScreenTest` ; `SharedTextTest` et tous les tests existants restent inchangés. La règle permanente de l'utilisatrice impose `/verifie-tests` avant de déclarer le travail terminé.

**Organization**: Une seule user story (US1, P1), précédée d'une phase Fondation (écran conteneur et icône) et suivie de la vérification. La partie « partage » (scénarios 5 et 6, FR-005, FR-005b) est déjà livrée le 2026-11-22 ; seul son point d'entrée UI est repris en T009.

Abréviations de chemins :
- `APP` = `app/src/main/java/com/example/recipelibrary`
- `IT` = `app/src/androidTest/java/com/example/recipelibrary`
- `UT` = `app/src/test/java/com/example/recipelibrary`

Règles pour toutes les tâches : tests sur le téléphone par `am instrument` uniquement (jamais `connectedAndroidTest`), `adb install -r`, `url_import.xml` (préférences d'import, **pas** une route) sauvegardé avec sa taille vérifiée puis restauré, volume média coupé pendant les tests ; aucune modification de `UrlImportScreen`, `FileImportScreen`, `PhotoImportScreen`, de `SharedText.kt` ni de `SharedTextTest` ; aucune donnée ni migration Room ; rien de commité sans demande.

## Format: `[ID] [P?] [Story] Description`

- **[P]** : parallélisable (fichiers différents, pas de dépendance sur une tâche en cours)
- **[Story]** : user story concernée (US1)

---

## Phase 1: Setup

**Purpose**: Partage vers l'app, déjà livré avant cette feature (à ne pas refaire).

- [X] T001 [US1] (fait le 2026-11-22) Classification du texte partagé : `APP/data/share/SharedText.kt` (`PageLink`, `ImageLink`, `RecipeJson`, `RecipeText`, `Unknown`), couverte par `UT/share/SharedTextTest.kt` (scénarios 5 et 6 côté classification, FR-005b)
- [X] T002 [US1] (fait le 2026-11-22) Traitement par type dans `APP/MainActivity.kt` (`handleIncomingIntent`, `handleSharedText`, dialogue d'image « Lire la recette » / « Photo d'une recette », texte de recette et JSON vers l'écran `add`, toast « Rien à importer dans ce partage ») et `UrlImportScreen(sharedUrl, onSharedUrlConsumed)` qui colle et lance le lien (FR-005, FR-005b)

---

## Phase 2: Foundational (bloquant pour US1)

**Purpose**: Écran conteneur testable seul et icône, sans toucher à la navigation.

- [X] T003 [P] [US1] (2026-11-22 : remplacée par la tuile `ic_nav_url` réutilisée, choix de l'utilisatrice) Créer l'icône vecteur « + » `app/src/main/res/drawable/ic_nav_add.xml`, dans le style des `ic_nav_*` existantes (même taille de viewport et même teinte) ; **ne pas supprimer** `ic_nav_url`, `ic_nav_file`, `ic_nav_photo` : elles sont encore utilisées comme image d'en-tête dans `UrlImportScreen.kt` (l. 187), `FileImportScreen.kt` (l. 59) et `PhotoImportScreen.kt` (l. 78) (décision de l'utilisatrice)
- [X] T004 [US1] Créer `APP/ui/AddTabScreen.kt` : `enum class AddMode { URL, FILE, PHOTO }` et `AddTabScreen(mode, onModeChange, url, file, photo)` (contrat `contracts/navigation-ui.md`) ; sélecteur `TabRow` Material 1.5 à trois `Tab` « URL », « Fichier », « Photo » (`selected` = mode actif, couleurs `ActiveLabel` / `ActiveSurface`), `testTag` `add_mode_selector` (rangée), `add_mode_url`, `add_mode_file`, `add_mode_photo`, et `add_mode_content` (zone du contenu) ; l'emplacement `url` est **toujours composé**, et masqué hors mode URL par `Modifier.layout` à taille nulle sans capter touches, focus ni clavier ; `file` et `photo` ne sont composés que lorsqu'ils sont actifs (research D3, scénario 4)

**Checkpoint**: `AddTabScreen` compile et ne dépend ni de `ImportQueue`, ni de l'OCR, ni de la navigation.

---

## Phase 3: User Story 1 - Un seul onglet pour ajouter une recette (Priority: P1) 🎯 MVP

**Goal**: Barre du bas à 4 boutons ; « Ajouter » s'ouvre toujours sur l'URL avec un sélecteur URL / Fichier / Photo ; un lien de page partagé arrive sur le mode URL.

**Independent Test**: Toucher « Ajouter » : import par URL, sélecteur sur « URL » ; toucher « Photo » : import par photo ; importer une recette par URL : elle arrive comme aujourd'hui (quickstart §1 à §8).

### Tests for User Story 1

> Écrire le test d'abord ; il doit échouer tant que T004 n'existe pas.

- [X] T005 [P] [US1] Créer `IT/AddTabScreenTest.kt` (Compose UI, `createComposeRule`, contenus factices dans les emplacements) : au premier affichage le contenu URL est visible et `add_mode_url` est sélectionné (FR-003) ; toucher `add_mode_file` puis `add_mode_photo` affiche le contenu correspondant et masque les autres ; un compteur ou un champ saisi dans l'emplacement URL survit à un aller-retour URL, Fichier, URL (scénario 4) ; le contenu URL masqué n'est ni affiché ni cliquable ; `add_mode_selector` reste visible dans les trois modes ; aucun test existant modifié

### Implementation for User Story 1

- [X] T006 [US1] Dans `APP/MainActivity.kt`, remplacer dans `NAV_ITEMS` les trois entrées `url_import` / `file` / `photo` par `NavItem("ajouter", "Ajouter", R.drawable.ic_nav_add)` (ordre : Recettes, Ajouter, Recherche, Menu) et fixer `MAIN_ROUTES = setOf("list", "ajouter", "search", "menu")` (FR-001) ; `contentDescription` des icônes inchangée (= libellé)
- [X] T007 [US1] Dans `APP/MainActivity.kt`, déclarer `var addMode by rememberSaveable { mutableStateOf(AddMode.URL) }` dans `setContent` (au niveau de `navController`, avant `LaunchedEffect(sharedUrl)`) ; si `AddMode` n'est pas directement sauvegardable, utiliser un `Saver` ou son nom (`name` / `valueOf`) ; la rotation garde le mode (edge case)
- [X] T008 [US1] Dans `APP/MainActivity.kt`, remplacer les destinations `composable("url_import")`, `composable("file")` et `composable("photo")` par une seule `composable("ajouter")` : `Scaffold` avec `RecipeTopBar("Ajouter")`, contenant `AddTabScreen(addMode, { addMode = it }, url = { UrlImportScreen(...) }, file = { FileImportScreen(...) }, photo = { PhotoImportScreen(...) })` avec les callbacks et paramètres **identiques** à ceux d'aujourd'hui (`onImport`, `onBack`, `parseJson`, `onQueue`, `pending`, `onOpenQueue`, `sharedUrl`, `onSharedUrlConsumed`, `onFilePicked`, `onGalleryPicked`) ; remplacer `popUpTo("url_import") { inclusive = true }` par `popUpTo("ajouter") { inclusive = true }` ; ne pas ajouter de route ni d'entrée de pile pour le changement de mode (le retour arrière quitte l'onglet vers `list`)
- [X] T009 [US1] Dans `APP/MainActivity.kt`, remplacer `LaunchedEffect(sharedUrl)` : si `sharedUrl != null`, `addMode = AddMode.URL` puis `navController.navigateToTab("ajouter")` si `currentRoute != "ajouter"` (contrat `contracts/share.md`, FR-005) ; ne pas toucher `handleSharedText`, `handleIncomingIntent`, le dialogue d'image ni le manifeste ; sur Ajouter en mode Fichier ou Photo, la seule mise à URL suffit
- [X] T010 [US1] Dans `APP/MainActivity.kt`, remplacer le lambda de `RecipeBottomNav(currentRoute) { route -> ... }` : pour `ajouter`, toujours `addMode = AddMode.URL` puis `navigateToTab("ajouter")` seulement si on n'y est pas déjà ; pour les autres routes, garder `if (route != currentRoute) navController.navigateToTab(route)` (FR-002, y compris onglet déjà affiché) ; vérifier que le `restoreState` de `navigateToTab` ne ramène pas un ancien mode (le mode vient de `addMode`)
- [X] T011 [US1] Grep final sur `app/src` : plus aucune occurrence de `"url_import"`, `"file"`, `"photo"` comme **route** (le fichier de préférences `url_import` de `UrlImportScreenTest.kt` et `HermesPrefs` ne se renomme pas) ; `ic_nav_url`, `ic_nav_file`, `ic_nav_photo` toujours référencées (ne rien supprimer) ; `ImportQueueScreen`, la notification de la file et `onOpenQueue` ouvrent toujours `queue`

**Checkpoint**: l'app compile, la barre a 4 boutons, les trois modes fonctionnent, un lien partagé atterrit sur URL.

---

## Phase 4: Vérification finale

**Purpose**: Preuves que rien n'est cassé (SC-003, FR-004, FR-006).

- [X] T012 Lancer tous les tests JVM (`testDebugUnitTest`) : `SharedTextTest` et `IngredientParserTest` verts, sans modification
- [X] T013 Sauvegarder `url_import.xml` du téléphone (vérifier la taille), couper le volume média, `adb install -r` de l'app et de l'APK de test, lancer toute la suite par `am instrument` (dont `AddTabScreenTest`, `UrlImportScreenTest`, `TextImportScreenTest`, `ImportQueueScreenTest`, `MenuScreenTest`, `SearchScreenTest`, `HomeScreenTest`) ; restaurer `url_import.xml` et revérifier sa taille, même en cas d'échec
- [ ] T014 Vérification manuelle sur le téléphone (quickstart §1 à §10) : barre à 4 boutons ; « Ajouter » depuis Recettes, Recherche et Menu ; modes Fichier et Photo jusqu'à l'écran de relecture ; retour sur URL en retouchant « Ajouter » depuis Photo ; import URL conservé après URL, Fichier, URL ; retour arrière et rotation ; file d'attente ; **partage d'un reel depuis Instagram** (et d'un lien depuis Chrome) : l'écran Ajouter s'ouvre sur « URL », lien collé, import lancé, y compris depuis Ajouter en mode Photo et depuis Menu ; autres partages (texte sans lien, image, JSON) inchangés ; données identiques (FR-006)
- [ ] T015 Lancer `/verifie-tests` (aucun test affaibli, désactivé ou réaligné ; `SharedTextTest` non modifié) avant de déclarer terminé ou de committer

---

## Couverture (exigences vers tâches)

| Exigence | Tâches |
|----------|--------|
| FR-001 (4 boutons), scénario 1 | T006, T014 |
| FR-002 (URL à chaque toucher), scénario 2 | T007, T010, T014 |
| FR-003 (sélecteur, mode actif), scénario 3 | T004, T005, T008 |
| FR-004, SC-003 (aucune régression) | T008, T011, T012, T013 |
| Scénario 4 (import conservé) | T004, T005, T014 |
| FR-005, FR-005b, scénarios 5 et 6 | T001, T002 (fait), T009, T014 |
| FR-006 (aucune donnée touchée) | règles générales, T011, T014 |
| Edge cases (retour, rotation, file) | T007, T008, T011, T014 |
| SC-001, SC-002 | T006, T008, T014 |

## Dependencies & Execution Order

- Phase 1 : faite. Phase 2 : T003 et T004 en parallèle (fichiers différents). Phase 3 : T005 [P] dès que la signature de T004 est connue ; T006 à T010 modifient le même fichier `MainActivity.kt`, donc séquentiels (T006, T007, T008, puis T009 et T010, puis T011). Phase 4 : après la phase 3, dans l'ordre T012, T013, T014, T015.
- T008 dépend de T004 (`AddTabScreen`), T003 (icône utilisée en T006) et T007 (`addMode`).

## Parallel Opportunities

- T003 avec T004.
- T005 avec T006 à T011 (fichier de test distinct), une fois T004 fait.

## Implementation Strategy

MVP = toute la US1 (petit chantier, une seule story) : T003 et T004, test T005, puis T006 à T011 dans `MainActivity.kt`, puis la phase 4. Livrer en un seul passage ; T006 à T010 s'enchaînent avant la première compilation pour ne pas laisser `MainActivity.kt` dans un état qui ne compile pas.
