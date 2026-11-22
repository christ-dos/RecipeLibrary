# Feature Specification: Menus et liste de courses

**Feature Branch**: `002-menus-liste-courses`

**Created**: 2026-11-21

**Status**: Draft

**Input**: User description: "Planifier les menus et générer la liste de courses. L'utilisatrice choisit parmi ses recettes enregistrées celles qu'elle veut cuisiner (planning de menus, par exemple sur la semaine), puis l'app produit la liste des ingrédients à acheter pour réaliser toutes ces recettes, afin de passer commande. Les données existantes ne doivent jamais être perdues."

## Clarifications

### Session 2026-11-21

- Q: Le menu est-il un calendrier (jour × repas) ou une simple liste ? → A: Une liste de recettes à cuisiner, avec un jour de la semaine facultatif par entrée ; affichage groupé par jour puis « Sans jour ».
- Q: Comment l'utilisatrice passe-t-elle commande ? → A: Dans l'application Chronodrive. Chronodrive n'ouvre pas son panier aux applications tierces (seulement à des partenaires comme Jow). Testé sur le téléphone le 2026-11-21 : un lien de recherche ouvre l'application Chronodrive sur une page d'erreur, et dans Chrome il impose le choix du magasin. Retenu : le bouton « Chronodrive » copie le nom du produit et ouvre l'application Chronodrive (magasin et panier déjà en place) ; l'utilisatrice colle le nom dans la recherche. Le partage de la liste en texte est conservé.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choisir les recettes à cuisiner (Priority: P1)

L'utilisatrice prépare son menu : elle ajoute des recettes de sa bibliothèque à son menu (depuis la fiche d'une recette ou depuis un écran « Menu » où elle cherche parmi ses recettes), indique pour chacune le nombre de parts qu'elle veut préparer, puis retrouve ce menu à tout moment. Chaque entrée peut, si elle le souhaite, recevoir un jour de la semaine (lundi à dimanche) ; le menu est alors trié par jour, les entrées sans jour venant à la fin sous « Sans jour ».

**Why this priority**: Sans menu, pas de liste de courses. C'est aussi utile seul : l'utilisatrice sait ce qu'elle a prévu de cuisiner.

**Independent Test**: Ajouter « Pâte à crêpes » (4 parts) et « Quiche lorraine » (6 parts) au menu, fermer et rouvrir l'app : les deux recettes sont toujours au menu avec leur nombre de parts.

**Acceptance Scenarios**:

1. **Given** une recette ouverte, **When** l'utilisatrice touche « Ajouter au menu », **Then** la recette apparaît dans le menu avec, par défaut, le nombre de parts de la recette (ou celui estimé).
2. **Given** l'écran Menu, **When** l'utilisatrice cherche « quiche » et choisit une recette, **Then** elle est ajoutée au menu.
3. **Given** une recette au menu, **When** l'utilisatrice change son nombre de parts ou la retire du menu, **Then** le menu est mis à jour immédiatement, et la recette elle-même n'est pas modifiée.
4. **Given** une même recette prévue deux fois (par exemple deux soirs), **When** l'utilisatrice l'ajoute une seconde fois, **Then** les deux entrées coexistent et comptent toutes les deux dans la liste de courses.
5. **Given** des entrées avec et sans jour, **When** l'utilisatrice ouvre le menu, **Then** elles sont groupées de lundi à dimanche, puis « Sans jour » ; elle peut changer ou enlever le jour d'une entrée.
6. **Given** une recette au menu, **When** l'utilisatrice supprime la recette de sa bibliothèque, **Then** elle est prévenue qu'elle est au menu, et l'entrée disparaît du menu si elle confirme.

---

### User Story 2 - Obtenir la liste de courses du menu (Priority: P1)

À partir du menu, l'utilisatrice génère la liste des ingrédients à acheter. Les quantités sont ajustées au nombre de parts prévu et les ingrédients identiques sont regroupés (deux recettes avec « 200 g de farine » et « 250 g de farine » donnent « farine : 450 g »).

**Why this priority**: C'est le but final exprimé par l'utilisatrice : savoir quoi acheter pour réaliser le menu.

**Independent Test**: Avec au menu une pâte à crêpes (recette de 4 parts, prévue pour 8) contenant « 250 g de farine, 4 œufs » et un gâteau contenant « 200 g de farine, 3 œufs », la liste affiche « farine : 700 g » et « œufs : 11 ».

**Acceptance Scenarios**:

