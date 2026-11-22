# Feature Specification: Calories par part

**Feature Branch**: `001-calories-par-part`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "Calcul des calories par part pour chaque recette, par méthode hybride. Ajouter le nombre de parts à une recette (récupéré à l'import quand la source l'indique, sinon saisi par l'utilisatrice). Estimer les calories totales puis par part : le poids en grammes de chaque ingrédient vient soit de la quantité écrite (200 g, 3 œufs, 2 c. à soupe, 25 cl), soit, pour les quantités vagues (1 pomme, une noix de beurre), d'une estimation faite par Hermes (le PC, modèle Llama local) au moment de l'extraction ; les calories pour 100 g viennent d'une table de référence officielle (Ciqual de l'Anses) embarquée dans l'app, utilisable hors ligne. Afficher sur la fiche « ≈ X kcal / part », le détail par ingrédient et la liste des ingrédients non comptés. L'utilisatrice peut corriger le poids d'un ingrédient et le total se met à jour. Les recettes déjà enregistrées et celles importées en texte ou fichier sont calculées avec la seule lecture des quantités. [Précisé en clarification : le PC complète ensuite automatiquement leurs quantités vagues.] L'estimation doit rester indicative (environ ±15 %). Les données existantes ne doivent jamais être perdues."

## Clarifications

### Session 2026-11-21

- Q: Calculer seulement les calories, ou aussi protéines, glucides et lipides ? → A: Calories affichées sur la fiche ; protéines, glucides et lipides par part visibles seulement dans le détail.
- Q: Que faire quand le nombre de parts n'est pas connu ? → A: Estimer les parts d'après le poids total des ingrédients et le poids habituel d'une part selon la catégorie ; affichées « ≈ N parts (estimé) », modifiables ; un nombre indiqué par la source ou par l'utilisatrice prime toujours.
- Q: Le PC aide-t-il aussi les recettes qui ne passent pas par l'extraction par lien ? → A: Oui, automatiquement et sans bouton : rattrapage unique des recettes existantes, puis au fil de l'eau pour chaque recette ajoutée ou modifiée, uniquement si elle a au moins un ingrédient à quantité vague non comptée.
- Q: Quand l'estimation d'une nouvelle recette est-elle faite ? → A: Tout de suite, pendant l'import (avec l'extraction pour un lien, au moment de l'import pour un texte ou un fichier) si le PC est joignable ; sinon la recette passe par la file d'attente.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Voir les calories par part d'une recette (Priority: P1)

L'utilisatrice ouvre la fiche d'une recette (déjà enregistrée ou nouvelle) et voit une estimation « ≈ X kcal / part », calculée à partir des quantités écrites dans la liste des ingrédients et du nombre de parts.

**Why this priority**: C'est la valeur centrale de la fonctionnalité. Elle marche pour toutes les recettes, y compris celles déjà enregistrées, sans dépendre du PC.

**Independent Test**: Créer une recette « Pâte à crêpes » pour 4 parts avec « 250 g de farine, 4 œufs, 50 cl de lait, 50 g de beurre » ; la fiche affiche une valeur par part proche de la valeur de référence calculée à la main (à ±15 %).

**Acceptance Scenarios**:

1. **Given** une recette de 4 parts dont tous les ingrédients ont une quantité lisible et un aliment reconnu, **When** l'utilisatrice ouvre la fiche, **Then** elle voit « ≈ X kcal / part », où X est le total divisé par 4, arrondi à la dizaine.
2. **Given** une recette « Gâteaux » sans nombre de parts dont 600 g d'ingrédients sont comptés, **When** l'utilisatrice ouvre la fiche, **Then** elle voit « ≈ 6 parts (estimé) » et la valeur par part correspondante, et peut corriger le nombre de parts d'un geste.
3. **Given** une recette sans nombre de parts dont plus de la moitié des lignes d'ingrédients (hors sel, poivre et épices) ne sont pas comptées (ou sans catégorie), **When** l'utilisatrice ouvre la fiche, **Then** elle voit le total de la recette (« ≈ X kcal pour la recette ») et une invitation à indiquer le nombre de parts.
4. **Given** une recette déjà enregistrée avant cette fonctionnalité, **When** l'utilisatrice l'ouvre, **Then** l'estimation est affichée sans qu'elle ait rien à ressaisir, et le contenu de la recette est inchangé.
5. **Given** une recette dont aucun ingrédient n'est reconnu, **When** l'utilisatrice ouvre la fiche, **Then** aucune valeur chiffrée n'est affichée et un message indique que l'estimation est impossible.

