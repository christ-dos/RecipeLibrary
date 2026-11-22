# Contrat UI : page Recherche en mode « choix »

## Route

```text
search_pick?day={day}&meal={meal}      # day, meal : Int, défaut -1 (= absent)
```

Ouverte par `onPickRecipe(day, meal)` (bouton « Ajouter une recette » : sans arguments ; case de la grille : avec jour et repas). Hors `MAIN_ROUTES` : pas de barre du bas. La route normale `search` (barre du bas) est inchangée.

Libellé du bandeau :
- sans case : « Choisis une recette pour le menu » ;
- avec jour et repas : « Choisis une recette pour Mercredi · Soir » ;
- avec jour seul : « Choisis une recette pour Mercredi ».

## Composable

```kotlin
data class SearchPick(val label: String, val alreadyInMenu: Set<String>)   // ids de recettes déjà au menu

@Composable
fun SearchScreen(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onBack: () -> Unit,
    pick: SearchPick? = null,          // nouveau ; null = comportement actuel
    onCancel: () -> Unit = {}          // nouveau, utilisé seulement si pick != null
)
```

## Comportement

| Situation | Résultat |
|---|---|
| `pick == null` | Identique à aujourd'hui (aucun bandeau, un clic ouvre la fiche) |
| `pick != null` | Bandeau `pick_banner` (libellé) + bouton « Annuler » `pick_cancel` au-dessus de la barre de recherche ; recherche, suggestions, filtres rapides, catégories, recherches récentes et photos inchangés |
| Clic sur une recette (résultat) | `onRecipeClick(recipe)` ; la fiche ne s'ouvre pas ; dans la route : `menuVm.add(recipe, day, meal)` puis `popBackStack()` |
| « Annuler » ou Retour système | `popBackStack()`, rien n'est ajouté |
| Recette déjà au menu | pastille « déjà au menu » (`pick_in_menu_<id>`) sur la ligne ; clic possible : seconde entrée ajoutée |
| `day`/`meal` invalides (hors 1..7 / 1..3) | ignorés (ajout sans placement) |

## Ajout

`MenuViewModel.add(recipe, day, meal)` : parts par défaut comme en 002 (`MenuLogic.defaultServings`), `day`/`meal` passés par `MenuLogic.normalizePlacement`, position en fin de menu, liste de courses recalculée par le flux existant.

## Impact sur les tests existants

Les cas de `MenuScreenTest` qui ouvraient la fenêtre d'ajout sont adaptés (voir research.md R6). Aucun test de `SearchScreen` existant n'est modifié (paramètres ajoutés avec valeur par défaut).
