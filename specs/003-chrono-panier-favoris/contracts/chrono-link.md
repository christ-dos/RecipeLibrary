# Contract: bouton « Chrono » avec produit retenu

**Sens** : l'app copie le nom de l'article et ouvre la fiche du produit retenu dans l'application Chronodrive. Aucune donnée personnelle envoyée, aucune action sur le panier ni sur le compte. Prolonge [002 / chronodrive-search.md](../../002-menus-liste-courses/contracts/chronodrive-search.md).

## Résolution (appui sur « Chrono »)

Entrée : clé de l'article (research R4) et terme de recherche (`ChronodriveSearch.term` / `termForExtra`).

| Situation | Action |
|---|---|
| Un choix existe pour la clé | ouvrir la fiche de ce produit |
| Pas de choix, 1 favori correspondant (FR-002) | enregistrer le choix, ouvrir la fiche |
| Pas de choix, plusieurs correspondants | boîte de choix ; produit touché : enregistrer le choix, ouvrir la fiche ; « Aucun de ceux-là » : comportement actuel, rien d'enregistré |
| Pas de choix, aucun correspondant, ou aucun favori importé | comportement actuel de la feature 002 |

Jamais de choix automatique entre plusieurs (FR-004).

## Ouverture de la fiche (FR-005)

1. Copier le nom de l'article (libellé « Chronodrive » ; FR-006).
2. `Intent(ACTION_VIEW, "chronodrive://product/<product_id>")` + `setPackage("com.ikomobi.chronodrive")` + `FLAG_ACTIVITY_NEW_TASK` (vérifié le 2026-11-22 : citron n° 122974).
3. Si `ActivityNotFoundException` (application absente, lien refusé) : `Intent(ACTION_VIEW, "https://www.chronodrive.com/product/<product_id>")` dans le navigateur.
4. Si rien ne s'ouvre : comportement actuel (favoris `chronodrive://favorites`, puis accueil, puis recherche du site), puis message « Impossible d'ouvrir Chronodrive ».
5. Mémoriser la clé de l'article pour la mise en évidence au retour (comportement 002).

Message affiché : « Fiche de « <nom du produit> » : appuie sur + ». Il ne doit jamais laisser croire que quelque chose a été ajouté au panier.

## Liens (référence)

| Usage | Lien | Statut |
|---|---|---|
| Fiche, application | `chronodrive://product/<id>` | vérifié sur le téléphone |
| Fiche, navigateur | `https://www.chronodrive.com/product/<id>` | vérifié |
| Favoris | `chronodrive://favorites` | vérifié, déjà utilisé |
| Adresse complète du site | `/<marque>--<slug>-P<id>` | non utilisée (le numéro suffit) |

## Appui long sur un article (US3)

Entrée « Choisir mon produit Chronodrive » : liste de tous les favoris importés, par rayon, avec recherche par texte ; le produit touché remplace le choix de l'article ; l'application Chronodrive n'est pas ouverte.

## Tests

- JVM : `ChronodriveLinksTest` (URI de l'application, URL du site, numéro non numérique refusé).
- Compose : `ShoppingListScreenTest` : « Chrono » avec produit retenu déclenche l'action avec le numéro ; plusieurs candidats : la boîte s'affiche ; aucun : comportement 002.
- Téléphone (quickstart §5) : ouverture réelle de la fiche dans l'application Chronodrive.
