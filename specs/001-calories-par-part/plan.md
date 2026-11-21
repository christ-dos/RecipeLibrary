# Implementation Plan: Calories par part

**Branch**: `001-calories-par-part` (travail sur `main`, pas de branche créée) | **Date**: 2026-11-21 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-calories-par-part/spec.md`

## Summary

Afficher sur chaque recette une estimation « ≈ X kcal / part » (et protéines, glucides, lipides dans le détail), calculée hors ligne dans l'app : lecture des quantités par des règles fixes, appariement à une table Ciqual réduite (~300 aliments) embarquée en JSON, estimation du nombre de parts d'après le poids et la catégorie. Les quantités vagues sont complétées par Hermes (Llama local) via un nouveau point d'entrée `/estimate` : pendant l'extraction par lien, pendant l'import texte/fichier si le PC répond, sinon par le worker de la file d'attente existante, qui fait aussi le rattrapage unique des recettes existantes. Les poids corrigés à la main priment et sont stockés avec la recette (migration Room 5 → 6, ajout de colonnes seulement).

## Technical Context

**Language/Version**: Kotlin (JVM 17) pour l'app ; Python 3.12 pour Hermes et le script de génération de la table.

**Primary Dependencies**: Jetpack Compose (Material 1.5), Room 2.6.1, WorkManager, Gson (déjà présents) ; aucune nouvelle dépendance d'app. Script : `openpyxl` (outil de développement uniquement, dans le venv de Hermes).

**Storage**: Room (`recipes.db`), migration 5 → 6 ; asset en lecture seule `assets/nutrition/foods.json`.

**Testing**: JUnit (tests JVM `app/src/test`) pour toute la logique pure ; Compose UI tests et `room-testing` (`app/src/androidTest`) lancés sur le téléphone via `am instrument` ; pytest pour Hermes.

**Target Platform**: Android minSdk 24, targetSdk 34, testé sur Galaxy S25 (Android 17) ; Hermes sous Windows 11.

**Project Type**: Application mobile + service local sur le PC (Hermes).

**Performance Goals**: Estimation affichée en moins de 0,5 s à l'ouverture de la fiche (SC-004) ; chargement de la table une seule fois par lancement.

**Constraints**: Hors ligne pour le calcul ; aucune perte de données (migration testée) ; l'attente du PC ne bloque jamais un import ni un enregistrement ; jamais `connectedAndroidTest` (efface les recettes).

**Scale/Scope**: ~80 recettes existantes, ~10 ingrédients par recette, ~300 aliments de référence.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` n'est pas encore rempli (modèle vide). À défaut, les règles permanentes de l'utilisatrice servent de portes :

| Porte | Statut | Comment |
|---|---|---|
| Données jamais perdues | ✅ | Migration par `ADD COLUMN` nullables + test de migration 5 → 6 ; installation par `adb install -r` |
| Tests avant « terminé » (`/verifie-tests`) | ✅ | Prévu à la fin ; logique pure testée en JVM, écrans en Compose UI tests |
| Aucun test affaibli | ✅ | Seuls des ajouts ; le test de migration existant est étendu, pas modifié |
| Réglages protégés pendant les tests | ✅ | Sauvegarde vérifiée (non vide) de `url_import.xml` avant, restauration après (quickstart §3) |
| Rien de commité sans demande | ✅ | |

Re-vérification après Phase 1 : ✅, la conception n'introduit aucune violation.

## Project Structure

### Documentation (this feature)

```text
specs/001-calories-par-part/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── hermes-estimate.md
│   └── foods-json.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
app/src/main/java/com/example/recipelibrary/
├── data/
│   ├── Recipe.kt                    # + servings, servingsSource, ingredientWeights
│   ├── Converters.kt                # + Map<String, IngredientWeight> <-> JSON
│   ├── AppDatabase.kt / Migrations.kt / DatabaseProvider.kt   # version 6, MIGRATION_5_6
│   ├── HermesClient.kt              # + estimate(server, title, category, lines)
│   ├── ImportQueue.kt               # worker : liens puis WeightEstimationProcessor
│   └── nutrition/                   # NOUVEAU, Kotlin pur (sans Android sauf FoodTable.load)
│       ├── FoodTable.kt             # chargement foods.json, appariement (R3)
│       ├── IngredientParser.kt      # quantité, unité, aliment, grammes (R2)
│       ├── NutritionCalculator.kt   # NutritionEstimate, priorités, avertissements
│       ├── ServingsEstimator.kt     # parts par catégorie (FR-016/017)
│       └── WeightEstimationProcessor.kt  # rattrapage et fil de l'eau (R5)
├── ui/
│   ├── RecipeDetailScreen.kt        # bandeau kcal/part + accès au détail
│   ├── NutritionDetailSheet.kt      # NOUVEAU : détail, correction d'un poids, parts
│   ├── AddEditRecipeScreen.kt       # + champ « Nombre de parts »
│   ├── TextImportScreen.kt / FileImportScreen.kt  # estimation immédiate si PC joignable (R6)
│   └── ImportQueueScreen.kt         # + « Estimation des quantités : n / N »
├── RecipeJsonParser.kt              # + servings, ingredientWeights de /extract
app/src/main/assets/nutrition/foods.json   # généré
app/src/test/java/com/example/recipelibrary/nutrition/   # tests JVM + 20 recettes de référence
app/src/androidTest/java/com/example/recipelibrary/      # migration 5→6, écrans
tools/ciqual/                        # selection.csv, build_foods.py, README
C:\Users\Kikine\AppData\Local\hermes\recipe_extractor.py # /estimate, champs /extract
C:\Users\Kikine\AppData\Local\hermes\test_recipe_extractor.py
```

**Structure Decision**: Application Android à module unique (`app`), avec un nouveau paquet `data/nutrition` en Kotlin pur pour que tout le calcul soit testable en JVM ; Hermes reste un script unique hors dépôt, modifié avec sauvegarde `.bak` préalable comme jusqu'ici.

## Phasage proposé (pour `/speckit-tasks`)

1. **Socle** : table Ciqual (script + asset + test du fichier), migration 5 → 6 + test.
2. **P1 — calcul et affichage** : analyseur, appariement, calcul, parts estimées, bandeau sur la fiche, champ parts du formulaire (stories 1 et 5).
3. **P2 — détail et correction** (story 2).
4. **P3 — Hermes** : `/estimate`, champs de `/extract`, lecture côté app (story 3).
5. **P3 — rattrapage et fil de l'eau** : processeur dans le worker, estimation immédiate en import texte/fichier, progression (story 4).
6. **Validation** : 20 recettes de référence (SC-001), mesure sur les recettes réelles (SC-002/003), `/verifie-tests`.

## Complexity Tracking

Aucune violation à justifier.
