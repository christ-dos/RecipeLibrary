# Contrat : `assets/nutrition/foods.json`

Généré par `tools/ciqual/build_foods.py` à partir du fichier Ciqual 2020 et de `tools/ciqual/selection.csv`. Ne pas éditer à la main.

```json
{
  "source": "Anses, table de composition nutritionnelle Ciqual 2020 (Licence Ouverte)",
  "version": 1,
  "foods": [
    {
      "code": "9410",
      "name": "Farine de blé tendre",
      "aliases": ["farine", "farine de ble", "farine t45", "farine t55"],
      "kcal": 343, "protein": 10.8, "carbs": 70.5, "fat": 1.2,
      "pieceG": null, "density": 0.55, "kind": "NORMAL"
    }
  ]
}
```

Règles vérifiées par un test unitaire du fichier :
- au moins 300 aliments ; `code` unique ; chaque alias unique dans tout le fichier ;
- alias en minuscules, sans accents, sans `œ` ;
- `kcal` entre 0 et 910 (900 pour un corps gras pur, plus l'arrondi de Ciqual : huile de tournesol 901), nutriments entre 0 et 100 ;
- `pieceG` > 0 si présent ; `density` entre 0,1 et 2 si présente ;
- `kind` ∈ `NORMAL`, `SEASONING`, `FRYING`.
- `aisle` (facultatif, ajouté par la spec 002 story 4) ∈ « Fruits et légumes », « Crèmerie », « Boucherie et poisson », « Épicerie salée », « Épicerie sucrée », « Surgelés », « Boissons », « Autres » ; absent = « Autres ».

`selection.csv` (versionné, édité à la main) : `code;aliases (séparés par |);pieceG;density;kind;aisle` (`aisle` facultatif : force le rayon).
