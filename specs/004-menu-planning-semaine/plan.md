# Implementation Plan: Menu en planning à la semaine

**Branch**: `004-menu-planning-semaine` | **Date**: 2026-11-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-menu-planning-semaine/spec.md`

## Summary

L'onglet Menu gagne une seconde vue « Semaine » (grille lundi à dimanche × petit-déjeuner, midi, soir) sur les mêmes entrées que la vue « Liste ». Chaque entrée de menu reçoit un **repas facultatif** (nouvelle colonne `meal` nullable). L'ajout d'une recette ne passe plus par la petite fenêtre `AddRecipeDialog` mais par la page Recherche existante en mode « choix » (nouvelle route `search_pick`), qui ajoute la recette (dans la case visée si besoin) puis revient au menu. « Retirer les jours » efface jour et repas de toutes les entrées. La liste de courses n'est pas touchée : `ShoppingListBuilder` ne lit ni `day` ni `meal`.

Stockage : migration Room **additive** (`ALTER TABLE menu_entries ADD COLUMN meal INTEGER`), aucune donnée existante modifiée ; la vue choisie est mémorisée dans une préférence locale (pas dans Room).

## Technical Context

**Language/Version**: Kotlin 1.9 (JVM 17).

**Primary Dependencies**: Jetpack Compose (Material 1.5), Navigation Compose 2.7, Room 2.6.1, Coil (déjà présents) ; aucune nouvelle dépendance.

**Storage**: Room `recipes.db`, `menu_entries.meal` (INTEGER nullable) ; préférence de vue dans `SharedPreferences` (fichier `menu_prefs`, clé `menu_view`).

**Testing**: JUnit JVM (`app/src/test`) pour la logique pure (`MenuLogic`) ; Compose UI tests, `MenuDaoTest` et `DatabaseMigrationTest` (`app/src/androidTest`) par `am instrument` sur le téléphone ; jamais `connectedAndroidTest`.

**Target Platform**: Android minSdk 24, targetSdk 34, Galaxy S25.

**Project Type**: Application mobile (module unique `app`).

**Performance Goals**: grille de 21 cases + « À placer » fluide avec 30 entrées ; regroupement en mémoire (`MenuLogic.groupByWeek`), pas de requête supplémentaire.

**Constraints**: hors ligne ; aucune écriture dans `recipes` ; liste de courses indépendante du jour et du repas (FR-006) ; données existantes jamais perdues (FR-008).

**Scale/Scope**: environ 100 recettes, menus de 5 à 30 entrées.

## Ordre des versions de base de données (décision prise)

Les numéros de version se prennent dans l'ordre de réalisation. Aujourd'hui `AppDatabase.kt` est en **version 7** (feature 002). Attribution fixée :

| Version | Feature | Migration |
|---|---|---|
| 8 | 001 (tâche T046) | prise en premier |
| 9 | 003 (Chronodrive) | prise ensuite |
| **10** | **004 (cette feature)** | **`MIGRATION_9_10`** : `ALTER TABLE menu_entries ADD COLUMN meal INTEGER` |

Au moment d'implémenter :

1. vérifier dans `AppDatabase.kt` que `version = 9` (sinon s'arrêter : les migrations 7 -> 8 et 8 -> 9 ne sont pas encore livrées, la 004 attend) ;
2. créer `MIGRATION_9_10`, l'ajouter à la liste de `DatabaseProvider.kt` et à **tous** les `addMigrations(...)` de `DatabaseMigrationTest.kt` ;
3. ne jamais réécrire ni renuméroter une migration déjà livrée (`MIGRATION_7_8`, `MIGRATION_8_9`).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` n'est pas rempli ; les règles permanentes de l'utilisatrice servent de portes.

