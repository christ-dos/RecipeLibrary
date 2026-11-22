# Table des aliments (Ciqual 2020)

`app/src/main/assets/nutrition/foods.json` est généré à partir de la table officielle Ciqual 2020 de l'Anses et de `selection.csv`. Ne pas éditer le JSON à la main.

## Source et licence

- **Fichier utilisé** : `Table Ciqual 2020_FR_2020 07 07.xls` (version française, XLS, 3,6 Mo, md5 `9830e49b787d6063cdc72a04c090c813`).
- **Téléchargement** : <https://zenodo.org/records/4770600> (dépôt officiel Anses, DOI 10.5281/zenodo.4770600). Le lien de ciqual.anses.fr donné par data.gouv.fr répondait 503 le 21/11/2026.
- **Licence** : Creative Commons Attribution 4.0 (Zenodo) ; Licence Ouverte / Open Licence (fiche data.gouv.fr). Les deux autorisent la réutilisation, à condition de citer la source.
- **Mention à afficher** : « Source : Anses, table Ciqual 2020 » (affichée dans le détail des calories). Citation complète : « Anses. 2020. Table de composition nutritionnelle des aliments Ciqual. https://ciqual.anses.fr/ ».
- Une version plus récente existe (recherche.data.gouv.fr, doi:10.57745/RDMHWY) ; la 2020 a été retenue pour cette fonctionnalité.

## Valeurs retenues

- `kcal` : colonne « Energie, Règlement UE N° 1169/2011 (kcal/100 g) », sinon « Energie, N x facteur Jones, avec fibres (kcal/100 g) », sinon calculée avec les coefficients du règlement UE 1169/2011 (protéines 4, glucides 4, lipides 9, fibres 2, alcool 7). Le script liste les aliments calculés.
- `protein` : « Protéines, N x facteur de Jones » ; `carbs` : « Glucides » ; `fat` : « Lipides » (g/100 g).
- « traces » et « < x » → 0 ; « - » → 0 pour les nutriments.

## `selection.csv`

Format : `code;aliases;pieceG;density;kind;aisle`

- `code` : code Ciqual (`alim_code`), ou `-` pour un assaisonnement sans équivalent Ciqual (tabasco, thé…), qui reçoit un code `X1`, `X2`… et 0 kcal.
- `aliases` : noms reconnus dans les recettes, séparés par `|`, en minuscules et sans accents. Chaque alias doit être unique, y compris après normalisation (le script échoue sinon).
- `pieceG` : poids moyen d'une pièce en grammes (œuf 55, oignon 100…), vide sinon.
- `density` : g/ml pour les mesures en volume (huile 0,92, farine 0,55…), vide = 1,0.
- `kind` : `NORMAL`, `SEASONING` (sel, poivre, épices, herbes : ignorés sans avertissement) ou `FRYING` (huile de friture : comptée mais signalée approximative).
- `aisle` (facultatif) : force le rayon de la liste de courses. Vide = rayon déduit du sous-groupe puis du groupe Ciqual (tables `AISLE_BY_SUBGROUP` et `AISLE_BY_GROUP` du script), sinon « Autres ». Forcé aujourd'hui : farines et fécules (Épicerie sucrée), pâtes à tarte et brick (Crèmerie), et les assaisonnements sans code Ciqual.

## Regénérer

Depuis la racine du dépôt, avec le fichier téléchargé dans un dossier à part :

```
C:\Users\Kikine\AppData\Local\hermes\hermes-agent\venv\Scripts\python.exe -I tools\ciqual\build_foods.py "<chemin>\Table Ciqual 2020_FR_2020 07 07.xls"
```

Dépendance : `xlrd` (installé dans le venv de Hermes), car le fichier officiel est au format XLS et non XLSX.

Puis lancer `FoodsJsonTest` (`:app:testDebugUnitTest`).
