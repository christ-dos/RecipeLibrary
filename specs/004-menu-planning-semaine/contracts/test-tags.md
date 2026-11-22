# Contrat : tags de test (`Modifier.testTag`)

Tags existants conservés (feature 002) : `menu_tab`, `courses_tab`, `add_recipe`, `clear_menu`, `clear_confirm`, `menu_empty`, `menu_list`, `group_<jour|0>`, `entry_<id>`, `remove_<id>`, `servings_<id>_(minus|value|plus)`, `times_<id>_(minus|value|plus)`, `day_<id>`, `day_option_<1..7|0>`, `set_recipe_servings_<id>`, `number_field`, `number_ok`, et ceux de la liste de courses.

Retirés avec la fenêtre d'ajout : `add_search`, `add_list`, `add_cat_<cat>`, `add_result_<id>`.

## Nouveaux tags

| Tag | Élément |
|---|---|
| `view_list`, `view_week` | Segments du sélecteur de vue |
| `clear_days`, `clear_days_confirm` | Bouton « Retirer les jours » et sa confirmation |
| `week_grid` | Conteneur de la grille Semaine |
| `week_day_<1..7>` | Ligne d'un jour |
| `week_cell_<jour>_<repas>` | Case (jour 1..7, repas 1..3) |
| `week_add_<jour>_<repas>` | « + » d'une case (vide ou occupée) |
| `week_entry_<entryId>` | Titre d'une entrée dans une case |
| `week_more_<jour>_<repas>` | « +N » d'une case de plus de 2 entrées |
| `week_nomeal_<jour>` | Ligne « Sans repas » d'un jour |
| `week_unplaced` | Section « À placer » |
| `week_empty` | Message de semaine vide |
| `entry_sheet` | Feuille d'entrée |
| `sheet_day_<1..7|0>`, `sheet_meal_<1..3|0>` | Choix de jour et de repas (0 = « Sans jour » / « Sans repas ») |
| `sheet_apply` | Valider le déplacement |
| `sheet_open`, `sheet_remove` | « Ouvrir la recette », « Retirer du menu » |
| `meal_option_<1..3|0>` | Options de repas du sélecteur de la vue Liste |
| `pick_banner`, `pick_cancel` | Bandeau et « Annuler » du mode choix |
| `pick_in_menu_<id>` | Pastille « déjà au menu » |
| `search_result_<id>` | Ligne de résultat de `SearchScreen` (ajouté, utile en mode choix et en usage normal) |

## Tests à écrire (résumé)

- JVM : `MenuLogicTest` (repas, tri, `groupByWeek`, `normalizePlacement`, `dayMealLabel`), `MenuPlacementShoppingTest`.
- Instrumentés : `DatabaseMigrationTest` (migration 9 -> 10), `MenuDaoTest` (placement, retirer les jours), `MenuScreenTest` (bascule Liste/Semaine, scénario d'exemple de l'US1, case vide -> `onPickRecipe(jour, repas)`, déplacer, « Sans jour », retirer, « Retirer les jours »), `SearchPickModeTest` (bandeau, clic = `onRecipeClick` sans ouvrir de fiche, « Annuler », « déjà au menu », mode normal inchangé).
