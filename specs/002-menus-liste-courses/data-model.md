# Data Model: Menus et liste de courses

Base Room `recipes.db`, version 6 → 7. Migration `MIGRATION_6_7` : trois `CREATE TABLE`, aucune table existante modifiée. Les recettes ne sont jamais écrites par cette fonctionnalité, sauf leur suppression demandée par l'utilisatrice et la saisie volontaire de leurs parts (« Indiquer les parts de la recette »).

## MenuEntry — table `menu_entries`

| Champ | Colonne | Type | Règles |
|---|---|---|---|
| id | `id` | TEXT, clé primaire | UUID |
| recipeId | `recipe_id` | TEXT NOT NULL | identifiant d'une recette existante ; index `index_menu_entries_recipe_id` |
| servings | `servings` | INTEGER, nullable | 1 à 100 ; null = recette sans parts ni estimation, réglée par `times` |
| times | `times` | INTEGER NOT NULL DEFAULT 1 | 1 à 10 ; nombre de fois la recette, utilisé seulement quand `servings` est null |
| day | `day` | INTEGER, nullable | 1 = lundi … 7 = dimanche ; null = « Sans jour » |
| position | `position` | INTEGER NOT NULL | ordre dans le menu (ajout en fin) |
| createdAt | `created_at` | INTEGER NOT NULL | horodatage ms |

- Facteur appliqué aux quantités : `servings ÷ parts de la recette` si les deux sont connus, sinon `times`.
- « Indiquer les parts de la recette » écrit `servings` et `servings_source = USER` dans la recette (seule écriture dans `recipes` permise par le menu, faite à la demande de l'utilisatrice), puis met `servings` de l'entrée à cette valeur.
- Plusieurs entrées peuvent viser la même recette (FR, scénario 1.4).
- À l'ajout : `servings` = parts de la recette (indiquées), sinon parts estimées par `ServingsEstimator`, sinon null.
- Recette supprimée → ses entrées supprimées dans la même transaction (R7).
- Entrée dont la recette est introuvable (cas anormal) : ignorée par le calcul de la liste et affichée « Recette supprimée » avec bouton Retirer.

## ShoppingCheck — table `shopping_checks`

| Champ | Colonne | Type | Règles |
|---|---|---|---|
| itemKey | `item_key` | TEXT, clé primaire | clé d'article : `food:<code Ciqual>` ou `txt:<texte normalisé>` |
| checkedAt | `checked_at` | INTEGER NOT NULL | horodatage ms |

- Une ligne = un article calculé coché. Décocher = supprimer la ligne.
- L'état suit la clé, donc survit au recalcul de la liste (FR-014). Les lignes dont la clé n'existe plus dans la liste restent sans effet ; elles sont supprimées quand le menu est vidé.

## ShoppingExtra — table `shopping_extras`

| Champ | Colonne | Type | Règles |
|---|---|---|---|
| id | `id` | TEXT, clé primaire | UUID |
| text | `text` | TEXT NOT NULL | 1 à 200 caractères, espaces en bord retirés ; la quantité éventuelle fait partie du texte (« 2 papier toilette ») |
| checked | `checked` | INTEGER NOT NULL DEFAULT 0 | 0 ou 1 |
| position | `position` | INTEGER NOT NULL | ordre d'ajout |
| createdAt | `created_at` | INTEGER NOT NULL | horodatage ms |

- Indépendant du menu : conservé quand le menu change ou est vidé (FR-013), supprimé seulement à la main.

## Objets calculés (jamais stockés)

### Amount et looseQuantity (ajouts à `ParsedIngredient`, défaut null)
`kind` ∈ {MASS, VOLUME, PIECE, SACHET}, `value` en g, ml, pièces ou sachets. Null pour une quantité vague ou absente. Une boîte ou un sachet avec un poids écrit (« 1 boîte de 400 g ») donne une masse.

`looseQuantity: LooseQuantity?` (`count`, `unit` sans accent) : pour une quantité vague, quantité et unité telles qu'écrites (« 1 boîte », « 1 botte », « 2 brins ») ; null sans quantité (« du sucre ») ou si `amount` est rempli.

### ShoppingItem
| Champ | Contenu |
|---|---|
| key | clé d'article (voir ShoppingCheck) |
| label | nom affiché : nom courant de l'aliment (premier alias), sinon texte de la ligne sans quantité |
| totals | somme par nature : masse, volume, pièces, sachets (chacune nullable) |
| loose | `looseQuantity` des lignes (« 1 boîte », « 1 botte »), additionnées si identiques (« 1 boîte » + « 1 boîte » → « 2 boîtes ») ; les lignes sans quantité n'ajoutent rien mais l'article existe (FR-010) |
| seasoning | vrai pour sel, poivre, épices : affiché sans quantité |
| aisle | rayon (story 4) ; « Autres » si inconnu |
| sources | recettes du menu qui l'utilisent (titre, nombre d'entrées) |
| searchTerm | terme de recherche Chronodrive (R4) |
| checked | vrai si une ligne `shopping_checks` porte sa clé |

Ordre d'affichage : non cochés puis cochés ; dans chaque groupe, par rayon (story 4) puis par libellé.

## Food (table d'aliments, fonctionnalité 001)

Ajout d'un champ facultatif `aisle: String?` dans `foods.json` (story 4, R5). Absent = « Autres ». Aucune autre modification du format.