1. **Given** un menu de plusieurs recettes, **When** l'utilisatrice ouvre la liste de courses, **Then** chaque ingrédient apparaît une seule fois, avec la somme des quantités de toutes les recettes.
2. **Given** une recette de 4 parts prévue pour 8 parts, **When** la liste est générée, **Then** ses quantités sont multipliées par 2.
3. **Given** deux lignes du même aliment dans des unités convertibles (« 50 cl de lait » et « 200 ml de lait »), **When** la liste est générée, **Then** elles sont additionnées dans une seule unité (« lait : 70 cl »).
4. **Given** deux lignes du même aliment dans des unités non convertibles (« 1 boîte de tomates » et « 400 g de tomates »), **When** la liste est générée, **Then** l'aliment apparaît une seule fois avec les deux quantités côte à côte (« tomates : 400 g + 1 boîte »).
5. **Given** une ligne dont l'aliment n'est pas reconnu ou sans quantité (« sel », « un peu de persil »), **When** la liste est générée, **Then** elle apparaît telle qu'écrite, sans être perdue.
6. **Given** un ingrédient de la liste, **When** l'utilisatrice veut savoir d'où il vient, **Then** elle peut voir les recettes du menu qui l'utilisent.

---

### User Story 3 - Préparer la commande (Priority: P2)

L'utilisatrice parcourt la liste, coche ce qu'elle a déjà chez elle, ajoute à la main des articles hors recettes (« papier toilette »), puis passe commande dans l'application Chronodrive : pour chaque article, le bouton « Chronodrive » copie le nom du produit et ouvre l'application Chronodrive ; elle touche la recherche, colle le nom, ajoute le produit à son panier, revient et coche l'article. Elle peut aussi partager la liste en texte vers l'application de son choix. L'app ne remplit pas le panier Chronodrive elle-même et ne se connecte pas à son compte.

**Why this priority**: Rend la liste réellement utilisable pour commander, mais la liste seule (story 2) apporte déjà l'essentiel.

**Independent Test**: Cocher « œufs », ajouter « papier toilette », partager la liste : le texte partagé contient la farine et le papier toilette, pas les œufs. Toucher « Chronodrive » sur « farine » copie « farine » et ouvre l'application Chronodrive.

**Acceptance Scenarios**:

1. **Given** la liste de courses, **When** l'utilisatrice coche un article (déjà à la maison ou mis au panier), **Then** il passe en bas de la liste, barré, et n'est pas partagé ; elle peut le décocher.
2. **Given** la liste de courses, **When** l'utilisatrice ajoute un article à la main, **Then** il apparaît dans la liste et reste même si le menu change.
3. **Given** la liste de courses, **When** l'utilisatrice touche « Partager », **Then** l'app produit un texte lisible (un article par ligne, avec quantité) qu'elle peut envoyer à l'application de son choix.
4. **Given** un article de la liste, **When** l'utilisatrice touche « Chronodrive », **Then** le nom de l'aliment, sans quantité ni mots inutiles (« farine », pas « 450 g de farine de blé T55 bio »), est copié, un message court le confirme (« « farine » copié, colle-le dans la recherche Chronodrive »), et l'application Chronodrive s'ouvre sur son accueil ; si elle n'est pas installée, la recherche Chronodrive s'ouvre dans le navigateur.
5. **Given** l'utilisatrice revient dans l'app après être passée par Chronodrive, **When** la liste réapparaît, **Then** elle est au même endroit et l'article cherché est mis en évidence pour être coché d'un geste.
6. **Given** une liste déjà en partie cochée, **When** l'utilisatrice ajoute une recette au menu, **Then** les articles cochés restent cochés et les nouveaux ingrédients s'ajoutent.

---

### User Story 4 - Liste rangée par rayon (Priority: P3)

Les articles de la liste sont regroupés par rayon (fruits et légumes, crèmerie, épicerie, boucherie et poisson, surgelés, boissons, autres) pour faire ou vérifier les courses plus vite.

**Why this priority**: Confort ; la liste reste utilisable sans ce classement.

**Independent Test**: Une liste contenant farine, lait, pommes et poulet affiche quatre rayons différents, chacun avec son article.

**Acceptance Scenarios**:

1. **Given** une liste de courses, **When** elle s'affiche, **Then** les articles reconnus sont groupés par rayon, et les articles non reconnus ou ajoutés à la main sont dans « Autres ».
2. **Given** une liste partagée, **When** l'utilisatrice la partage, **Then** le texte reprend les mêmes rayons comme titres.

