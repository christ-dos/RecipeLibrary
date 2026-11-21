# Research: Calories par part

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-11-21

## R1. Source des valeurs nutritionnelles

- **Decision**: Table Ciqual 2020 de l'Anses (fichier XLSX officiel), réduite par un script de développement à environ 300 aliments courants, embarquée dans l'app sous forme d'un fichier JSON d'assets (`app/src/main/assets/nutrition/foods.json`). Le script et la liste d'aliments choisis (avec leurs variantes de nom, poids par pièce, masse volumique) sont versionnés dans `tools/ciqual/`.
- **Rationale**: Données officielles françaises, sous Licence Ouverte Etalab (réutilisation libre avec mention de la source) ; hors ligne ; noms d'aliments en français, comme les recettes. Un fichier JSON d'environ 60 Ko se charge en quelques millisecondes et n'alourdit pas la base Room.
- **Alternatives considered**: Open Food Facts (produits de marque, pas d'aliments génériques, besoin de réseau) ; USDA FoodData Central (noms en anglais) ; table complète Ciqual (~3 200 aliments, beaucoup de bruit pour l'appariement, 1 Mo+) ; table dans Room (migration et préremplissage inutiles pour des données en lecture seule).
- **À vérifier à l'implémentation**: URL exacte du fichier Ciqual 2020 sur ciqual.anses.fr et mention de licence ; la mention « Source : Anses, table Ciqual 2020 » est affichée dans le détail.

## R2. Lecture des quantités (règles fixes, côté app)

