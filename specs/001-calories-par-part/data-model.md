# Data Model: Calories par part

**Feature**: [spec.md](./spec.md) · **Research**: [research.md](./research.md)

## Recipe (table `recipes`, existante — version 6)

Colonnes ajoutées par la migration 5 → 6 (aucune donnée existante modifiée ; `servings` et `servings_source` valent `NULL` pour les recettes existantes, `ingredient_weights` vaut `{}`) :

| Colonne | Type | Règles |
|---|---|---|
| `servings` | INTEGER | 1 à 100 ; `NULL` = inconnu (FR-001) |
| `servings_source` | TEXT | `USER` (saisi ou corrigé) ou `SOURCE` (donné par l'import) ; `NULL` si `servings` est `NULL` (FR-018) |
| `ingredient_weights` | TEXT NOT NULL DEFAULT '{}' (JSON) | Objet `{ "<texte exact de la ligne>": { "g": <nombre ou null>, "src": "PC" \| "USER" } }` ; `{}` = aucune entrée. NOT NULL parce que le champ Kotlin n'est pas nullable (Room l'exige) ; une valeur `NULL` ou illisible est lue comme une carte vide |

Règles :
- La clé est le texte exact d'une ligne de `ingredients`. Une entrée dont la clé ne correspond plus à aucune ligne est ignorée et supprimée au prochain enregistrement (FR-021, scénario 4 de la story 2).
- `src = USER` : poids saisi à la main, prioritaire sur tout (FR-008). Jamais remplacé par une réponse du PC.
- `src = PC` : poids estimé par Hermes ; `g = null` signifie « le PC n'a pas su », et sert de marqueur « déjà envoyé » (R5).
- Modifier le nombre de parts depuis la fiche ou le formulaire met `servings_source = USER`.

Champs Kotlin correspondants dans `data class Recipe` : `servings: Int? = null`, `servingsSource: String? = null`, `ingredientWeights: Map<String, IngredientWeight> = emptyMap()` (converti en JSON par `Converters`).

## IngredientWeight (valeur, dans `ingredient_weights`)

| Champ | Type | Règles |
|---|---|---|
| `g` | Double? | > 0 et ≤ 10 000 ; `null` autorisé seulement pour `src = PC` |
| `src` | `PC` \| `USER` | |

## Food (aliment de référence, asset en lecture seule)

Fichier `app/src/main/assets/nutrition/foods.json`, voir [contracts/foods-json.md](./contracts/foods-json.md).

| Champ | Type | Règles |
|---|---|---|
| `code` | String | Code Ciqual |
| `name` | String | Nom affiché |
| `aliases` | List<String> | Variantes normalisées (minuscules, sans accents), au moins une |
| `kcal`, `protein`, `carbs`, `fat` | Double | Pour 100 g, ≥ 0 |
| `pieceG` | Double? | Poids moyen d'une pièce (œuf 55, oignon 100…) |
| `density` | Double? | g/ml (huile 0,92, farine 0,55, miel 1,4…) ; défaut 1,0 |
| `kind` | `NORMAL` \| `SEASONING` \| `FRYING` | `SEASONING` = sel, poivre, épices : ignoré sans avertissement ; `FRYING` = huile de friture : compté mais signalé approximatif |

## ParsedIngredient (calculé, non stocké)

`line`, `quantity: Double?`, `unit: Unit?`, `foodText`, `food: Food?`, `grams: Double?`, `gramsOrigin: WRITTEN | PC | USER | null`, `issue: VAGUE | UNKNOWN_FOOD | NO_PIECE_WEIGHT | TO_COMPLETE | null`.

## NutritionEstimate (calculé à l'affichage, non stocké)

| Champ | Description |
|---|---|
| `counted` | Lignes comptées : grammes, origine, kcal, protéines, glucides, lipides |
| `ignored` | Lignes non comptées avec leur raison (`issue`) |
| `total` | kcal, protéines, glucides, lipides de la recette |
| `servings` / `servingsOrigin` | Valeur retenue et origine : `USER`, `SOURCE`, `ESTIMATED` ou `null` |
| `perServing` | `total / servings`, kcal arrondis à la dizaine, nutriments au gramme ; `null` sans parts |
| `warnings` | `PARTIAL` (> 50 % des lignes non comptées), `CHECK_VALUE` (> 3 000 kcal/part ou > 30 parts estimées), `FRYING` |

## Transitions (rattrapage)

```
ligne vague sans entrée ──(PC joignable, worker)──► entrée {src: PC, g: x|null}
entrée PC ──(utilisatrice corrige)──► entrée {src: USER, g: y}
texte de la ligne modifié ──► ancienne entrée orpheline (supprimée) ; nouvelle ligne vague sans entrée
```
