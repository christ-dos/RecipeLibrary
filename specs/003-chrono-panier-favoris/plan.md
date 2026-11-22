# Implementation Plan: Mes produits Chronodrive (US1 à US3)

**Branch**: `003-chrono-panier-favoris` | **Date**: 2026-11-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-chrono-panier-favoris/spec.md`

**Périmètre de ce plan** : User Stories **US1** (import des favoris), **US2** (« Chrono » ouvre la fiche du produit) et **US3** (écran « Mes produits Chronodrive »). **US4** (connexion au compte, bouton « Panier », « Mettre à jour mes favoris » depuis le compte) est une phase ultérieure, **hors périmètre** ici : sa faisabilité (API Chronodrive d'ajout au panier et de connexion, non documentée) doit d'abord être étudiée (voir research.md, R8) ; si elle n'est pas faisable proprement, US4 est abandonnée et US2 reste la solution.

## Summary

Un fichier de favoris Chronodrive (relevé du 2026-11-22 : 120 produits, numéro + nom, par rayon) est importé dans l'app et stocké dans une nouvelle table. Pour chaque article de la liste de courses, l'app cherche dans ces favoris les produits dont le nom contient tous les mots significatifs du terme de recherche (mots entiers, sans accents ni majuscules, pluriel simple, réutilisation de `Normalize`). Un seul candidat : retenu ; plusieurs : l'utilisatrice choisit une fois pour toutes (jamais de choix au hasard) ; aucun : comportement actuel. Le choix est mémorisé par clé d'article. « Chrono » ouvre alors directement `chronodrive://product/<numéro>` dans l'application Chronodrive, sinon `https://www.chronodrive.com/product/<numéro>`, sinon le comportement actuel ; le nom reste copié. Un écran « Mes produits Chronodrive » permet de voir, changer, oublier les choix et d'en attribuer un à la main. Stockage : deux nouvelles tables, migration Room 8 → 9 par création de tables seulement, avec test de migration.

## Technical Context

**Language/Version**: Kotlin 1.9 (JVM 17).

**Primary Dependencies**: Jetpack Compose (Material 1.5), Navigation Compose 2.7, Room 2.6.1 ; aucune nouvelle dépendance (le fichier est du texte, pas de Gson requis).

**Storage**: Room `recipes.db`, version 8 → **9** (`chronodrive_products`, `product_choices`) ; aucune table existante touchée.

**Testing**: JUnit en JVM (`app/src/test`) pour l'analyse du fichier, la correspondance et les liens ; Compose UI tests et tests Room (`app/src/androidTest`) par `am instrument` sur le téléphone, jamais `connectedAndroidTest`.

**Target Platform**: Android minSdk 24, targetSdk 34, Galaxy S25 ; application Chronodrive `com.ikomobi.chronodrive` déjà déclarée dans `<queries>` du manifeste.

**Project Type**: Application mobile (module unique `app`).

**Performance Goals**: correspondance d'un article sur ~125 produits en mémoire (négligeable) ; ouverture de la fiche en moins de 3 s (SC-001), dominée par l'application Chronodrive.

