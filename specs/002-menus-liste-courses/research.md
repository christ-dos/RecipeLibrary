# Research: Menus et liste de courses

## R1 — Où ranger le menu et la liste dans l'app

- **Decision**: un nouvel onglet « Menu » dans la barre du bas (6ᵉ onglet), route `menu`, avec deux vues dans le même écran : « Menu » et « Courses ». Ajout au menu aussi depuis la fiche d'une recette (bouton « Ajouter au menu »).
- **Rationale**: le menu est un usage hebdomadaire, il doit être à un geste ; deux vues dans un écran évitent un 7ᵉ onglet. Six onglets tiennent sur un téléphone de 412 dp de large (environ 68 dp chacun, libellés courts).
- **Alternatives considered**: bouton dans la barre du haut de « Mes recettes » (moins visible) ; regrouper URL, Fichier et Photo en un onglet « Importer » pour libérer une place (changement d'habitude non demandé, à proposer plus tard).

## R2 — Stockage

- **Decision**: trois nouvelles tables Room, migration 6 → 7 par `CREATE TABLE` uniquement (aucune table existante touchée) :
  `menu_entries`, `shopping_checks`, `shopping_extras` (voir data-model.md). Pas de clé étrangère vers `recipes` : la suppression des entrées d'une recette supprimée est faite explicitement, dans la même transaction que la suppression de la recette.
- **Rationale**: une clé étrangère sur une table existante obligerait à activer et vérifier `foreign_keys` et rendrait la migration plus risquée ; la règle « prévenir puis retirer » (FR-005) se fait de toute façon dans l'app.
- **Alternatives considered**: JSON dans `SharedPreferences` (pas de transaction avec la suppression de recette, pas de test de migration) ; colonne « au menu » dans `recipes` (interdit plusieurs entrées pour une même recette, modifie la table des recettes).

## R3 — Calcul de la liste de courses

- **Decision**: fonction pure `ShoppingListBuilder.build(entries, recipes, parser)` (paquet `data/shopping`, Kotlin sans Android), recalculée à chaque changement du menu ou d'une recette. Elle réutilise `IngredientParser` et `FoodTable` de la fonctionnalité 001.
  - Lignes de titre ignorées avec la même règle que le calcul des calories (`isHeader`, à rendre partagée).
  - Facteur d'une entrée = parts prévues ÷ parts de la recette (indiquées, sinon estimées) ; si la recette n'a ni parts ni estimation, facteur = nombre de fois la recette choisi sur l'entrée (`times`, ×1 par défaut).
  - Regroupement par **clé d'article** : code Ciqual de l'aliment reconnu (`food:<code>`), sinon texte normalisé de l'aliment sans quantité ni unité (`txt:<texte>`).
  - Dans un article, quantités additionnées **par nature d'unité** : masse (g), volume (ml), pièces, sachets ; les quantités vagues ou sans quantité sont gardées telles qu'écrites, côte à côte (FR-009, FR-010).
  - Assaisonnements (`FoodKind.SEASONING`) : un seul article, sans quantité.
- **Parser** : `ParsedIngredient` reçoit un champ `amount: Amount?` (`kind` = MASS / VOLUME / PIECE / SACHET, `value` en g, ml ou nombre) avec valeur par défaut `null`, pour ne casser ni les appels ni les tests existants. Les cuillères et verres sont des volumes (15 ml, 5 ml, 200 ml…), déjà définis.
- **Affichage** : g jusqu'à 999, puis kg à 0,1 près ; ml < 100 en ml, sinon cl jusqu'à 99 cl, puis l à 0,1 près ; pièces arrondies au supérieur (1,5 citron → 2) ; sachets entiers au supérieur.
- **Alternatives considered**: conversions volume ↔ masse avec la densité de la table (exclu par la spec : la liste montre ce qu'on achète, pas des grammes calculés) ; regroupement par texte seul (rate « œufs » / « oeuf », « tomates » / « tomate »).

## R4 — Chronodrive

- **Decision**: le bouton « Chronodrive » d'un article **copie le nom du produit** et **ouvre l'application Chronodrive** (`getLaunchIntentForPackage("com.ikomobi.chronodrive")`) ; l'utilisatrice colle le nom dans la recherche de l'application. Secours si l'application n'est pas installée : `https://www.chronodrive.com/search/<terme>` dans le navigateur. Détail dans `contracts/chronodrive-search.md`.
- **Rationale** (tests sur le téléphone, 2026-11-21) :
  - l'adresse `https://www.chronodrive.com/search/<terme>` fonctionne sur le site (200, résultats), mais l'application Chronodrive, qui revendique ces liens, affiche une page d'erreur au lieu des résultats ;
  - dans Chrome, le site impose le choix du magasin et le consentement aux cookies tant qu'ils n'ont pas été faits dans ce navigateur, et le panier n'est pas celui de l'application sans connexion ;
  - l'ouverture de l'application arrive sur le magasin et le panier de l'utilisatrice, sans fenêtre de choix.
- **Terme** : premier alias de l'aliment reconnu (nom courant), sinon texte de l'aliment sans quantité, unité ni article. Jamais la quantité.
- **Retour dans l'app** : clé du dernier article envoyé gardée (`rememberSaveable`) ; au retour, l'écran est au même endroit et l'article mis en évidence avec un bouton « Coché ».
- **Alternatives considered**: lien de recherche vers l'application (page d'erreur, testé) ; lien de recherche dans Chrome (fenêtre de magasin et cookies, panier séparé sans connexion) ; format interne `chronodrive://` (non documenté, fragile) ; API interne non officielle pour remplir le panier (identifiants, conditions d'utilisation, fragilité) — rejetée avec l'utilisatrice ; Jow (catalogue Jow seulement).

## R5 — Rayons (story 4)

- **Decision**: ajouter un champ facultatif `aisle` à chaque aliment de `foods.json`, calculé dans `tools/ciqual/build_foods.py` à partir du groupe et du sous-groupe Ciqual (`alim_grp_code`, `alim_ssgrp_code`), avec une colonne facultative de forçage dans `selection.csv`. Rayons : Fruits et légumes, Crèmerie, Boucherie et poisson, Épicerie salée, Épicerie sucrée, Surgelés, Boissons, Autres.
- **Rationale**: classement hors ligne, déterministe et testable ; la table est déjà générée par ce script.
- **Alternatives considered**: liste de mots-clés dans l'app (doublonne la table) ; demander à Hermes (dépend du PC, inutile pour 385 aliments fixes).

## R6 — Partage en texte

- **Decision**: `Intent.ACTION_SEND` `text/plain` via `Intent.createChooser`, comme le partage de recette existant. Format défini dans `contracts/shopping-text.md`.

## R7 — Suppression d'une recette au menu

- **Decision**: les deux chemins de suppression (liste « Mes recettes » et fiche) demandent `menuDao.countForRecipe(id)` ; si > 0, le texte de confirmation ajoute « Elle est au menu (n fois) et en sera retirée. ». La suppression de la recette et de ses entrées se fait dans une seule transaction.

## R8 — Tests et données

- Logique pure (regroupement, unités, facteur de parts, terme Chronodrive, texte partagé) en tests JVM avec la vraie table `foods.json` (comme `TestFoods`).
- Écrans en Compose UI tests ; migration 6 → 7 en test `room-testing` qui vérifie que les recettes sont identiques après migration.
- Tests lancés par `am instrument` uniquement (jamais `connectedAndroidTest`), réglages `url_import.xml` sauvegardés puis restaurés.
