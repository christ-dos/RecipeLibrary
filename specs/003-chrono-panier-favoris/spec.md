# Feature Specification: Mes produits Chronodrive (fiche directe, puis ajout au panier)

**Feature Branch**: `003-chrono-panier-favoris`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Ajouter au panier Chronodrive depuis les favoris. Depuis la liste de courses, le bouton Chrono d'un article doit retrouver le produit dans les favoris Chronodrive de l'utilisatrice, ouvrir sa fiche, voire l'ajouter au panier. Les données existantes ne doivent jamais être perdues."

## Clarifications

### Session 2026-11-22

- Q: Comment l'app trouve-t-elle le produit dans les favoris ? → A: Pas en lisant l'écran de Chronodrive : l'utilisatrice refuse le service d'accessibilité. Ses favoris (nom + numéro de produit) sont relevés depuis son compte sur chronodrive.com (elle se connecte elle-même dans Chrome sur le PC) et importés dans l'app ; l'app choisit le produit dans ses propres données.
- Q: Que fait « Chrono » quand le produit est connu ? → A: En deux temps, validés par l'utilisatrice : (1) d'abord, ouvrir directement la fiche du produit dans l'application Chronodrive (elle appuie sur « + ») ; (2) ensuite, en plus, ajout direct au panier par l'app via son compte Chronodrive, avec la fiche en repli.
- Q: Comment se connecter au compte Chronodrive ? → A: Depuis l'app, un bouton ouvre la page de connexion de Chronodrive (elle saisit ses identifiants elle-même) ; si la connexion a expiré, « Panier » propose de se reconnecter.
- Q: Comment tenir la liste des favoris à jour ? → A: Un bouton « Mettre à jour mes favoris » relit les favoris sur le compte Chronodrive (connexion de US4) ; sans connexion, import d'un fichier.
- Q: Fiche et ajout au panier : un seul bouton ou deux ? → A: Deux boutons distincts sur la ligne de l'article : « Fiche » (ouvre la fiche dans Chronodrive) et « Panier » (ajoute directement, US4).

## Contexte

Constaté sur le téléphone et sur chronodrive.com le 2026-11-22 :

- L'application Chronodrive n'accepte aucun lien de recherche, mais ouvre la fiche d'un produit par son numéro (`chronodrive://product/<numéro>` : testé avec le citron n° 122974) et ouvre les favoris (`chronodrive://favorites`).
- Les favoris de l'utilisatrice comptent 125 produits ; 120 ont été relevés avec leur numéro et leur nom (rayons Fruits & Légumes, Viandes, Charcuterie, Produits laitiers, Surgelés, Épicerie salée et sucrée, Boissons, Hygiène, Entretien, Bébé). Exemples : « banane » n° 365920 et n° 305254 (deux produits), « Lactel Matin Léger lait sans lactose » n° 55785.
- Une application ne peut pas lire l'écran d'une autre ni y appuyer sans service d'accessibilité ; l'utilisatrice ne veut pas l'activer.

Cette fonctionnalité prolonge la User Story 3 de la feature 002 (bouton « Chrono » de la liste de courses).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Importer mes favoris Chronodrive (Priority: P1)

L'utilisatrice importe dans l'app la liste de ses favoris Chronodrive (fichier préparé depuis son compte chronodrive.com) : chaque produit avec son nom, sa marque, son rayon et son numéro. Elle peut la réimporter plus tard pour ajouter ses nouveaux favoris.

**Why this priority**: Sans la liste de ses produits, l'app ne peut rien ouvrir directement.

**Independent Test**: Importer le fichier de 120 favoris : l'app affiche « 120 produits Chronodrive importés » et la liste « Mes produits Chronodrive » les montre par rayon. Réimporter un fichier avec 3 produits de plus : « 3 ajoutés, 120 déjà connus », rien n'est perdu.

**Acceptance Scenarios**:

1. **Given** un fichier de favoris valide, **When** l'utilisatrice l'importe, **Then** l'app enregistre chaque produit (numéro, nom, marque, rayon) et affiche le nombre importé.
2. **Given** des produits déjà importés et des choix déjà faits (US2), **When** elle réimporte, **Then** les nouveaux produits sont ajoutés, ceux déjà connus sont mis à jour (nom), et aucun choix n'est perdu ; un produit absent du nouveau fichier n'est pas supprimé automatiquement mais marqué « plus en favori ».
3. **Given** un fichier invalide ou vide, **When** elle l'importe, **Then** rien n'est modifié et un message l'explique.
4. (US4, phase ultérieure) **Given** l'écran « Mes produits Chronodrive », **When** l'utilisatrice touche « Mettre à jour mes favoris » en étant connectée à son compte Chronodrive dans l'app (voir US4), **Then** l'app relit ses favoris sur son compte et les fusionne comme un réimport (scénario 2) : « 3 ajoutés, 120 déjà connus ».
5. (US4, phase ultérieure) **Given** l'utilisatrice non connectée, **When** elle touche « Mettre à jour mes favoris », **Then** l'app propose « Se connecter à Chronodrive » (même page qu'en US4) ou « Importer un fichier ».

---

### User Story 2 - Chrono ouvre directement la fiche de mon produit (Priority: P1)

Dans l'onglet Courses, « Chrono » à côté de « banane » ouvre directement, dans l'application Chronodrive, la fiche du produit retenu pour « banane » ; l'utilisatrice appuie sur « + », revient, et coche l'article (comme aujourd'hui). La première fois, l'app propose les produits de ses favoris qui correspondent à l'article ; s'il n'y en a qu'un, il est retenu ; s'il y en a plusieurs, elle choisit son habituel, une fois pour toutes.

**Why this priority**: C'est le gain demandé : ne plus chercher ses produits habituels.

**Independent Test**: Avec les favoris importés et « banane » dans la liste : toucher « Chrono » propose les deux bananes (n° 365920 et 305254) ; l'utilisatrice en choisit une ; la fiche de ce produit s'ouvre dans Chronodrive. Toucher « Chrono » à nouveau sur « banane » (ou dans une liste ultérieure) ouvre directement la même fiche, sans question.

**Acceptance Scenarios**:

1. **Given** un article dont le produit est déjà retenu, **When** l'utilisatrice touche « Chrono », **Then** la fiche de ce produit s'ouvre dans l'application Chronodrive, et l'article est mis en évidence au retour pour être coché (comportement de la feature 002).
2. **Given** un article sans produit retenu et un seul favori correspondant, **When** elle touche « Chrono », **Then** ce produit est retenu pour cet article et sa fiche s'ouvre.
3. **Given** un article sans produit retenu et plusieurs favoris correspondants, **When** elle touche « Chrono », **Then** l'app affiche ces produits (nom, marque) ; celui qu'elle touche est retenu et sa fiche s'ouvre ; « Aucun de ceux-là » ouvre les favoris comme aujourd'hui.
4. **Given** un article sans favori correspondant, **When** elle touche « Chrono », **Then** le comportement actuel s'applique (nom copié, favoris ouverts).
5. **Given** un produit retenu, **When** la fiche ne s'ouvre pas (application Chronodrive absente ou lien refusé), **Then** l'app ouvre la fiche du produit sur le site dans le navigateur, sinon le comportement actuel.

---

### User Story 3 - Voir et corriger mes choix (Priority: P2)

Un écran « Mes produits Chronodrive » montre les correspondances « article → produit » : l'utilisatrice peut changer le produit d'un article, l'oublier (l'app redemandera), ou en attribuer un à la main depuis la liste de ses favoris importés.

**Why this priority**: Un mauvais choix doit pouvoir se corriger, mais l'usage courant (US2) fonctionne sans cet écran.

**Independent Test**: Changer « banane » du n° 365920 au n° 305254 : le prochain « Chrono » sur « banane » ouvre la fiche du n° 305254. Oublier « banane » : le prochain « Chrono » repropose les deux bananes.

**Acceptance Scenarios**:

1. **Given** des choix enregistrés, **When** l'utilisatrice ouvre « Mes produits Chronodrive », **Then** elle voit chaque article avec le produit retenu, et la liste de ses favoris par rayon.
2. **Given** un choix, **When** elle le change ou l'oublie, **Then** le prochain « Chrono » en tient compte.
3. **Given** un article (depuis l'onglet Courses, appui long), **When** elle choisit « Choisir mon produit Chronodrive », **Then** elle peut retenir n'importe lequel de ses favoris importés.

---

### User Story 4 - Ajout direct au panier (Priority: P3, phase ultérieure)

Une fois connectée à son compte Chronodrive dans l'app (elle saisit elle-même ses identifiants sur la page de connexion de Chronodrive), un second bouton « Panier » apparaît, à côté de « Chrono » (fiche), sur chaque article dont le produit est retenu. « Panier » ajoute 1 unité de ce produit à son panier Chronodrive sans ouvrir l'application ; l'article est coché et un message nomme le produit ajouté. « Chrono » continue d'ouvrir la fiche (US2). En cas d'échec de « Panier », l'app propose d'ouvrir la fiche.

**Why this priority**: Gain supplémentaire demandé par l'utilisatrice, mais qui dépend d'un fonctionnement non documenté de Chronodrive (à étudier avant le plan de cette phase) ; US2 sert de repli.

**Independent Test**: Connectée, toucher « Panier » sur « lait » (produit n° 55785 retenu) : le panier Chronodrive contient 1 Lactel de plus (vérifié dans l'application Chronodrive), l'article « lait » est coché ; « Chrono » sur « lait » ouvre toujours la fiche.

**Acceptance Scenarios**:

1. **Given** l'utilisatrice connectée et un produit retenu, **When** elle touche « Panier », **Then** 1 unité est ajoutée au panier, l'article est coché, un message nomme le produit.
2. **Given** l'ajout échoue (non connectée, connexion expirée, réponse inattendue, produit indisponible), **When** elle touche « Panier », **Then** rien n'est coché, un message l'explique avec un bouton « Ouvrir la fiche » (US2).
3. **Given** un article sans produit retenu, ou l'utilisatrice non connectée, **When** la liste s'affiche, **Then** le bouton « Panier » n'apparaît pas (seul « Chrono » est là).
4. **Given** l'app, **When** elle agit sur le compte Chronodrive, **Then** elle ne fait qu'ajouter au panier : jamais de retrait, de commande, de paiement, de modification du compte.
5. **Given** l'utilisatrice non connectée, **When** elle touche « Se connecter à Chronodrive » (dans l'onglet Courses), **Then** la page de connexion de Chronodrive s'ouvre dans l'app ; elle y saisit elle-même ses identifiants ; une fois connectée, la page se ferme et les boutons « Panier » apparaissent.
6. **Given** une connexion expirée, **When** elle touche « Panier », **Then** l'app le dit et propose « Se reconnecter », qui rouvre la page de connexion ; après reconnexion, l'ajout demandé est fait.
7. **Given** l'utilisatrice, **When** elle choisit « Se déconnecter de Chronodrive » dans l'app, **Then** l'app oublie sa connexion et revient à US2.

---

### Edge Cases

- Article avec quantité (« 5 bananes ») : la fiche s'ouvre (US2) ; l'ajout direct (US4) ajoute 1 unité ; la quantité reste affichée dans la liste pour qu'elle ajuste.
- Correspondance par mots : « eau » ne correspond pas à « gâteau » ; « oeuf » correspond à « oeufs de poules » ; les accents et majuscules sont ignorés.
- Même nom pour deux produits (« Emmental râpé » n° 368958 et 82295, « eau de source » × 3) : toujours proposé au choix (US2 scénario 3), jamais choisi au hasard.
- Produit retenu qui n'est plus vendu : la fiche Chronodrive l'indique ; l'utilisatrice peut changer de produit (US3).
- Article ajouté à la main (« 2 papier toilette ») : même fonctionnement, sur « papier toilette ».
- Réimport d'un fichier où un produit a disparu : le produit reste connu (marqué « plus en favori ») tant qu'un choix l'utilise.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: L'app DOIT permettre d'importer un fichier de favoris Chronodrive (numéro, nom, marque, rayon) et l'enregistrer durablement ; un réimport ajoute et met à jour sans rien supprimer ni perdre de choix.
- **FR-001b**: Un bouton « Mettre à jour mes favoris » DOIT relire les favoris sur le compte Chronodrive quand l'utilisatrice est connectée (lecture seule), et sinon proposer la connexion ou l'import d'un fichier.
- **FR-002**: Un favori correspond à un article si son nom (sans la marque) contient chaque mot significatif du terme de recherche de l'article, en mots entiers, sans accents ni majuscules, pluriel simple accepté.
- **FR-003**: L'app DOIT retenir, pour chaque article (même clé que les coches de la feature 002), le produit choisi ; ce choix vaut pour toutes les listes de courses suivantes.
- **FR-004**: S'il y a un seul favori correspondant, il est retenu sans question ; s'il y en a plusieurs, l'utilisatrice choisit ; l'app ne choisit jamais entre plusieurs.
- **FR-005**: « Chrono » sur un article dont le produit est retenu DOIT ouvrir la fiche de ce produit dans l'application Chronodrive ; à défaut sur le site ; à défaut le comportement actuel.
- **FR-006**: Le nom de l'article DOIT rester copié comme aujourd'hui, quel que soit le chemin.
- **FR-007**: L'utilisatrice DOIT pouvoir voir, changer et oublier ses choix (US3).
- **FR-008**: (US4) La connexion au compte Chronodrive DOIT se faire par la page de connexion de Chronodrive, l'utilisatrice saisissant elle-même ses identifiants ; l'app ne conserve pas le mot de passe, seulement ce que Chronodrive lui rend pour rester connectée, et l'oublie à la déconnexion.
- **FR-009**: (US4) L'app NE DOIT faire, sur le compte Chronodrive, que lire les favoris (FR-001b) et ajouter 1 unité au panier ; jamais de retrait, commande, paiement, modification des favoris ou du compte.
- **FR-010**: (US4) L'ajout direct DOIT être un bouton « Panier » distinct de « Chrono » (fiche), visible seulement si l'utilisatrice est connectée et le produit retenu ; tout échec d'ajout laisse l'article non coché et propose d'ouvrir la fiche (FR-005).
- **FR-011**: Les données existantes (recettes, menu, coches, ajouts à la main, réglages d'import) NE DOIVENT PAS être modifiées ni perdues ; les nouvelles données (favoris importés, choix) sont ajoutées à côté.

### Key Entities

- **Produit Chronodrive** : numéro (unique), nom, marque, rayon, « encore en favori » (oui/non), date d'import.
- **Choix de produit** : clé d'article de la liste de courses (ex. `food:<code>` ou texte normalisé) → numéro de produit ; date du choix.
- **Connexion Chronodrive** (US4) : jeton de connexion rendu par Chronodrive, date d'expiration ; aucune autre donnée du compte.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Pour un article dont le produit est retenu, l'utilisatrice passe de « Chrono » à la fiche du produit ouverte dans Chronodrive en moins de 3 secondes, sans recherche.
- **SC-002**: Sur sa liste de courses réelle, au moins 50 % des articles trouvent au moins un favori correspondant, et 0 article n'est rattaché à un produit sans rapport (vérifié avec elle).
- **SC-003**: Après un premier passage sur une liste, le second passage ne pose plus aucune question pour les articles déjà choisis.
- **SC-004**: (US4) Pour un article dont le produit est retenu, le produit est dans le panier en moins de 5 secondes après « Panier », sans ouvrir Chronodrive ; 0 ajout d'un autre produit que celui retenu.
- **SC-005**: Les recettes, le menu, les coches existantes et les réglages d'import sont identiques avant et après l'installation de cette version.

## Assumptions

- Le fichier de favoris est préparé depuis le compte chronodrive.com de l'utilisatrice, connectée par elle-même dans Chrome ; le relevé du 2026-11-22 (120 produits) sert de premier import et de jeu de test.
- Le numéro de produit du site est le même que celui de l'application (vérifié : n° 122974 ouvre le citron dans l'application).
- Le terme de recherche d'un article est celui de la feature 002.
- Une seule utilisatrice, un seul téléphone ; pas de synchronisation automatique des favoris (réimport à la demande).
- US4 suppose qu'il existe un moyen stable de se connecter et d'ajouter au panier ; à étudier avant d'en faire le plan. Si ce n'est pas faisable proprement, US4 est abandonnée et US2 reste la solution.
- Hors périmètre : service d'accessibilité (refusé par l'utilisatrice), recherche dans tout le catalogue Chronodrive, retrait du panier, commande, autres enseignes.
