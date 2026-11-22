# Feature Specification: Menu en planning à la semaine

**Feature Branch**: `004-menu-planning-semaine`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Menu en planning à la semaine. Dans l'onglet Menu, l'utilisatrice peut choisir entre l'affichage actuel en liste et un affichage « Semaine » : une grille du lundi au dimanche avec trois repas par jour (petit-déjeuner, midi, soir), semaine type sans dates. Les deux vues montrent les mêmes recettes du menu et la liste de courses ne change pas. Elle peut placer une recette dans une case (jour + repas), la déplacer, la retirer, et ajouter une recette directement dans une case vide. Les recettes sans jour ou sans repas restent visibles. Les données existantes (menu actuel avec ses jours, recettes, liste de courses) ne doivent jamais être perdues."

## Clarifications

### Session 2026-11-22

- Q: Quels repas par jour ? → A: Trois : petit-déjeuner, midi, soir.
- Q: Semaine type ou dates précises ? → A: Semaine type (lundi à dimanche, sans dates), comme le menu actuel ; dates éventuellement plus tard.
- Q: Ordre de réalisation ? → A: Après les étapes 1 et 2 de la feature 003 (Chronodrive).
- Q: Comment choisir la recette à ajouter ? → A: En réutilisant la page Recherche de l'app (photos, recherche par titre, ingrédient ou tag) en mode « choix », à la place de la petite fenêtre actuelle (noms et catégories seulement, jugée insuffisante par l'utilisatrice).

## Contexte

Le menu de la feature 002 est une liste de recettes ; chaque entrée a un nombre de parts (ou un nombre de fois la recette) et un jour facultatif (lundi à dimanche), et la liste est groupée par jour puis « Sans jour ». Cette fonctionnalité ajoute le repas (facultatif) à chaque entrée et une seconde façon d'afficher le même menu.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Voir mon menu en semaine (Priority: P1)

En haut de l'onglet Menu, un sélecteur « Liste / Semaine » permet de passer d'un affichage à l'autre. La vue Semaine montre une grille : une ligne par jour (lundi à dimanche) et trois cases par jour (petit-déjeuner, midi, soir), avec dans chaque case le titre des recettes prévues. Les recettes sans jour sont regroupées sous la grille dans « À placer » ; celles qui ont un jour mais pas de repas apparaissent sur la ligne de leur jour, dans « Sans repas ».

**Why this priority**: C'est la demande : voir sa semaine d'un coup d'œil.

**Independent Test**: Avec au menu « Quiche » (lundi, midi), « Soupe » (lundi, soir), « Crêpes » (mardi, sans repas) et « Salade » (sans jour) : la vue Semaine montre Quiche dans lundi/midi, Soupe dans lundi/soir, Crêpes sur la ligne mardi en « Sans repas », Salade dans « À placer ». La vue Liste montre les mêmes 4 recettes.

**Acceptance Scenarios**:

1. **Given** un menu existant, **When** l'utilisatrice touche « Semaine », **Then** la grille s'affiche avec toutes les recettes du menu, aucune n'est absente.
2. **Given** les entrées créées avant cette version (jour sans repas), **When** elle ouvre la vue Semaine, **Then** elles apparaissent sur la ligne de leur jour en « Sans repas », ou dans « À placer » si elles n'ont pas de jour.
3. **Given** une case avec plusieurs recettes (soir : plat et dessert), **When** la grille s'affiche, **Then** toutes les recettes de la case sont visibles (ou le nombre de recettes si la case est trop petite, avec accès à la liste en la touchant).
4. **Given** l'utilisatrice a choisi une vue, **When** elle revient plus tard dans l'onglet Menu, **Then** la dernière vue choisie s'affiche.
5. **Given** la vue Liste, **When** une entrée a un repas, **Then** il est indiqué à côté du jour (« Lundi · Midi »), et dans un même jour les entrées sont triées petit-déjeuner, midi, soir, puis sans repas.

---

### User Story 2 - Placer, déplacer, retirer une recette (Priority: P1)

Dans la vue Semaine, l'utilisatrice touche une recette pour la placer ou la déplacer (choix du jour et du repas, ou « Sans jour »), changer ses parts, ouvrir la recette ou la retirer du menu. Elle touche une case vide pour y ajouter une recette de sa bibliothèque : la page Recherche de l'app s'ouvre en mode « choix » (voir User Story 5).

**Why this priority**: Une grille qu'on ne peut pas remplir ne sert à rien.

