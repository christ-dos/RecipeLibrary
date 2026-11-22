# Feature Specification: Onglet « Ajouter » unique

**Feature Branch**: `005-onglet-ajouter`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Remanier le menu principal : il y a 3 méthodes d'extraction de recette (URL, Fichier, Photo), celle que j'utilise le plus est l'URL. Un seul bouton qui permet de choisir le mode d'extraction, ouvert par défaut sur l'URL."

## Clarifications

### Session 2026-11-22

- Q: Quelle forme pour le bouton unique ? → A: Un onglet « Ajouter » dans la barre du bas, qui s'ouvre sur l'import par URL, avec en haut un sélecteur « URL / Fichier / Photo ».

## Contexte

La barre du bas compte aujourd'hui 6 boutons : Recettes, URL, Fichier, Photo, Recherche, Menu. Trois d'entre eux servent à ajouter une recette.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Un seul onglet pour ajouter une recette (Priority: P1)

La barre du bas passe à 4 boutons : Recettes, Ajouter, Recherche, Menu. « Ajouter » s'ouvre directement sur l'import par URL ; un sélecteur en haut (URL, Fichier, Photo) permet de passer aux deux autres modes, qui fonctionnent exactement comme aujourd'hui.

**Why this priority**: C'est la demande ; la barre est plus lisible et le mode le plus utilisé est à un geste.

**Independent Test**: Toucher « Ajouter » : l'écran d'import par URL s'affiche, sélecteur sur « URL ». Toucher « Photo » dans le sélecteur : l'écran d'import par photo s'affiche. Importer une recette par URL : elle arrive dans Recettes comme aujourd'hui.

**Acceptance Scenarios**:

1. **Given** l'app ouverte, **When** l'utilisatrice regarde la barre du bas, **Then** elle voit 4 boutons : Recettes, Ajouter, Recherche, Menu.
2. **Given** n'importe quel écran, **When** elle touche « Ajouter », **Then** l'import par URL s'affiche, sélecteur sur « URL ».
3. **Given** l'écran Ajouter, **When** elle choisit « Fichier » ou « Photo », **Then** l'import correspondant s'affiche, avec les mêmes possibilités qu'aujourd'hui.
4. **Given** un import en cours (par exemple une URL en attente ou en traitement), **When** elle change de mode puis revient sur « URL », **Then** l'import n'est pas perdu.
5. **Given** un contenu partagé vers l'app depuis une autre application, **When** l'app s'ouvre, **Then** il part directement vers la bonne extraction. Réalisé le 2026-11-22 : lien de page → import par URL, import lancé ; image → choix « Lire la recette » / « Photo d'une recette » ; texte de recette → import texte ; fichier recette → écran d'ajout. Reste à faire dans cette feature : le lien de page arrive sur le mode URL du nouvel écran Ajouter.
6. **Given** un reel ou une publication partagé depuis Instagram (ou un lien de page depuis Chrome, TikTok, YouTube) vers l'app, **When** l'app s'ouvre, **Then** l'import par URL s'affiche avec le lien déjà collé ; l'utilisatrice n'a plus qu'à toucher « Importer ». (Avant le 2026-11-22 un lien de page partagé était ignoré sans message ; corrigé ce jour-là.)

---

### Edge Cases

- Retour arrière depuis Fichier ou Photo : revient à l'écran précédent de l'app (pas à « URL »), comme les autres onglets.
- Rotation ou retour dans l'app : le mode choisi reste affiché.
- File d'attente des imports par URL (feature 001) : inchangée, accessible depuis l'écran URL comme aujourd'hui.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: La barre du bas DOIT contenir 4 boutons : Recettes, Ajouter, Recherche, Menu.
- **FR-002**: « Ajouter » DOIT s'ouvrir sur l'import par URL, à chaque fois qu'on le touche depuis la barre.
- **FR-003**: L'écran Ajouter DOIT proposer un sélecteur URL / Fichier / Photo, le mode actif étant mis en évidence.
- **FR-004**: Les trois modes DOIVENT garder exactement leur fonctionnement actuel (aucune régression d'import).
- **FR-005**: Les ouvertures de l'app depuis un partage DOIVENT garder le traitement par type de contenu (scénario 5) ; un lien de page DOIT arriver sur le mode URL de l'écran Ajouter, pré-rempli et importé ; un partage inexploitable n'est jamais ignoré sans message.
- **FR-006**: Aucune donnée (recettes, file d'attente d'import, réglages d'import, menu) NE DOIT être modifiée ni perdue.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Importer une recette par URL demande le même nombre de gestes qu'aujourd'hui (un appui sur « Ajouter » au lieu de « URL »).
- **SC-002**: Passer à l'import par fichier ou photo demande un seul geste de plus qu'aujourd'hui.
- **SC-003**: Tous les tests d'import existants (URL, fichier, photo, partage) restent verts.

## Assumptions

- « Recherche » et « Menu » ne changent pas.
- Le nom « Ajouter » et une icône « + » sont utilisés pour l'onglet.
- Ordre de réalisation : après Chronodrive (étapes 1-2) et le planning semaine, sauf si l'utilisatrice préfère autrement (petit chantier, peut être avancé).
