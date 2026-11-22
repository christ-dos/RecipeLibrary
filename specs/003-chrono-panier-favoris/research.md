# Research: Mes produits Chronodrive (US1 à US3)

## R1. Ouverture directe de la fiche produit

- **Decision**: `Intent.ACTION_VIEW` sur `chronodrive://product/<numéro>` avec `setPackage("com.ikomobi.chronodrive")` ; repli `https://www.chronodrive.com/product/<numéro>` (navigateur) ; repli final : comportement actuel (favoris, accueil, recherche du site).
- **Rationale**: vérifié sur le téléphone le 2026-11-22 (citron n° 122974). Même mécanisme que `chronodrive://favorites` déjà en place ; l'application est déjà déclarée dans `<queries>`.
- **Alternatives considered**: lien de recherche `https://www.chronodrive.com/search/...` (écarté en 002 : « Cette page s'est volatilisée ») ; adresse complète du site `/<marque>--<slug>-P<numéro>` (inutile : le numéro seul suffit et évite de stocker le slug) ; service d'accessibilité (refusé par l'utilisatrice).

## R2. Format du fichier de favoris

- **Decision**: le fichier texte du relevé (`favoris-releve-2026-11-22.txt`) est lui-même le format d'import : titres de rayon `## Rayon (n)`, puis une ligne de produits `numéro slug` séparés par `;`. Marque et nom sont déduits du slug (`<marque>--<nom>` ; sans `--`, pas de marque). Voir contracts/favorites-file.md.
- **Rationale**: le relevé réel existe déjà et sert de premier import et de jeu de test ; aucun outil de conversion à écrire ; le rayon vient des titres. Le nom tiré du slug est sans accent ni majuscule, ce qui est sans effet sur la correspondance (normalisée) et acceptable à l'affichage.
- **Alternatives considered**: JSON (plus strict mais demande un convertisseur) ; CSV (même défaut, rayon répété). Un format JSON pourra s'ajouter plus tard sans casser celui-ci (US4 pourra produire directement des produits).

## R3. Règle de correspondance (FR-002)

- **Decision**: `Normalize.words` (déjà utilisé par les aliments : minuscules, accents retirés, `œ`→`oe`, pluriel simple retiré mot par mot) appliqué au terme et au nom du produit **sans la marque** ; un produit correspond si chaque mot significatif du terme est parmi les mots du nom (égalité de mots entiers). Mots ignorés : mots d'un caractère et mots vides (`de`, `du`, `des`, `la`, `le`, `les`, `un`, `une`, `et`, `au`, `aux`, `en`, `a`) ; si le terme ne contient plus aucun mot significatif, aucun produit ne correspond.
- **Rationale**: réutilise le code testé des features 001/002, donc même comportement pour « oeuf »/« oeufs » ; « eau » ne correspond pas à « gateau » (mots entiers) ; « lait » correspond à plusieurs produits, d'où le choix explicite (FR-004).
- **Alternatives considered**: sous-chaîne (écartée : « eau » dans « gâteau ») ; distance d'édition ou score (écartés : l'app ne doit jamais choisir à la place de l'utilisatrice). Tri des candidats : alphabétique sur le nom, puis numéro.

## R4. Clé d'article pour mémoriser le choix (FR-003)

- **Decision**: clé = celle des coches (`food:<code>` ou `txt:<texte normalisé>`) pour les articles calculés ; pour un article ajouté à la main (dont la clé de ligne est `extra:<id>`, propre à une liste), clé `txt:` + `Normalize.text(termForExtra(texte))`.
- **Rationale**: le choix doit valoir pour toutes les listes suivantes (FR-003) ; l'identifiant d'un ajout à la main ne survit pas à sa suppression. « papier toilette » écrit à la main et issu d'une recette partagent ainsi le même choix, ce qui est voulu.
- **Alternatives considered**: clé `extra:<id>` (le choix serait perdu à chaque nouvelle liste).

## R5. Stockage et migration

- **Decision**: deux tables (`chronodrive_products`, `product_choices`), migration Room 8 → 9 par `CREATE TABLE IF NOT EXISTS` seulement, schéma identique à celui que Room génère. Pas de clé étrangère (même convention que 002) : un choix garde son numéro de produit même si le produit n'est plus en favori ; les produits ne sont jamais supprimés par l'import.
- **Rationale**: aucune donnée existante touchée (FR-011, SC-005) ; réimport sans perte (FR-001).
- **Test de migration**: le projet n'exporte pas de schémas Room (`room.schemaLocation` absent) donc pas de `MigrationTestHelper`. Le test (`DatabaseMigrationTest`) ouvre la base Room en mémoire, y insère recette, entrée de menu, coche et ajout à la main, relève les colonnes des deux nouvelles tables (`PRAGMA table_info`), les supprime, exécute `MIGRATION_8_9.migrate(...)`, puis vérifie (a) les anciennes lignes identiques, (b) les nouvelles tables présentes avec les mêmes colonnes qu'avant suppression, (c) le DAO écrit et lit dans les nouvelles tables. `MIGRATION_8_9` est aussi ajoutée à la liste des migrations du test existant.
- **Alternatives considered**: activer l'export de schémas (changement de build, historique v1-v7 absent) ; `fallbackToDestructiveMigration` (interdit : perte de données).

## R6. Import par fichier

- **Decision**: sélecteur de documents du système (`ActivityResultContracts.OpenDocument`, types `text/*`), comme `FileImportScreen` ; lecture UTF-8 ; analyse entièrement faite avant toute écriture ; écriture en une transaction ; fichier sans aucun produit valide : rien n'est modifié, message explicite.
- **Rationale**: aucune permission ; atomique ; mêmes habitudes que l'import de recettes.
- **Fusion**: nouveau numéro → ajouté ; numéro connu → nom, marque, rayon mis à jour, `still_favorite = 1` ; numéro absent du fichier → conservé avec `still_favorite = 0` (jamais supprimé). Message « N ajoutés, M déjà connus ».
- **Alternatives considered**: import par presse-papiers (fragile) ; lecture des favoris sur le compte (US4).

## R7. Choix parmi plusieurs candidats et retour au premier plan

- **Decision**: boîte de choix (nom, marque, rayon, numéro) ; « Aucun de ceux-là » ouvre les favoris (comportement actuel) sans rien mémoriser. Après le choix, la fiche s'ouvre et la clé de l'article est mémorisée pour la mise en évidence au retour (comportement 002, `sentKey`).
- **Rationale**: FR-004 (l'app ne choisit jamais entre plusieurs) ; réutilise le mécanisme existant.
- **Cas à même nom** (« Emmental râpé » n° 368958 et 82295, « eau de source » × 3, « banane » × 2) : le numéro est affiché pour les distinguer ; aucune autre information n'existe dans le relevé.

## R8. Faisabilité de US4 (étude préalable, hors de ce plan)

- **Decision**: ne rien planifier ni préparer d'autre que le schéma neutre. L'étude de faisabilité (à faire avant le plan de la phase US4) doit établir : (1) existence d'une API d'ajout au panier et de lecture des favoris, observée depuis le site (réseau du navigateur), (2) mode de connexion et durée de vie du jeton sans conserver le mot de passe, (3) risque de blocage ou d'incompatibilité avec les conditions d'utilisation, (4) stabilité des réponses.
- **Rationale**: fonctionnement non documenté (spec, Assumptions) ; US2 sert de repli complet.

## Questions ouvertes

1. **Produits non relevés** : le relevé compte 120 produits sur 125 (rayons Animaux, Maison & Loisirs, Vins, Parapharmacie, et 1 produit laitier manquants). Faut-il compléter le fichier avant le premier import ? Sans effet sur le code : un réimport ajoutera les 5 manquants.
2. **Accents et majuscules à l'affichage** : les noms viennent des slugs (« creme fraiche epaisse legere »). Accepté pour cette phase ; un nom affiché propre exigerait un fichier plus riche (ou US4).
3. **Point d'entrée de l'écran** : proposé dans le menu de l'onglet Courses (« Mes produits Chronodrive ») ; à confirmer avec elle à la revue de l'écran.