---

### Edge Cases

- Menu vide : la liste de courses indique qu'il faut d'abord ajouter des recettes, sans erreur.
- Recette sans nombre de parts ni estimation possible : elle est ajoutée pour « × 1 recette » ; ses quantités sont multipliées par ce nombre de fois (×1 à ×10). Le lien « Indiquer les parts de la recette » fait passer l'entrée en parts.
- Recette modifiée après son ajout au menu (ingrédients changés) : la liste suit la version à jour de la recette.
- Quantités à fraction ou à intervalle (« 1/2 citron », « 2 à 3 pommes ») : elles sont additionnées comme le fait le calcul des calories (moyenne pour un intervalle) et affichées de façon lisible (« 1,5 citron » → « 2 citrons » arrondi au supérieur pour les pièces).
- Lignes de titre dans les ingrédients (« Pour la pâte : ») : elles n'apparaissent pas dans la liste.
- Sel, poivre, épices : listés une seule fois, sans quantité additionnée, pour que l'utilisatrice vérifie qu'elle en a.
- Très grand menu (30 recettes) : la liste se génère sans attente perceptible.
- Le même aliment écrit de deux façons (« œufs » et « oeuf », « tomate » et « tomates ») : regroupé en un seul article.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: L'utilisatrice MUST pouvoir ajouter une recette de sa bibliothèque au menu depuis la fiche de la recette et depuis l'écran Menu (avec recherche par titre).
- **FR-002**: Chaque entrée du menu MUST avoir un nombre de parts prévu (1 à 100), initialisé au nombre de parts de la recette (connu ou estimé), modifiable sans changer la recette. Si la recette n'a ni parts ni estimation, l'entrée se règle en nombre de fois la recette (×1 à ×10, ×1 par défaut), et un lien « Indiquer les parts de la recette » permet de les saisir (enregistrées dans la recette, origine « saisi ») ; l'entrée passe alors en parts.
- **FR-002a**: Chaque entrée du menu MUST pouvoir recevoir un jour de la semaine facultatif (lundi à dimanche), modifiable et retirable ; le menu MUST être affiché groupé par jour dans l'ordre de la semaine, puis les entrées « Sans jour ».
- **FR-003**: L'utilisatrice MUST pouvoir retirer une entrée du menu et vider tout le menu (avec confirmation pour vider).
- **FR-004**: Le menu MUST être conservé entre les ouvertures de l'app et ne contenir que des références aux recettes : aucune recette n'est copiée, modifiée ou supprimée par le menu.
- **FR-005**: La suppression d'une recette présente au menu MUST prévenir l'utilisatrice, puis retirer les entrées correspondantes du menu.
- **FR-006**: La liste de courses MUST être calculée à partir des ingrédients à jour de chaque recette du menu, quantités multipliées par (parts prévues ÷ parts de la recette).
- **FR-007**: Les lignes désignant le même aliment MUST être regroupées en un seul article, en reconnaissant l'aliment de la même manière que le calcul des calories (pluriels, accents et variantes d'écriture compris).
- **FR-008**: Les quantités d'un même aliment MUST être additionnées quand les unités sont convertibles (masses entre elles, volumes entre eux, pièces entre elles) et affichées dans une unité lisible (g ou kg, cl ou l, nombre de pièces).
- **FR-009**: Quand les unités ne sont pas convertibles, l'article MUST afficher les quantités côte à côte plutôt que d'en perdre une.
- **FR-010**: Une ligne non reconnue ou sans quantité MUST apparaître dans la liste telle qu'écrite ; aucune ligne d'ingrédient du menu (hors titres) ne doit disparaître de la liste.
- **FR-011**: Pour chaque article, l'utilisatrice MUST pouvoir voir les recettes du menu qui l'utilisent.
- **FR-012**: L'utilisatrice MUST pouvoir cocher un article (déjà à la maison ou mis au panier) et le décocher ; les articles cochés ne sont pas partagés.
- **FR-013**: L'utilisatrice MUST pouvoir ajouter, modifier et supprimer des articles à la main ; ils sont conservés quand le menu change.
- **FR-014**: L'état coché d'un article MUST être conservé quand la liste est recalculée (ajout ou retrait d'une recette), tant que l'article existe encore.
- **FR-015**: L'utilisatrice MUST pouvoir partager la liste (articles non cochés) sous forme de texte, un article par ligne avec sa quantité, vers l'application de son choix.
- **FR-015a**: Chaque article non coché MUST proposer « Chronodrive », qui copie le nom de l'aliment (nom court de l'aliment reconnu, ou texte de la ligne sans quantité ni unité si l'aliment n'est pas reconnu) et ouvre l'application Chronodrive ; si elle n'est pas installée, ouvre la recherche Chronodrive dans le navigateur.
- **FR-015b**: L'app MUST NOT demander ni conserver d'identifiant ou de mot de passe Chronodrive, ni agir sur le compte ou le panier Chronodrive.
- **FR-016**: Les articles reconnus MUST être classés par rayon ; les autres vont dans « Autres » (story 4).
- **FR-016b** (ajout du 2026-11-22, décidé par l'utilisatrice) : une coche « déjà à la maison ou dans le panier » d'un article calculé MUST s'effacer au bout de 3 jours (l'article revient dans la liste) ; ajouter une recette ne décoche rien (farine, condiments… restent cochés) ; décocher remet l'article dans la liste avec son bouton Chrono. Les articles ajoutés à la main ne sont pas concernés.
- **FR-017**: La mise en place de la fonctionnalité MUST laisser intactes toutes les recettes existantes et les réglages ; une recette ne doit jamais être modifiée par une action faite dans le menu ou la liste, sauf la saisie volontaire de ses parts par « Indiquer les parts de la recette » (FR-002).