- **Decision**: Analyseur Kotlin pur (sans dépendance Android) en trois temps : (1) quantité (entiers, décimales à virgule, fractions `1/2`, `½ ¼ ¾`, fourchettes `2 à 3` / `100-150` → moyenne, mots « un/une/deux… douze ») ; (2) unité (g, kg, mg, ml, cl, dl, l, c. à soupe/càs/cs, c. à café/càc/cc, verre, tasse, pincée, noix, gousse, tranche, sachet, botte, pièce implicite) ; (3) aliment = le reste, nettoyé (« de », « d' », parenthèses, adjectifs courants « frais », « haché »…).
- **Conversion en grammes**: masse → direct ; volume → ml × masse volumique de l'aliment (défaut 1,0 g/ml) ; c. à soupe = 15 ml, c. à café = 5 ml, verre = 200 ml, tasse = 250 ml ; pièce → poids moyen par pièce de l'aliment (sinon non compté) ; sachet de levure 11 g, sachet de sucre vanillé 7,5 g ; pincée, « selon le goût », « un peu » → non compté (raison VAGUE).
- **Rationale**: Résultat déterministe, testable en tests unitaires JVM rapides ; couvre la grande majorité des lignes écrites proprement.
- **Alternatives considered**: Tout confier à Llama (non déterministe, rejeté en spec) ; bibliothèque NLP (aucune pour le français culinaire, lourd).

## R3. Appariement ingrédient → aliment de référence

- **Decision**: Normalisation (minuscules, accents retirés, `œ`→`oe`, pluriels simples `s/x/aux`), puis recherche de la plus longue variante de nom contenue dans le texte de l'ingrédient, par mots entiers (« beurre demi-sel » → « beurre demi-sel » avant « beurre »). Chaque aliment a une liste de variantes dans `tools/ciqual/selection.csv`.
- **Rationale**: Simple, explicable, sans faux positifs grossiers (mots entiers) ; la plus longue correspondance gère les aliments composés.
- **Alternatives considered**: Distance d'édition (faux positifs : « riz » ≈ « ris de veau ») ; embeddings (trop lourd pour le téléphone).

## R4. Estimation des quantités vagues par le PC

- **Decision**: Nouveau point d'entrée Hermes `POST /estimate` qui reçoit le titre, la catégorie et la liste des ingrédients, et renvoie un poids en grammes (ou `null`) par ingrédient et un nombre de parts (ou `null`). Appel à Llama 3.1 local avec `"format": "json"` et `temperature: 0` pour limiter la variabilité. Pendant `/extract`, Hermes appelle la même fonction après l'extraction et ajoute `servings` et `ingredientWeights` à la réponse ; le prompt d'extraction existant gagne seulement le champ `servings`.
- **Rationale**: Une seule fonction d'estimation, testée une fois, utilisée par les deux chemins (extraction et rattrapage) ; le prompt d'extraction, qui marche bien, reste quasi intact. Mesures dans `logs/extractor.log` : une réponse Llama prend 7 à 27 s pour une extraction complète ; une estimation de poids (prompt court) devrait prendre 5 à 10 s, soit 5 à 15 min de rattrapage pour environ 80 recettes.
- **Alternatives considered**: Ajouter les poids dans le prompt d'extraction (risque de dégrader l'extraction et de mélanger les rôles) ; endpoint par ingrédient (trop d'allers-retours).
- **Règle de priorité (côté app)**: poids corrigé à la main > quantité écrite lisible > poids estimé par le PC. L'app ne retient le poids du PC que pour les lignes non lisibles par les règles.

## R5. Rattrapage et file d'attente

- **Decision**: Le même `ImportQueueWorker` (contrainte Wi-Fi, nouvel essai linéaire 1 min, réveil du PC via `HermesImporter`) enchaîne deux traitements : les liens en attente (inchangé), puis un `WeightEstimationProcessor` qui cherche les recettes ayant au moins une ligne vague sans estimation enregistrée. Pas de nouvelle ligne dans `pending_imports` : la liste à traiter est calculée à partir des recettes elles-mêmes, ce qui couvre à la fois le rattrapage unique et le « fil de l'eau » (une recette enregistrée ou modifiée avec une ligne vague nouvelle relance le worker).
- **Rationale**: Aucun état dupliqué à synchroniser (si une recette est supprimée ou modifiée, rien à nettoyer) ; réutilise le réveil, le Wi-Fi et les nouveaux essais déjà éprouvés.
- **Marquage « déjà estimé »**: chaque ligne envoyée au PC reçoit une entrée dans `ingredient_weights`, même si le PC répond `null` (source `PC`, grammes `null`), pour ne pas la renvoyer. La clé de l'entrée est le texte exact de la ligne : modifier le texte invalide l'entrée (FR-021).
- **Progression**: l'écran de la file affiche « Estimation des quantités : n / N » tant qu'il reste des recettes à traiter ; pas de notification (rien à valider).
- **Alternatives considered**: Nouveau type de ligne dans `pending_imports` (doublons et nettoyage à gérer) ; worker séparé (deux réveils et deux politiques de nouvel essai concurrentes).

## R6. Import texte ou fichier : estimation immédiate

- **Decision**: Après l'analyse du texte, si l'une des lignes est vague, l'app vérifie `/health` (délai court, 2 s, sans réveil) ; si le PC répond, elle appelle `/estimate` (délai 45 s, indicateur « Estimation des quantités… ») et pré-remplit la recette ; sinon elle continue sans attendre, et le worker s'en chargera après l'enregistrement.
- **Rationale**: Respecte « tout de suite si le PC est joignable, sinon la file » sans jamais bloquer l'import ; pas de réveil à ce moment pour ne pas imposer 30 s d'attente.

## R7. Stockage

- **Decision**: Migration Room 5 → 6 en `ALTER TABLE recipes ADD COLUMN` : `servings INTEGER`, `servings_source TEXT`, `ingredient_weights TEXT NOT NULL DEFAULT '{}'` (JSON). Aucune donnée existante modifiée. Test de migration ajouté à `DatabaseMigrationTest` (les recettes v5 relues à l'identique en v6).
- **Rationale**: Même méthode que les migrations précédentes, éprouvée ; pas de nouvelle table car les poids appartiennent à la recette.
- **Alternatives considered**: Table `ingredient_weights` séparée (jointures et migrations plus lourdes pour ~10 lignes par recette).

## R8. Estimation du nombre de parts

- **Decision**: Fonction pure : `parts = round(poidsCompté / poidsPart[catégorie])`, minimum 1, non calculée si poids non compté > 50 % du poids connu+estimé ou sans catégorie ; table des poids de part = FR-016. Priorité : `USER` > `SOURCE` > estimé (non stocké, recalculé à l'affichage).
- **Note**: le poids d'une ligne non comptée est inconnu par définition ; la spec (FR-013, FR-017) fixe donc la règle sur le nombre de lignes : plus de la moitié des lignes non comptées (hors sel, poivre, épices, signalés mais ignorés) → pas d'estimation de parts et avertissement « estimation partielle ».
