# Contract: texte partagé de la liste de courses

Partage `Intent.ACTION_SEND`, type `text/plain`, sujet « Liste de courses ».

## Format

```
Liste de courses (5 recettes)

Fruits et légumes
- pommes : 6
- citron : 2

Crèmerie
- lait : 70 cl
- œufs : 11

Épicerie salée
- farine : 700 g
- tomates : 400 g + 1 boîte

Autres
- papier toilette
- sel, poivre (à vérifier)
```

## Règles

- Seuls les articles **non cochés** sont partagés (calculés et ajoutés à la main).
- Un article par ligne : `- <libellé>` puis ` : <quantités>` s'il y en a ; quantités de natures différentes séparées par ` + `.
- Story 4 livrée : titres de rayon, dans l'ordre fixe des rayons, rayons vides omis ; ajouts à la main puis assaisonnements à la fin de « Autres ». Si aucun article n'a de rayon connu (tout serait dans « Autres »), liste à plat sans titre, comme avant.
- Assaisonnements regroupés sur une ligne « (à vérifier) ».
- Nombres : virgule décimale, pas de zéros inutiles (`1,5 kg`, `70 cl`).
- Ligne d'en-tête : `Liste de courses (<n> recette(s))`, n = nombre d'entrées du menu.
- Liste vide (tout coché ou menu vide et aucun ajout) : le bouton Partager est désactivé.