### Key Entities

- **Entrée du menu**: une recette choisie, avec le nombre de parts prévu (ou un nombre de fois la recette si elle n'a pas de parts) et un ordre d'affichage et un jour de la semaine facultatif. Plusieurs entrées peuvent viser la même recette.
- **Article de la liste de courses**: un aliment (ou une ligne non reconnue) avec ses quantités additionnées, son rayon, les recettes d'origine et son état coché. Calculé depuis le menu, sauf les articles ajoutés à la main.
- **Article ajouté à la main**: un texte libre (la quantité s'écrit dans le texte, ex. « 2 papier toilette »), conservé indépendamment du menu.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: L'utilisatrice compose un menu de 5 recettes et obtient sa liste de courses en moins de 2 minutes.
- **SC-002**: Sur un jeu de 10 menus de référence, 100 % des lignes d'ingrédients (hors titres) se retrouvent dans la liste, et au moins 90 % des aliments présents dans plusieurs recettes sont regroupés en un seul article avec la bonne somme.
- **SC-003**: La liste d'un menu de 30 recettes s'affiche en moins d'1 seconde.
- **SC-004**: Après installation de la fonctionnalité, les recettes enregistrées sont identiques à avant (même nombre, même contenu).
- **SC-005**: Le texte partagé est lisible tel quel dans une application de messagerie ou de notes, sans retouche, pour 100 % des listes de référence.
- **SC-006**: Pour 90 % des articles reconnus, le nom copié par l'app, collé dans la recherche Chronodrive, montre le bon type de produit dans les 5 premiers résultats.

## Assumptions

- Une seule utilisatrice et un seul menu actif à la fois ; pas de synchronisation entre appareils ni de partage du menu avec d'autres personnes.
- La reconnaissance des aliments, des quantités et des unités réutilise celle de la fonctionnalité « Calories par part » (même table d'aliments, mêmes règles), et fonctionne hors ligne.
- Le nombre de parts d'une recette vient de la fonctionnalité « Calories par part » (indiqué par la source, saisi, ou estimé).
- Les conversions se limitent aux unités de même nature (masse, volume, pièces) ; aucune conversion volume ↔ masse ni pièce ↔ masse n'est faite dans la liste.
- Pas de gestion de stock (garde-manger) au-delà de la case à cocher pour la liste en cours.
- Pas de prix, de budget ni de suggestion automatique de recettes dans cette version.
- Le magasin est Chronodrive. Aucun accès public ne permet à une application tierce de remplir le panier Chronodrive, et l'application Chronodrive n'affiche pas les résultats d'un lien de recherche (testé le 2026-11-21) : la commande se fait dans l'application Chronodrive, avec le nom du produit copié par l'app. Si l'application Chronodrive accepte un jour ces liens, le bouton pourra ouvrir directement les résultats.
- D'autres magasins pourront être ajoutés plus tard sur le même principe (adresse de recherche).
- Le jour d'une entrée est un jour de la semaine sans date précise ; pas de repas (midi, soir) ni d'historique des semaines passées.