**Constraints**: hors ligne (rien n'est envoyé à Chronodrive) ; aucune lecture d'écran ni service d'accessibilité ; aucun identifiant de compte ; aucune action sur le panier ; données existantes intactes (FR-011, SC-005).

**Scale/Scope**: environ 125 produits, quelques dizaines de choix.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` est resté le modèle vide ; les règles permanentes de l'utilisatrice (CLAUDE.md et mémoire du projet) servent de portes, comme pour la feature 002.

| Porte | Statut | Commentaire |
|---|---|---|
| Données jamais perdues | ✅ | Migration 8 → 9 par `CREATE TABLE` seulement ; test de migration (recettes, menu, coches, ajouts à la main identiques) ; `adb install -r` ; vérification sur une copie de la base ; réimport sans suppression |
| Tests avant « terminé » (`/verifie-tests`) | ✅ | Prévu en fin de phase |
| Aucun test affaibli | ✅ | Ajouts ; `DatabaseMigrationTest` reçoit seulement `MIGRATION_8_9` dans sa liste (sera signalé) |
| Réglages protégés pendant les tests | ✅ | Sauvegarde et restauration de `url_import.xml` (quickstart §3) |
| Pas d'identifiants saisis pour l'utilisatrice | ✅ | Aucun compte, aucune connexion dans ce périmètre |
| Pas de service d'accessibilité | ✅ | Refusé par l'utilisatrice ; seuls des liens et un fichier sont utilisés |
| Trancher sans tourner | ✅ | Les choix ci-dessous sont décidés ; seules les questions ouvertes de research.md restent à elle |
| Rien de commité sans demande | ✅ | |

Re-vérification après Phase 1 : ✅ (aucune violation, aucune complexité à justifier).

## Project Structure

### Documentation (this feature)

```text
specs/003-chrono-panier-favoris/
├── spec.md
├── favoris-releve-2026-11-22.txt    # premier import et jeu de test
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── favorites-file.md            # format du fichier de favoris
│   └── chrono-link.md               # comportement du bouton « Chrono »
└── tasks.md                         # /speckit-tasks
```

### Source Code (repository root)

```text
app/src/main/java/com/example/recipelibrary/
├── data/
│   ├── AppDatabase.kt / Migrations.kt / DatabaseProvider.kt  # version 9, MIGRATION_8_9
│   ├── chronodrive/                      # NOUVEAU
│   │   ├── ChronodriveProduct.kt, ProductChoice.kt   # entités Room
│   │   ├── ChronodriveDao.kt             # produits, choix
│   │   ├── ChronodriveRepository.kt      # import (fusion), choix, correspondances
│   │   ├── FavoritesFileParser.kt        # Kotlin pur : texte -> produits (contracts/favorites-file.md)
│   │   └── ProductMatcher.kt             # Kotlin pur : FR-002
│   └── shopping/
│       └── ChronodriveSearch.kt          # + productAppUri(id), productWebUrl(id), clé de choix
├── ui/
│   ├── ChronodriveLauncher.kt            # + ouverture directe de la fiche (contracts/chrono-link.md)
│   ├── ChronodriveProductsScreen.kt      # NOUVEAU : écran « Mes produits Chronodrive »
│   ├── ProductPickerDialog.kt            # NOUVEAU : choix parmi plusieurs candidats
│   ├── MenuScreen.kt                     # « Chrono » consulte les choix ; appui long « Choisir mon produit Chronodrive »
│   └── MenuViewModel.kt                  # + choix de produit (ou ChronodriveViewModel dédié)
└── MainActivity.kt                       # + destination de navigation vers l'écran

app/src/test/java/com/example/recipelibrary/chronodrive/
└── FavoritesFileParserTest.kt, ProductMatcherTest.kt, ChronodriveLinksTest.kt

app/src/androidTest/java/com/example/recipelibrary/
├── DatabaseMigrationTest.kt (+ MIGRATION_8_9), ChronodriveDaoTest.kt
└── ChronodriveProductsScreenTest.kt, ShoppingListScreenTest.kt (+ cas « Chrono » avec produit)
```

**Structure Decision**: même découpage que la feature 002 : logique pure Kotlin testable en JVM (`FavoritesFileParser`, `ProductMatcher`), persistance Room dans un sous-paquet dédié, UI Compose. Aucun module nouveau.

## Phases

1. **Fondations** : entités, DAO, `MIGRATION_8_9` + son test, parseur de fichier, correspondance (tests JVM d'abord, sur les 120 favoris réels).
2. **US1** : import par sélecteur de fichier (même mécanisme que `FileImportScreen`), fusion, messages (« 120 produits importés », « 3 ajoutés, 120 déjà connus », fichier invalide). Dans cette phase le bouton s'appelle « Importer mes favoris » ; le scénario 4 de US1 (relecture depuis le compte) et la proposition de connexion du scénario 5 relèvent de US4.
3. **US2** : « Chrono » : résolution article → produit (retenu / un seul / plusieurs / aucun), ouverture de la fiche et replis, nom toujours copié.
4. **US3** : écran « Mes produits Chronodrive » (choix par article, favoris par rayon, changer / oublier), appui long sur un article.
5. **Vérification** : `/verifie-tests`, test sur copie de base, parcours à la main (quickstart).

## Hors périmètre (phase ultérieure)

**US4** (connexion à Chronodrive, bouton « Panier », « Mettre à jour mes favoris » depuis le compte) n'est pas planifiée ici. Avant de la planifier, une étude de faisabilité doit établir : existence d'une API stable d'ajout au panier et de lecture des favoris, mode de connexion sans conserver le mot de passe, durée de vie du jeton, conformité aux conditions d'utilisation (research.md R8). Les données de cette phase sont conçues pour ne rien bloquer : le bouton « Importer mes favoris » (fichier) pourra devenir « Mettre à jour mes favoris » sans changer le schéma, et la table des choix sert telle quelle au bouton « Panier ».

## Complexity Tracking

Aucune violation à justifier.