---

### User Story 2 - Comprendre et corriger l'estimation (Priority: P2)

Depuis la fiche, l'utilisatrice ouvre le détail : pour chaque ingrédient, le poids retenu et les calories correspondantes ; puis la liste des ingrédients non comptés et la raison (quantité absente ou vague, aliment inconnu). Elle peut saisir ou corriger le poids d'un ingrédient ; le total et la valeur par part se mettent à jour immédiatement et la correction est conservée.

**Why this priority**: Rend l'estimation vérifiable et permet de la rendre juste quand la lecture automatique échoue.

**Independent Test**: Sur une recette contenant « 1 noix de beurre » (non comptée), saisir 10 g ; le beurre passe dans les ingrédients comptés et le total augmente d'environ 75 kcal.

**Acceptance Scenarios**:

1. **Given** une recette avec un ingrédient non compté, **When** l'utilisatrice ouvre le détail, **Then** l'ingrédient apparaît dans « Non comptés » avec sa raison.
2. **Given** le détail ouvert, **When** l'utilisatrice saisit un poids pour un ingrédient, **Then** le total et la valeur par part sont recalculés et la valeur saisie est conservée à la prochaine ouverture.
3. **Given** un poids corrigé à la main, **When** la recette est modifiée sans toucher à cet ingrédient, **Then** la correction est conservée.
4. **Given** l'utilisatrice modifie le texte de cet ingrédient, **When** elle enregistre, **Then** la correction ne s'applique plus à l'ancien texte et le poids est relu à partir du nouveau texte.

---

### User Story 3 - Estimation automatique des quantités vagues à l'import par lien (Priority: P3)

Lors de l'extraction d'une recette depuis un lien (vidéo, page), le PC fournit en plus le nombre de parts indiqué par la source et un poids estimé en grammes pour chaque ingrédient, ce qui permet de compter « 1 pomme » ou « une noix de beurre » sans saisie.

**Why this priority**: Améliore la couverture du calcul pour les recettes importées par lien, mais la fonctionnalité est déjà utile sans elle (stories 1 et 2).

**Independent Test**: Importer un lien dont la recette contient « 2 pommes » et « pour 6 personnes » ; la recette arrive avec 6 parts et les pommes sont comptées avec un poids estimé.

**Acceptance Scenarios**:

1. **Given** une source qui indique « pour 6 personnes », **When** la recette est extraite, **Then** le formulaire d'ajout est pré-rempli avec 6 parts.
2. **Given** un ingrédient à quantité vague, **When** la recette est extraite, **Then** son poids estimé est repris et marqué comme « estimé » dans le détail.
3. **Given** un ingrédient avec une quantité écrite précise (ex. « 200 g »), **When** la recette est extraite, **Then** c'est la quantité écrite qui fait foi, pas l'estimation du PC.
4. **Given** le PC ne fournit ni parts ni poids (ancienne version, réponse incomplète), **When** la recette est extraite, **Then** l'import se déroule comme avant et le calcul repose sur la lecture des quantités.

---

### User Story 4 - Rattrapage automatique par le PC (Priority: P3)

Sans aucune action de l'utilisatrice, les recettes qui ont des ingrédients à quantité vague non comptés (recettes existantes, imports texte ou fichier, saisies à la main) sont envoyées au PC quand il est joignable ; il renvoie un poids estimé pour ces ingrédients, et le calcul est mis à jour.

**Why this priority**: Donne aux recettes existantes et aux imports hors lien la même qualité d'estimation que les imports par lien, sans clic.

**Independent Test**: Une recette enregistrée avec « 1 pomme » non comptée ; le PC devient joignable ; peu après, la pomme est comptée avec un poids marqué « estimé ».

**Acceptance Scenarios**:

1. **Given** des recettes existantes avec des quantités vagues, **When** le PC devient joignable pour la première fois après la mise à jour, **Then** elles sont toutes traitées une fois, sans action de l'utilisatrice, et la progression est visible (« Estimation des quantités : 12 / 40 »).
2. **Given** une recette dont toutes les quantités sont lisibles, **When** le rattrapage passe, **Then** elle n'est pas envoyée au PC.
3. **Given** une nouvelle recette (texte, fichier) avec une quantité vague et le PC joignable, **When** elle est importée, **Then** les poids estimés sont demandés tout de suite et la recette arrive dans le formulaire déjà complétée.
4. **Given** la même situation mais le PC ou le Wi-Fi indisponible, **When** la recette est enregistrée, **Then** elle passe par la file d'attente et est traitée dès que le PC est joignable, sans bloquer l'enregistrement.
5. **Given** une recette déjà traitée, **When** elle est rouverte, **Then** elle n'est pas renvoyée, sauf si le texte d'un de ses ingrédients a changé (seuls les ingrédients modifiés sont re-estimés).
6. **Given** le PC injoignable ou en erreur, **When** le rattrapage échoue, **Then** il est retenté plus tard, sans message bloquant, et le calcul affiché reste celui de la lecture des quantités.
7. **Given** un poids corrigé à la main, **When** le PC renvoie une estimation pour cet ingrédient, **Then** la correction à la main est conservée.

---

### User Story 5 - Saisir et modifier le nombre de parts (Priority: P1)

Dans le formulaire d'ajout ou de modification, l'utilisatrice peut indiquer le nombre de parts (facultatif).

**Why this priority**: Nécessaire pour afficher une valeur « par part » ; très simple.

**Independent Test**: Modifier une recette, saisir 6 parts, enregistrer ; la fiche affiche la valeur par part divisée par 6.

**Acceptance Scenarios**:

1. **Given** le formulaire, **When** l'utilisatrice saisit 6 parts et enregistre, **Then** la recette est enregistrée avec 6 parts.
2. **Given** le formulaire, **When** l'utilisatrice saisit 0, une valeur négative, non numérique ou supérieure à 100, **Then** la valeur est refusée avec un message.
3. **Given** le champ laissé vide, **When** elle enregistre, **Then** la recette est enregistrée sans nombre de parts (le champ n'est pas obligatoire).

---

### User Story 6 - Aliment de remplacement (Priority: P2)

*(Ajout du 2026-11-21, tâche T046, décidé par l'utilisatrice. Numérotée 6 : le numéro 5 est déjà pris par le nombre de parts.)*

Dans le détail des calories, un ingrédient « aliment non reconnu » (ex. « 150 g de skyr ») propose « Choisir l'aliment ». L'utilisatrice cherche un aliment de la table de référence (ex. « fromage blanc 0 % ») et le choisit. Ce choix est retenu **pour toutes ses recettes**, existantes et futures : partout où le même texte apparaît, l'ingrédient est compté avec l'aliment choisi. Les choix sont consultables et annulables dans un écran « Aliments de remplacement ». La liste de courses (spec 002) s'en sert aussi pour regrouper les ingrédients.

**Why this priority**: Un aliment absent de la table rend la ligne non comptée dans toutes les recettes où il apparaît ; un seul choix les corrige toutes, sans toucher aux recettes.

**Independent Test**: Deux recettes contiennent « skyr » (non reconnu) ; dans le détail de l'une, choisir « fromage blanc » pour « skyr » ; les deux recettes comptent désormais le skyr comme du fromage blanc, la liste de courses regroupe skyr et fromage blanc, et annuler le choix ramène l'état d'avant.

**Acceptance Scenarios**:

1. **Given** une ligne « aliment non reconnu » dans le détail, **When** l'utilisatrice touche « Choisir l'aliment », **Then** une fenêtre propose le texte à reconnaître (prérempli avec le texte de l'aliment, ex. « skyr nature ») et une recherche parmi les aliments de la table.
2. **Given** la fenêtre ouverte, **When** elle saisit « fromage » puis touche « Fromage blanc… », **Then** le choix est enregistré, la fenêtre se ferme, la ligne passe dans « Comptés » avec le poids lu comme avant, l'aliment choisi et la mention « Aliment de remplacement pour « skyr nature » », et le total se met à jour.
3. **Given** un choix fait depuis une recette, **When** elle ouvre une autre recette (existante ou importée plus tard) contenant le même texte, **Then** l'ingrédient y est compté avec le même aliment, sans nouvelle saisie.
4. **Given** le texte prérempli « skyr nature », **When** elle le raccourcit en « skyr », **Then** le choix vaut pour toute ligne contenant le mot « skyr » (« 200 g de skyr », « 1 pot de skyr à la vanille »).
5. **Given** un texte à reconnaître qui ne figure pas dans la ligne (ex. « yaourt » pour « 150 g de skyr »), **When** elle touche un aliment, **Then** rien n'est enregistré et un message indique que le texte doit figurer dans l'ingrédient.
6. **Given** des choix enregistrés, **When** elle ouvre « Aliments de remplacement » (depuis le détail des calories), **Then** elle voit chaque texte et l'aliment choisi, et « Annuler » supprime le choix : les lignes concernées redeviennent « aliment non reconnu » partout, aucune recette n'étant modifiée.
7. **Given** un choix « skyr → fromage blanc », **When** le menu contient une recette avec « 200 g de skyr » et une autre avec « 100 g de fromage blanc », **Then** la liste de courses montre un seul article « fromage blanc » de 300 g.

---

### Edge Cases

- Quantité en fourchette (« 2 à 3 pommes », « 100-150 g ») : on retient la moyenne.
- Fractions et décimales (« 1/2 litre », « 1,5 kg », « ½ citron ») : lues correctement.
- Unités de volume (cl, ml, l, c. à soupe, c. à café, verre, tasse) : converties en grammes via une masse volumique par aliment, ou une valeur par défaut proche de l'eau.
- Unités « pièce » (« 3 œufs », « 1 oignon ») : converties avec un poids moyen par pièce de l'aliment ; sans poids moyen connu → non compté.
- Quantités « selon le goût », « une pincée », sel, poivre, épices : non comptés, signalés, sans bloquer le calcul (impact négligeable).
- Ingrédient « À compléter » : ignoré dans le calcul.
- Huile de friture : comptée mais signalée comme approximative (la quantité absorbée n'est pas connue).
- Ingrédient mentionné deux fois : compté deux fois, comme écrit.
- Trop de lignes non comptées, parts estimées très élevées ou valeur aberrante : avertissements définis par FR-013 et FR-017.
- Aliment de remplacement (story 6) :
  - Un aliment reconnu par la table n'est jamais remplacé : le choix ne s'applique qu'aux lignes sans aliment reconnu.
  - Plusieurs choix s'appliquent à une ligne (« skyr » et « skyr vanille ») : le texte le plus long l'emporte (même règle que les variantes de nom de la table).
  - Le texte retenu se compare mot à mot, après normalisation (minuscules, accents et pluriels retirés) : « Skyrs » = « skyr », mais « skyrella » ne contient pas « skyr ».
  - Le texte de l'unité n'est jamais pris pour l'aliment (« 2 c. à soupe » ne peut pas être retenu comme « soupe »).
  - Refaire un choix pour le même texte remplace l'ancien.
  - Aliment choisi devenu absent de la table (mise à jour de `foods.json`) : le choix est ignoré, la ligne redevient « non reconnu » ; l'écran le signale « aliment introuvable » et permet de l'annuler.
  - Choix d'un assaisonnement (sel, épices) : la ligne est ignorée comme un assaisonnement.
  - Quantités : la lecture de la quantité et de l'unité ne change pas ; seul l'aliment change. Les conversions qui dépendent de l'aliment (masse volumique pour un volume, poids d'une pièce) utilisent l'aliment choisi. Un poids corrigé à la main reste prioritaire.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Une recette MUST pouvoir porter un nombre de parts facultatif, entier de 1 à 100.
- **FR-002**: Le système MUST lire, dans chaque ligne d'ingrédient, la quantité, l'unité et l'aliment, pour les formes courantes en français (g, kg, mg, ml, cl, dl, l, c. à soupe, c. à café, pièce, fractions, décimales à virgule, fourchettes).
- **FR-003**: Le système MUST convertir chaque quantité lisible en grammes.
- **FR-004**: Le système MUST associer chaque ingrédient à un aliment d'une table de référence officielle des valeurs énergétiques pour 100 g, disponible hors ligne, couvrant au minimum les 300 aliments les plus courants en cuisine familiale française, en tolérant pluriels, accents et variantes usuelles (« oeufs », « œuf », « farine de blé »).
- **FR-005**: Le système MUST calculer le total énergétique de la recette (somme des ingrédients comptés) et, si le nombre de parts est connu, la valeur par part, arrondie à la dizaine de kcal.
- **FR-006**: La fiche de la recette MUST afficher « ≈ X kcal / part » (ou « ≈ X kcal pour la recette » quand aucun nombre de parts n'est connu ni estimable), avec une mention claire qu'il s'agit d'une estimation.
- **FR-007**: Le système MUST offrir un détail listant, pour chaque ingrédient compté, le poids retenu, sa provenance (écrit, estimé à l'import, corrigé à la main) et les kcal ; et pour chaque ingrédient non compté, la raison.
- **FR-008**: L'utilisatrice MUST pouvoir saisir ou corriger le poids en grammes d'un ingrédient ; la correction prime sur toute autre source, est conservée, et met à jour le résultat immédiatement.
- **FR-009**: Lors d'une extraction par lien, le système MUST accepter du PC un nombre de parts et un poids estimé par ingrédient, et les utiliser uniquement quand la quantité écrite n'est pas lisible.
- **FR-010**: L'absence de ces informations dans la réponse du PC MUST ne jamais faire échouer ni dégrader un import.
- **FR-011**: Les recettes existantes MUST être conservées intégralement lors de la mise à jour de l'application ; les nouvelles informations (parts, poids) sont vides pour elles.
- **FR-012**: Le calcul MUST fonctionner pour toutes les recettes, quelle que soit leur origine (saisie, texte, fichier, lien, partage), sans connexion réseau.
- **FR-013**: Le système MUST signaler une estimation partielle quand plus de la moitié des lignes d'ingrédients (hors sel, poivre et épices) ne sont pas comptées, et une valeur à vérifier au-delà de 3 000 kcal par part ou de 30 parts estimées. (Le poids d'une ligne non comptée étant inconnu, la règle porte sur le nombre de lignes.)
- **FR-014**: Le nombre de parts et les poids corrigés MUST être conservés avec la recette, au même titre que ses autres informations (survivent aux mises à jour et à la sauvegarde du téléphone).
- **FR-015**: Le détail MUST afficher aussi, par part (ou pour la recette sans nombre de parts), les protéines, glucides et lipides en grammes, arrondis au gramme, calculés sur les mêmes ingrédients comptés et selon les mêmes règles ; la fiche n'affiche que les calories.
- **FR-016**: Sans nombre de parts indiqué, le système MUST estimer le nombre de parts = poids total compté ÷ poids habituel d'une part de la catégorie, arrondi à l'entier (minimum 1), avec ce poids habituel d'une part par catégorie : Plats principaux 350 g ; Pâtes et riz 350 g ; Soupes 300 g ; Boissons 250 g ; Entrées 200 g ; Salades 200 g ; Accompagnements 200 g ; Sandwichs et wraps 200 g ; Tartes et quiches 150 g ; Petits-déjeuners 150 g ; Desserts 120 g ; Gâteaux 100 g ; Goûters 80 g ; Apéritifs 50 g ; Sauces 50 g ; Biscuits et petits gâteaux 25 g. Le nombre est affiché « ≈ N parts (estimé) ».
- **FR-017**: Le système MUST ne pas estimer le nombre de parts si plus de la moitié des lignes d'ingrédients (hors sel, poivre et épices) ne sont pas comptées, ou si la recette n'a pas de catégorie.
- **FR-018**: Ordre de priorité du nombre de parts : saisi par l'utilisatrice > indiqué par la source à l'import > estimé. Corriger une estimation l'enregistre comme saisi par l'utilisatrice.
- **FR-019**: Le système MUST envoyer automatiquement au PC, quand il est joignable, chaque recette ayant au moins un ingrédient à quantité vague non compté et pas encore estimé (rattrapage unique des recettes existantes, puis au fil de l'eau pour les recettes ajoutées ou modifiées), sans action de l'utilisatrice et sans bouton. Pour une nouvelle recette, l'estimation est faite tout de suite pendant l'import si le PC est joignable (avec l'extraction pour un lien) ; sinon la recette passe par la file d'attente. L'attente du PC ne doit jamais bloquer l'enregistrement.
- **FR-020**: Le PC MUST renvoyer, pour ces ingrédients, un poids estimé (et un nombre de parts si la recette le permet) ; seuls ces ingrédients sont mis à jour, jamais une quantité écrite, une correction à la main, ni le texte de la recette.
- **FR-021**: Un ingrédient déjà estimé MUST ne pas être renvoyé tant que son texte ne change pas.
- **FR-022**: Le rattrapage MUST être retenté plus tard en cas d'échec, sans message bloquant, et sa progression MUST être visible tant qu'il est en cours.
- **FR-023** (story 6): Le détail des calories MUST proposer « Choisir l'aliment » sur chaque ligne « aliment non reconnu », avec une recherche parmi les aliments de la table embarquée (`foods.json`), insensible à la casse et aux accents, chaque mot saisi devant commencer un mot du nom ou d'une variante.
- **FR-024** (story 6): Le choix MUST être enregistré comme une équivalence « texte normalisé → code Ciqual », le texte étant normalisé par les règles communes (`Normalize`) ; il est prérempli avec le texte de l'aliment de la ligne et peut être raccourci, à condition de s'appliquer encore à cette ligne.
- **FR-025** (story 6): Une équivalence MUST s'appliquer à toutes les recettes, existantes et futures, à toute ligne dont l'aliment n'est pas reconnu par la table et dont le texte contient le texte retenu en mots entiers ; aucune recette n'est modifiée.
- **FR-026** (story 6): La lecture de la quantité et de l'unité MUST rester inchangée ; seul l'aliment est remplacé. Le détail MUST signaler une ligne comptée grâce à une équivalence.
- **FR-027** (story 6): Un écran « Aliments de remplacement », accessible depuis le détail des calories, MUST lister les équivalences (texte, aliment) et permettre d'annuler chacune ; l'annulation prend effet partout immédiatement.
- **FR-028** (story 6, révisé le 2026-11-22 par l'utilisatrice) : dans la liste de courses (spec 002), une ligne remplacée MUST garder le nom écrit dans la recette (« skyr »), y compris pour Chronodrive ; les lignes de même texte sont additionnées entre elles, sans être fusionnées avec l'aliment choisi ; seul le rayon vient de l'aliment choisi.

### Key Entities

- **Recette** : gagne un nombre de parts (facultatif) et, par ingrédient, un poids estimé à l'import et un poids corrigé à la main (tous deux facultatifs).
- **Aliment de référence** : nom, variantes de nom, kcal, protéines, glucides et lipides pour 100 g, poids moyen d'une pièce (facultatif), masse volumique (facultative). Données en lecture seule fournies avec l'application.
- **Aliment de remplacement** (story 6) : texte d'ingrédient normalisé → code d'un aliment de référence, choisi par l'utilisatrice ; vaut pour toutes les recettes ; annulable.
- **Estimation** : calculée à l'affichage, jamais saisie : total, valeur par part (kcal, protéines, glucides, lipides), ingrédients comptés (poids, provenance, kcal), ingrédients non comptés (raison), avertissements.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Sur un jeu de 20 recettes de référence aux quantités précises, la valeur par part est à ±15 % de la valeur calculée à la main avec la table de référence pour au moins 18 d'entre elles.
- **SC-002**: Sur les recettes déjà enregistrées par l'utilisatrice, au moins 70 % des lignes d'ingrédients sont comptées sans aucune saisie avant le rattrapage, et au moins 85 % après.
- **SC-003**: Pour une recette importée par lien, au moins 85 % des lignes d'ingrédients sont comptées sans aucune saisie.
- **SC-004**: L'estimation s'affiche à l'ouverture de la fiche sans attente perceptible (moins d'une demi-seconde), hors ligne.
- **SC-005**: Corriger un poids et voir le nouveau total prend moins de 15 secondes.
- **SC-006**: Après la mise à jour de l'application, 100 % des recettes existantes sont présentes et identiques.

## Assumptions

- Une seule utilisatrice, sur son téléphone ; pas de partage ni de compte.
- Énergie (kcal), protéines, glucides et lipides sont concernés ; les autres nutriments (fibres, sel, sucres, vitamines…) sont hors périmètre de cette version.
- La table de référence est celle de l'Anses (Ciqual), sous licence ouverte, réduite aux aliments courants ; une mise à jour de la table se fait avec une nouvelle version de l'application.
- Le traitement de fond de la file d'attente existante (réveil du PC, nouvel essai, Wi-Fi de la maison) est réutilisé : après les liens en attente, il complète les poids des recettes enregistrées qui en ont besoin. Ces recettes sont mises à jour directement, sans étape « à valider » ni notification ; la progression s'affiche sur l'écran de la file.
- Les poids estimés par le PC ne servent que pour les quantités vagues. Le rattrapage des recettes existantes (environ 80, seules celles qui ont des quantités vagues) garde le PC allumé une fois, environ 5 à 15 minutes d'après les temps de réponse mesurés de Llama ; c'est accepté.
- Pas de nombre fixe de parts par défaut : sans parts indiquées, on les estime (FR-016) ; à défaut, on affiche le total de la recette.
- Les liquides qui s'évaporent à la cuisson (bouillon, vin réduit) faussent l'estimation des parts ; c'est accepté, la valeur reste modifiable.
- L'affichage se fait sur la fiche de la recette ; la liste et la recherche ne montrent pas les calories dans cette version.
- Les ingrédients sont pesés crus, tels qu'écrits ; les pertes ou gains à la cuisson ne sont pas modélisés.
