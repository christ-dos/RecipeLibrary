# Contrat : Hermes `POST /estimate` et ajouts à `POST /extract`

Serveur : Hermes (`recipe_extractor.py`), port 18642, réseau local uniquement.

## `POST /estimate` (nouveau)

Requête (`Content-Type: application/json`, UTF-8) :

```json
{
  "title": "Tarte aux pommes",
  "category": "Tartes et quiches",
  "ingredients": ["1 pâte brisée", "4 pommes", "1 noix de beurre", "sucre"]
}
```

- `ingredients` : 1 à 60 chaînes, obligatoire. `title` et `category` facultatifs (aident Llama).
- L'app n'envoie que les lignes qu'elle ne sait pas lire, mais Hermes accepte n'importe quelle ligne.

Réponse 200 :

```json
{
  "weights": [
    {"ingredient": "1 pâte brisée", "grams": 230},
    {"ingredient": "4 pommes", "grams": 600},
    {"ingredient": "1 noix de beurre", "grams": 10},
    {"ingredient": "sucre", "grams": null}
  ],
  "servings": 6
}
```

- `weights` : même ordre et même nombre que `ingredients`, `ingredient` recopié à l'identique. `grams` : nombre > 0 et ≤ 10 000, ou `null` si inconnu.
- `servings` : entier 1 à 100, ou `null`.
- Hermes valide et nettoie la sortie de Llama : valeur hors bornes ou non numérique → `null` ; réponse de Llama illisible → toutes les valeurs à `null` (jamais d'erreur 500 pour ça).

Erreurs : 400 `{"error": "..."}` si le corps est invalide ; 503 `{"error": "Ollama indisponible"}` si Llama ne répond pas (l'app retente plus tard).

Effets de bord : compte comme activité (`reset_idle_timer`) et bloque la veille pendant le calcul (`extraction_started/finished`), comme `/extract`.

## `POST /extract` (existant, champs ajoutés)

La réponse gagne deux champs facultatifs :

```json
{
  "title": "...", "ingredients": ["..."], "steps": ["..."], "category": "...",
  "servings": 4,
  "ingredientWeights": [{"ingredient": "2 pommes", "grams": 300}]
}
```

- `servings` vient du prompt d'extraction (« pour 4 personnes ») ; `null` si la source ne le dit pas.
- `ingredientWeights` vient de l'appel interne à la fonction d'estimation ; peut être absent si l'estimation a échoué : l'extraction réussit quand même (FR-010).
- Rétrocompatibilité : une app ancienne ignore ces champs ; une app nouvelle face à un Hermes ancien les trouve absents et calcule avec les seules règles.