**Independent Test**: Toucher la case mercredi/soir vide, choisir « Quiche » : elle apparaît dans mercredi/soir et dans la liste de courses. La déplacer vers jeudi/midi : elle n'est plus dans mercredi/soir. La retirer : elle disparaît du menu et de la liste de courses.

**Acceptance Scenarios**:

1. **Given** une case vide, **When** l'utilisatrice la touche et choisit une recette, **Then** la recette est ajoutée au menu dans ce jour et ce repas, avec le nombre de parts par défaut (comme un ajout normal).
2. **Given** une case occupée, **When** elle touche « + » dans cette case, **Then** elle peut y ajouter une autre recette.
3. **Given** une recette dans la grille, **When** elle la touche et choisit un autre jour et repas, **Then** la recette change de case ; ses parts ne changent pas.
4. **Given** une recette dans « À placer », **When** elle lui choisit un jour et un repas, **Then** elle passe dans la grille.
5. **Given** une recette dans la grille, **When** elle choisit « Sans jour », **Then** elle passe dans « À placer » sans quitter le menu.
6. **Given** une recette dans la grille, **When** elle choisit « Retirer du menu », **Then** elle disparaît du menu (même comportement que dans la vue Liste).
7. **Given** la vue Liste, **When** elle change le jour d'une entrée, **Then** elle peut aussi choisir son repas (ou aucun) ; chaque choix s'applique immédiatement.

---

### User Story 3 - Liste de courses inchangée (Priority: P1)

La liste de courses reste calculée à partir de toutes les recettes du menu, qu'elles soient placées ou non, quelle que soit la vue.

**Why this priority**: La liste de courses est le but final ; le planning ne doit pas la casser.

**Independent Test**: Le même menu donne exactement la même liste de courses avant et après avoir placé ses recettes dans la grille, et en vue Liste ou Semaine.

**Acceptance Scenarios**:

1. **Given** un menu, **When** l'utilisatrice place, déplace ou passe en « Sans jour » des recettes, **Then** la liste de courses ne change pas.
2. **Given** les coches et articles ajoutés à la main, **When** elle change de vue ou déplace des recettes, **Then** ils sont conservés.

---

### User Story 4 - Vider la semaine (Priority: P2)

Comme aujourd'hui, « Vider le menu » retire toutes les recettes après confirmation. En plus, « Retirer les jours » remet toutes les recettes dans « À placer » sans les retirer du menu, pour réorganiser une nouvelle semaine avec les mêmes recettes.

**Why this priority**: Utile pour repartir chaque semaine, mais le vidage actuel existe déjà.

**Independent Test**: Avec 5 recettes placées, « Retirer les jours » puis confirmer : les 5 recettes sont dans « À placer », la liste de courses est inchangée.

**Acceptance Scenarios**:

1. **Given** des recettes placées, **When** l'utilisatrice choisit « Retirer les jours » et confirme, **Then** toutes passent sans jour ni repas, et restent au menu.
2. **Given** le menu, **When** elle choisit « Vider le menu » et confirme, **Then** le comportement est celui de la feature 002 (recettes et coches retirées, articles ajoutés à la main conservés).

---

### User Story 5 - Choisir la recette avec la page Recherche (Priority: P1)

Pour ajouter une recette au menu (« + » de la vue Liste ou case de la vue Semaine), l'app ouvre sa page Recherche en mode « choix » : mêmes photos, même recherche par titre, ingrédient ou tag, mêmes suggestions qu'aujourd'hui, avec en haut un bandeau « Choisis une recette pour le menu » (ou « … pour Mercredi · Soir ») et « Annuler ». Toucher une recette l'ajoute et ramène au menu. La petite fenêtre actuelle (noms et catégories) est remplacée.

**Why this priority**: Choisir sans voir les photos est peu pratique ; c'est le geste le plus fréquent du planning.

**Independent Test**: Toucher la case mercredi/soir vide : la page Recherche s'ouvre avec le bandeau « Choisis une recette pour Mercredi · Soir » et les photos ; taper « quiche », toucher « Quiche lorraine » : retour à la vue Semaine, la quiche est dans mercredi/soir. « Annuler » ramène au menu sans rien ajouter.

**Acceptance Scenarios**:

1. **Given** le mode « choix », **When** l'utilisatrice touche une recette, **Then** elle est ajoutée au menu (dans la case visée s'il y en a une) et l'app revient au menu ; elle n'ouvre pas la fiche de la recette.
2. **Given** le mode « choix », **When** elle touche « Annuler » ou Retour, **Then** rien n'est ajouté et l'app revient au menu.
3. **Given** l'onglet Recherche ouvert normalement depuis la barre du bas, **When** elle touche une recette, **Then** la fiche s'ouvre comme aujourd'hui (le mode « choix » ne change rien à l'usage habituel).
4. **Given** le mode « choix », **When** la recette est déjà au menu, **Then** elle est signalée (« déjà au menu ») mais peut être ajoutée une seconde fois.

