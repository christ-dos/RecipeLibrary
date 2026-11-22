# Contrat UI : onglet Menu, vues Liste et Semaine

## Signature (MenuScreen)

```kotlin
MenuScreen(
    entries, recipesById, allRecipes, shopping, actions, modifier, extras, shoppingActions,
    viewMode: MenuViewMode = MenuViewMode.LIST,          // nouveau
    onViewMode: (MenuViewMode) -> Unit = {}              // nouveau, écrit la préférence
)
enum class MenuViewMode { LIST, WEEK }

MenuActions(
    onPickRecipe: (day: Int?, meal: Int?) -> Unit,       // remplace onAdd : ouvre la route de choix
    onPlace: (entryId: String, day: Int?, meal: Int?) -> Unit,   // nouveau (déplacer)
    onClearDays: () -> Unit,                              // nouveau
    // inchangés : onServings, onTimes, onSetRecipeServings, onRemove, onClear, onOpenRecipe
    // onDay conservé pour la vue Liste (setDay : garde le repas)
)
```

## En-tête de l'onglet « Menu » (sous le TabRow Menu / Courses)

- Sélecteur segmenté « Liste | Semaine » (`view_list`, `view_week`) ; sélection = `viewMode`.
- Bouton « Ajouter une recette » (`add_recipe`) : `onPickRecipe(null, null)`.
- Menu « ... » ou boutons texte : « Retirer les jours » (`clear_days`, visible si au moins une entrée a un jour) puis « Vider le menu » (`clear_menu`, inchangé).
- « Retirer les jours » : `AlertDialog` « Retirer les jours ? » / « Les recettes restent au menu, sans jour ni repas. La liste de courses ne change pas. » ; confirmation `clear_days_confirm` -> `onClearDays()`.
- Onglet Courses : inchangé.

## Vue Liste

- Groupes et cartes comme en 002. Titre de groupe : `MenuLogic.dayName(day)`.
- Chaque carte indique le repas à côté du jour : bouton `day_<id>` affiche « Lundi · Midi » (ou « Lundi » sans repas, « Sans jour »).
- Dans un jour : ordre petit-déjeuner, midi, soir, sans repas.
- Le bouton `day_<id>` ouvre le sélecteur de placement (voir plus bas).

## Sélecteur de placement (Liste et feuille de la Semaine)

Un `DropdownMenu` en deux temps ou un `AlertDialog` avec deux rangées : jour (Lundi à Dimanche, « Sans jour ») et repas (Petit-déjeuner, Midi, Soir, « Sans repas »). « Sans jour » désactive et efface le repas. Valider = `onPlace(id, day, meal)`.

## Vue Semaine

Disposition (téléphone, sans défilement horizontal) :

```text
        | Petit-déj | Midi     | Soir
--------+----------+----------+----------
Lundi   | [ + ]    | Quiche   | Soupe
Mardi   | [ + ]    | [ + ]    | [ + ]
 ... (7 lignes)
Dimanche| ...
--------+----------+----------+----------
À placer : Salade  (cartes comme la vue Liste, avec « Placer »)
```

- Ligne de jour : nom court (« Lun. ») + 3 cases de même largeur.
- Case vide : zone cliquable avec « + » (`week_add_<jour>_<repas>`) -> `onPickRecipe(jour, repas)`.
- Case occupée : jusqu'à 2 titres (max 2 lignes chacun, ellipsis) puis « +N » si plus de 2 ; un petit « + » dans la case permet d'ajouter une autre recette (US2 sc. 2). Toucher un titre ouvre la feuille d'entrée. Toucher « +N » ouvre la liste de la case (`AlertDialog` des entrées de la case, chacune ouvre la feuille).
- « Sans repas » : sous la ligne du jour, une ligne secondaire « Sans repas : titre, titre » (`week_nomeal_<jour>`), affichée seulement si le jour a de telles entrées.
- « À placer » : titre de section (`week_unplaced`) et cartes (titre + bouton de placement), affiché seulement s'il y a des entrées sans jour.
- Semaine vide : grille vide + message « Touche une case pour y ajouter une recette » (`week_empty`).
- Toutes les entrées du menu sont visibles quelque part (invariant testé).
- Entrée dont la recette est supprimée : titre « Recette supprimée », feuille avec « Retirer du menu » seulement.

## Feuille d'entrée (`AlertDialog`, `entry_sheet`)

Titre complet de la recette ; stepper de parts ou de fois (mêmes tags que la vue Liste : `servings_<id>_*`, `times_<id>_*`) ; sélecteur jour + repas ; boutons « Ouvrir la recette » (`sheet_open`) -> `onOpenRecipe`, « Retirer du menu » (`sheet_remove`) -> `onRemove`, « Fermer ». Changer de case n'altère pas les parts.

## Persistance et état

- `viewMode` vient de la préférence (`menu_prefs` / `menu_view`) ; `onViewMode` l'écrit.
- Entrée ouverte dans la feuille : `rememberSaveable` (id).

## Garanties

- Les deux vues lisent la même liste `entries` ; aucune écriture de recette ; aucun accès à la liste de courses.
- Placer, déplacer, retirer les jours ne modifient ni `shopping` ni les coches.
