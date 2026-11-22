# Contract: fichier de favoris Chronodrive

**Sens** : l'utilisatrice choisit un fichier texte (UTF-8) ; l'app le lit, ne l'envoie nulle part. Référence : [favoris-releve-2026-11-22.txt](../favoris-releve-2026-11-22.txt) (120 produits).

## Format

```text
# Commentaire (ligne ignorée)
## <Rayon> (<n> ou <n> sur <m>)
<numéro> <slug>;<numéro> <slug>;...
## <Autre rayon> (...)
...
```

- Encodage UTF-8, fins de ligne `\n` ou `\r\n`, BOM éventuel ignoré.
- Une ligne `## ` ouvre un rayon : le nom est le texte jusqu'à la parenthèse finale exclue, espaces de bord retirés (`Epicerie salée`, `Produits laitiers & Oeufs`). Les compteurs `(20 sur 21)` sont informatifs, jamais contrôlés. Une ligne sans parenthèse finale (`## Restent ~5 : ...`) est un titre sans produit : le texte sert de rayon, et comme la ligne n'a pas de produits valides elle n'ajoute rien.
- Une ligne commençant par un seul `#` suivi d'une espace est un commentaire ; une ligne vide est ignorée.
- Toute autre ligne contient des produits séparés par `;` ; chaque produit est `<numéro> <slug>` : numéro entier positif, une ou plusieurs espaces, slug `[a-z0-9-]+`.
- Un produit avant le premier `## ` reçoit le rayon « Autre ».

## Slug -> marque et nom

1. Si le slug contient `--` : marque = partie avant le premier `--`, nom = le reste ; sinon pas de marque, nom = slug entier.
2. Dans le nom, `---` devient ` - ` puis chaque `-` devient une espace, espaces multiples réduites.
3. Marque : les `-` deviennent des espaces.

| Entrée | Marque | Nom |
|---|---|---|
| `365920 banane` | (aucune) | `banane` |
| `55785 lactel--matin-leger---lait-sans-lactose-12-mg-uht` | `lactel` | `matin leger - lait sans lactose 12 mg uht` |
| `173555 happy-bio--graines-de-chia-bio` | `happy bio` | `graines de chia bio` |

## Validité et erreurs

| Cas | Comportement |
|---|---|
| Au moins 1 produit valide | importé ; les éléments mal formés sont ignorés et comptés (« 2 éléments ignorés ») |
| Aucun produit valide, fichier vide, illisible ou non UTF-8 | rien n'est modifié ; message « Ce fichier ne contient aucun favori Chronodrive reconnu » |
| Même numéro deux fois dans le fichier | le premier gagne (comportement déterministe) |
| Fichier de plus de 1 Mo | refusé comme invalide (garde-fou) |

## Résultat de la fusion

`ImportReport(added, known, markedGone, ignored)` ; message : « 120 produits Chronodrive importés » au premier import, « 3 ajoutés, 120 déjà connus » ensuite (+ « 1 plus en favori » si `markedGone > 0`). Règles de fusion : voir data-model.md.

## Tests de référence (JVM)

- Le relevé réel : 120 produits, 12 rayons, premier numéro 189100 (`champignons-bruns-bio`), dernier 500297 (`biolane--lingettes-nettoyantes`).
- `banane` : deux produits distincts (365920 et 305254) ; `emmental rape` : 368958 et 82295.
- Fichier vide, fichier sans rayon, élément mal formé au milieu, `\r\n`, BOM.