---

### Edge Cases

- Écran étroit : la grille tient en largeur sur le téléphone (3 colonnes de repas), les titres longs sont abrégés ; le titre complet s'affiche en touchant la recette.
- Semaine vide : la grille vide s'affiche avec un message « Touche une case pour y ajouter une recette ».
- Beaucoup de recettes dans une case (plus de 2) : la case montre les 2 premières et « +N ».
- Même recette deux fois dans la semaine : deux entrées distinctes, chacune dans sa case, toutes deux comptées dans la liste de courses (feature 002).
- Recette supprimée de la bibliothèque : l'avertissement et le retrait du menu de la feature 002 s'appliquent ; elle disparaît de la grille.
- Retour arrière ou rotation de l'écran : la vue et la position restent les mêmes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: L'onglet Menu DOIT proposer deux vues, « Liste » et « Semaine », sur les mêmes entrées de menu ; la dernière vue choisie DOIT être retrouvée au retour.
- **FR-002**: Chaque entrée de menu DOIT pouvoir recevoir un repas facultatif parmi petit-déjeuner, midi, soir ; le jour reste facultatif ; un repas sans jour n'est pas permis (choisir « Sans jour » efface aussi le repas).
- **FR-003**: La vue Semaine DOIT afficher une grille lundi à dimanche × petit-déjeuner, midi, soir ; les entrées avec jour sans repas sur la ligne de leur jour en « Sans repas » ; les entrées sans jour dans « À placer ».
- **FR-004**: L'utilisatrice DOIT pouvoir, depuis la vue Semaine : ajouter une recette dans une case (vide ou non), déplacer une entrée vers une autre case ou « Sans jour », changer ses parts, ouvrir la recette, retirer l'entrée du menu.
- **FR-005**: La vue Liste DOIT indiquer le repas à côté du jour et trier un même jour par petit-déjeuner, midi, soir, puis sans repas ; le choix du jour DOIT permettre aussi le choix du repas.
- **FR-006**: La liste de courses NE DOIT PAS dépendre du placement (jour, repas) ni de la vue.
- **FR-007**: « Retirer les jours » DOIT, après confirmation, effacer jour et repas de toutes les entrées sans les retirer du menu.
- **FR-007b**: L'ajout d'une recette au menu (vue Liste ou Semaine) DOIT passer par la page Recherche de l'app en mode « choix » (photos, recherche par titre, ingrédient ou tag) ; toucher une recette l'ajoute et revient au menu ; « Annuler » ne change rien ; l'usage normal de l'onglet Recherche reste inchangé.
- **FR-008**: Les données existantes (entrées de menu avec leurs parts et jours, recettes, coches, articles ajoutés à la main, réglages) NE DOIVENT PAS être modifiées ni perdues par la mise à jour ; les entrées existantes n'ont pas de repas après la mise à jour.

### Key Entities

- **Entrée de menu** (existante, feature 002) : recette, parts ou nombre de fois, jour facultatif ; **nouveau** : repas facultatif (petit-déjeuner, midi, soir).
- **Préférence de vue** : dernière vue choisie (Liste ou Semaine).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: L'utilisatrice voit toute sa semaine (21 cases + « À placer ») sur un seul écran de téléphone sans défiler horizontalement.
- **SC-002**: Placer une recette dans une case vide prend au plus 3 gestes (case, recherche, recette), en voyant la photo des recettes proposées.
- **SC-003**: Déplacer une recette vers une autre case prend au plus 4 gestes (recette, jour, repas, valider).
- **SC-004**: Après la mise à jour, 100 % des entrées de menu existantes sont présentes, avec les mêmes parts et jours.
- **SC-005**: Pour un même menu, la liste de courses est identique en vue Liste et en vue Semaine, avant et après placement des recettes.

## Assumptions

- Semaine type sans dates : la semaine ne « tourne » pas toute seule ; l'utilisatrice vide ou réorganise quand elle veut (US4).
- Pas de glisser-déposer obligatoire : le déplacement se fait en touchant la recette puis en choisissant la case (plus fiable sur téléphone) ; le glisser-déposer peut être ajouté plus tard.
- Pas de collations ni d'autres repas que les trois demandés.
- Une seule utilisatrice, un seul téléphone ; pas de partage du planning.
- À réaliser après les étapes 1 et 2 de la feature 003 (Chronodrive).
