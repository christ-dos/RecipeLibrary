# Data Model: Mes produits Chronodrive

Base Room `recipes.db`, version **8 → 9**. Deux nouvelles tables ; aucune table existante (`recipes`, `pending_imports`, `menu_entries`, `shopping_checks`, `shopping_extras`) n'est modifiée.

## ChronodriveProduct (`chronodrive_products`)

| Champ | Colonne | Type | Notes |
|---|---|---|---|
| numéro | `product_id` | INTEGER, clé primaire | numéro du site et de l'application (ex. 365920) ; deux produits peuvent avoir le même nom |
| nom | `name` | TEXT NOT NULL | sans la marque, déduit du slug (« viande hachee pur boeuf 5 mg ») |
| marque | `brand` | TEXT, nullable | ex. « auchan » ; null pour les produits sans marque (« banane ») |
| rayon | `aisle` | TEXT NOT NULL | titre du rayon du fichier (« Epicerie salée ») |
| encore en favori | `still_favorite` | INTEGER NOT NULL DEFAULT 1 | 0 si absent du dernier import |
| date d'import | `imported_at` | INTEGER NOT NULL | millisecondes, dernier import qui l'a vu |

Index : aucun en plus de la clé (~125 lignes).

**Règles** : jamais supprimé par un import (FR-001) ; réimport : mise à jour de `name`, `brand`, `aisle`, `still_favorite = 1`, `imported_at`.

## ProductChoice (`product_choices`)

| Champ | Colonne | Type | Notes |
|---|---|---|---|
| clé d'article | `item_key` | TEXT, clé primaire | `food:<code Ciqual>` ou `txt:<texte normalisé>` (research R4) |
| produit retenu | `product_id` | INTEGER NOT NULL | numéro de `chronodrive_products` ; pas de clé étrangère |
| date du choix | `chosen_at` | INTEGER NOT NULL | millisecondes |

Index : `index_product_choices_product_id` sur `product_id` (recherche « quels articles utilisent ce produit », pour l'écran et pour afficher « plus en favori »).

**Règles** : un article = au plus un produit ; « oublier » supprime la ligne ; changer remplace la ligne ; un choix n'est jamais supprimé par un import.

## SQL de migration 8 → 9 (indicatif ; à valider contre le schéma que Room génère)

```text
CREATE TABLE IF NOT EXISTS `chronodrive_products` (`product_id` INTEGER NOT NULL, `name` TEXT NOT NULL,
  `brand` TEXT, `aisle` TEXT NOT NULL, `still_favorite` INTEGER NOT NULL DEFAULT 1,
  `imported_at` INTEGER NOT NULL, PRIMARY KEY(`product_id`))
CREATE TABLE IF NOT EXISTS `product_choices` (`item_key` TEXT NOT NULL, `product_id` INTEGER NOT NULL,
  `chosen_at` INTEGER NOT NULL, PRIMARY KEY(`item_key`))
CREATE INDEX IF NOT EXISTS `index_product_choices_product_id` ON `product_choices` (`product_id`)
```

Le test de migration compare les colonnes obtenues par la migration à celles que Room crée (research R5).

## Objets non stockés (calculés)

- **Correspondance** : `ProductMatcher.candidates(term, produits)` → liste de produits ; recalculée à chaque appui sur « Chrono » (~125 produits).
- **Résultat d'import** : `ImportReport(added, known, markedGone, ignored)` ou erreur ; affiché en message.
- **Résolution de « Chrono »** : `Retained(product)` | `Single(product)` | `Several(candidates)` | `None`.

## Transitions

```text
article sans choix --(1 candidat, « Chrono »)--> choix créé
article sans choix --(n candidats, choix de l'utilisatrice)--> choix créé
choix --(changer)--> choix remplacé
choix --(oublier)--> aucun choix (l'app redemandera)
produit --(absent du réimport)--> still_favorite = 0 (conservé)
produit --(présent au réimport)--> still_favorite = 1, nom mis à jour
```

US4 (non planifiée) : un éventuel jeton de connexion sera stocké à part ; rien ici ne l'anticipe.
