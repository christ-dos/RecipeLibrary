# Contract: bouton « Chronodrive »

**Sens** : l'app copie un terme et ouvre l'application Chronodrive. Aucune donnée personnelle, aucun identifiant, aucune action sur le panier.

## Comportement

1. Copier `<terme>` dans le presse-papiers (`ClipboardManager`, libellé « Chronodrive »).
2. Afficher un message court : `« <terme> » copié : regarde dans tes favoris ou colle-le dans la recherche 🔍`.
3. Ouvrir l'application Chronodrive sur « Mon espace favoris » : `chronodrive://favorites` (`ACTION_VIEW` + `setPackage`, vérifié le 2026-11-22 ; la loupe en haut permet de chercher ce qui n'est pas en favori). Si ce lien est refusé : `packageManager.getLaunchIntentForPackage("com.ikomobi.chronodrive")` (accueil).
4. Si elle n'est pas installée : ouvrir `https://www.chronodrive.com/search/<terme encodé>` dans le navigateur (`Intent.ACTION_VIEW`).
5. Si rien ne peut s'ouvrir : message « Impossible d'ouvrir Chronodrive » ; le terme reste copié.

**Ouverture directe sur la recherche : écartée (essai du 2026-11-22).** L'application déclare les liens `www.chronodrive.com/search/..*` (domaine vérifié), mais les ouvrir dans l'application (`ACTION_VIEW` + `setPackage`) affiche « Cette page s'est volatilisée… », même pour « eau » et après redémarrage de l'application, alors que la même adresse fonctionne sur le site.
6. La clé de l'article est mémorisée pour le mettre en évidence au retour (FR scénario 3.5).

## Constats sur le téléphone (2026-11-21, Galaxy S25, application `com.ikomobi.chronodrive`)

- L'application déclare les liens `https://www.chronodrive.com/search/..*` (domaine vérifié), mais un lien de recherche (`/search/lait`, `/search/cr%C3%A8me%20fra%C3%AEche`) y affiche « Cette page s'est volatilisée ». **Ne pas utiliser ces liens vers l'application.**
- Ouverture de l'application : accueil sur le magasin déjà choisi, sans fenêtre de choix du magasin (une publicité peut s'afficher au démarrage).
- Dans Chrome sans historique Chronodrive : fenêtre « Choisir mon Chrono » et bandeau de cookies. D'où l'usage du navigateur seulement en secours.
- Le format de lien interne `chronodrive://` n'est pas documenté : non utilisé.

## Terme

| Cas | Terme |
|---|---|
| Aliment reconnu | premier alias de l'aliment dans `foods.json` (« farine », « fromage blanc 0% », « oeuf ») |
| Aliment non reconnu | texte de l'aliment sans quantité, sans unité, sans article de tête (« de », « d' », « du », « des ») |
| Article ajouté à la main | son texte, sans quantité de tête |

Règles : jamais de quantité ni d'unité ; 1 à 60 caractères ; espaces multiples réduits.

| Ligne | Terme copié |
|---|---|
| `450 g de farine` | `farine` |
| `20 cl de crème fraîche` | `creme fraiche` *(alias)* |
| `1 c. à café de piment d'Espelette` (non reconnu) | `piment d'Espelette` |
| `papier toilette` (à la main) | `papier toilette` |