| Porte | Statut | Commentaire |
|---|---|---|
| Données jamais perdues | OK | Migration par `ADD COLUMN` nullable seulement + test 9 -> 10 (entrées, parts, jours, recettes, coches, articles manuels identiques) ; `adb install -r` ; vérification sur une copie de la base |
| Liste de courses indépendante du placement | OK | Aucune modification de `ShoppingListBuilder` ; test JVM qui compare la liste avant/après placement |
| Tests avant « terminé » (`/verifie-tests`) | OK | Prévu en fin de phase |
| Aucun test affaibli | OK | Ajouts ; les cas de `MenuScreenTest` qui passent par la fenêtre `add_search` / `add_result_*` sont **réécrits** pour le mode choix : à signaler explicitement (comportement remplacé voulu par FR-007b, pas un affaiblissement) |
| Réglages protégés pendant les tests | OK | Sauvegarde et restauration de `url_import.xml` (quickstart) |
| Rien de commité sans demande | OK | |

Re-vérification après Phase 1 : OK.

## Project Structure

### Documentation (this feature)

```text
specs/004-menu-planning-semaine/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    ├── week-view-ui.md
    ├── search-pick-mode.md
    └── test-tags.md
```

### Source Code (repository root)

```text
app/src/main/java/com/example/recipelibrary/
├── data/
│   ├── AppDatabase.kt            # version 9 -> 10
│   ├── DatabaseProvider.kt       # + MIGRATION_9_10
│   ├── Migrations.kt             # + MIGRATION_9_10 (ADD COLUMN meal)
│   └── menu/
│       ├── MenuEntities.kt       # MenuEntry.meal, constantes MEAL_*
│       ├── MenuDao.kt            # setPlacement, clearPlacements, setDay(null) efface le repas
│       ├── MenuRepository.kt     # add(recipe, servings, day, meal), setPlacement, clearPlacements
│       ├── MenuLogic.kt          # mealName, groupByDay trié par repas, groupByWeek
│       └── MenuViewPref.kt       # (nouveau) lecture/écriture de la préférence de vue
├── ui/
│   ├── MenuScreen.kt             # sélecteur Liste/Semaine, WeekView, feuille d'entrée, « Retirer les jours »
│   ├── MenuViewModel.kt          # add(recipe, day, meal), setPlacement, clearPlacements
│   └── SearchScreen.kt           # paramètre facultatif `pick` (mode choix)
└── MainActivity.kt               # route search_pick, câblage MenuActions

app/src/test/java/com/example/recipelibrary/menu/
├── MenuLogicTest.kt              # + repas, groupByWeek, tri, placement
└── MenuPlacementShoppingTest.kt  # (nouveau) liste de courses identique avant/après placement

app/src/androidTest/java/com/example/recipelibrary/
├── DatabaseMigrationTest.kt      # + migration_9_to_10_keepsMenuEntries_mealNull
├── MenuDaoTest.kt                # + placement, retirer les jours
├── MenuScreenTest.kt             # adapté au mode choix ; + vue Semaine
└── SearchPickModeTest.kt         # (nouveau) mode choix de SearchScreen
```

**Structure Decision**: module unique ; la logique pure reste dans `MenuLogic` (testable en JVM), l'UI dans `MenuScreen.kt` (avec éventuellement `WeekView.kt` si le fichier devient trop long).

## Phases de réalisation

1. **Données** : colonne `meal`, migration, DAO, repository, `MenuLogic` (tests JVM, `MenuDaoTest` et test de migration écrits d'abord).
2. **Vue Liste** : repas à côté du jour, tri, choix jour + repas, « Retirer les jours ».
3. **Mode choix de Recherche** : route `search_pick`, bandeau, « Annuler », ajout puis retour ; remplacement de `AddRecipeDialog`.
4. **Vue Semaine** : sélecteur, grille, « À placer », feuille d'entrée (déplacer, parts, ouvrir, retirer), préférence de vue.
5. **Vérification** : `/verifie-tests`, essai sur le téléphone avec les données réelles (quickstart).

## Complexity Tracking

Aucune violation. Seul écart assumé : la fenêtre `AddRecipeDialog` disparaît ; `groupByCategory` reste tant que `GroupByCategoryTest` l'utilise (research.md, R6).
