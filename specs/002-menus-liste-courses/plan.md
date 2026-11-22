# Implementation Plan: Menus et liste de courses

**Branch**: `002-menus-liste-courses` | **Date**: 2026-11-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-menus-liste-courses/spec.md`

## Summary

Nouvel onglet « Menu » : l'utilisatrice y ajoute des recettes de sa bibliothèque (aussi depuis la fiche), avec un nombre de parts et un jour facultatif. La vue « Courses » calcule, hors ligne et à la volée, la liste des ingrédients du menu : quantités ajustées aux parts, aliments identiques regroupés grâce à l'analyseur et à la table Ciqual de la fonctionnalité 001, quantités additionnées par nature d'unité. Elle permet de cocher, d'ajouter des articles à la main, de partager la liste en texte et, pour chaque article, de copier son nom et d'ouvrir l'application Chronodrive (magasin et panier déjà en place) ; un lien de recherche direct a été testé et ne fonctionne pas dans l'application Chronodrive. Stockage : trois nouvelles tables, migration Room 6 → 7 par création de tables seulement.

## Technical Context

**Language/Version**: Kotlin (JVM 17) ; Python 3.12 pour le script de la table d'aliments (story 4 seulement).

**Primary Dependencies**: Jetpack Compose (Material 1.5), Navigation Compose 2.7, Room 2.6.1, Gson (déjà présents) ; aucune nouvelle dépendance.

**Storage**: Room `recipes.db`, version 7 (`menu_entries`, `shopping_checks`, `shopping_extras`) ; asset `assets/nutrition/foods.json` (+ champ facultatif `aisle`, story 4).

**Testing**: JUnit en JVM (`app/src/test`) pour toute la logique ; Compose UI tests et `room-testing` (`app/src/androidTest`) par `am instrument` sur le téléphone.

**Target Platform**: Android minSdk 24, targetSdk 34, Galaxy S25.

**Project Type**: Application mobile (module unique `app`).

**Performance Goals**: liste d'un menu de 30 recettes en moins d'1 s (SC-003) ; calcul en mémoire hors du fil principal.

**Constraints**: hors ligne ; aucune écriture dans `recipes` sauf la suppression demandée ; aucun identifiant Chronodrive (FR-015b) ; jamais `connectedAndroidTest`.

**Scale/Scope**: environ 100 recettes, menus de 5 à 30 entrées, 385 aliments de référence.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` n'est pas rempli ; les règles permanentes de l'utilisatrice servent de portes.

| Porte | Statut | Commentaire |
|---|---|---|
| Données jamais perdues | ✅ | Migration par `CREATE TABLE` seulement + test 6 → 7 vérifiant les recettes ; `adb install -r` ; vérification sur une copie de la base |
| Tests avant « terminé » (`/verifie-tests`) | ✅ | Prévu en fin de phase |
| Aucun test affaibli | ✅ | Ajouts ; le test de migration existant reçoit seulement `MIGRATION_6_7` dans sa liste (sera signalé) |
| Réglages protégés pendant les tests | ✅ | Sauvegarde et restauration de `url_import.xml` (quickstart §3) |
| Pas d'identifiants saisis pour l'utilisatrice | ✅ | Copie du nom et ouverture de l'application Chronodrive ; aucun compte, aucun panier manipulé |
| Rien de commité sans demande | ✅ | |

Re-vérification après Phase 1 : ✅.

## Project Structure

### Documentation (this feature)

```text
specs/002-menus-liste-courses/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── chronodrive-search.md
│   └── shopping-text.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
app/src/main/java/com/example/recipelibrary/
├── data/
│   ├── AppDatabase.kt / Migrations.kt / DatabaseProvider.kt   # version 7, MIGRATION_6_7
│   ├── RecipeRepository.kt          # + suppression de recette avec ses entrées (transaction)
│   ├── menu/                        # NOUVEAU
│   │   ├── MenuEntry.kt, ShoppingCheck.kt, ShoppingExtra.kt   # entités Room
│   │   ├── MenuDao.kt               # entrées, coches, ajouts à la main
│   │   └── MenuRepository.kt
│   ├── shopping/                    # NOUVEAU, Kotlin pur
│   │   ├── ShoppingListBuilder.kt   # regroupement, facteur de parts, clés d'articles
│   │   ├── QuantityFormat.kt        # g/kg, ml/cl/l, pièces arrondies
│   │   ├── ChronodriveSearch.kt     # terme copié, adresse de secours
│   │   └── ShoppingText.kt          # texte partagé
│   └── nutrition/
│       ├── IngredientParser.kt      # + amount (nature + valeur), défaut null
│       ├── NutritionCalculator.kt   # isHeader rendu partagé
│       └── Food.kt / FoodTable.kt   # + aisle facultatif (story 4)
├── ui/
│   ├── MenuScreen.kt                # NOUVEAU : vues Menu et Courses
│   ├── ShoppingListView.kt          # NOUVEAU
│   ├── MenuViewModel.kt             # NOUVEAU
│   ├── RecipeDetailScreen.kt        # + « Ajouter au menu », avertissement à la suppression
│   └── HomeScreen.kt                # avertissement à la suppression
├── MainActivity.kt                  # onglet « Menu », route « menu »
tools/ciqual/build_foods.py, selection.csv   # + rayon (story 4)
app/src/test/.../shopping/           # tests JVM, 10 menus de référence
app/src/androidTest/.../             # migration 6→7, MenuScreenTest, ShoppingListScreenTest
```

**Structure Decision**: module unique `app` ; la logique de la liste dans un paquet `data/shopping` en Kotlin pur, testable en JVM avec la vraie table d'aliments ; la persistance dans `data/menu`.

## Phasage proposé (pour `/speckit-tasks`)

1. **Socle** : entités, DAO, migration 6 → 7 + test ; champ `amount` de l'analyseur + tests.
2. **P1 — menu** (story 1) : onglet, ajout depuis la fiche et par recherche, parts, jour, retrait, suppression de recette avec avertissement.
3. **P1 — liste** (story 2) : `ShoppingListBuilder`, formats de quantité, origine des articles, vue Courses.
4. **P2 — commande** (story 3) : coches, ajouts à la main, partage en texte, bouton Chronodrive (copie + ouverture de l'application) et retour.
5. **P3 — rayons** (story 4) : `aisle` dans le script et `foods.json`, groupement à l'écran et dans le texte.
6. **Validation** : 10 menus de référence (SC-002), 30 recettes < 1 s (SC-003), migration sur une copie (SC-004), parcours à la main (SC-001, SC-006), `/verifie-tests`.

## Complexity Tracking

Aucune violation à justifier.
