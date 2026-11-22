# Implementation Plan: Onglet « Ajouter » unique

**Branch**: `005-onglet-ajouter` | **Date**: 2026-11-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-onglet-ajouter/spec.md`

## Summary

La barre du bas passe de 6 à 4 boutons : Recettes, Ajouter, Recherche, Menu. « Ajouter » est une seule destination de navigation (`ajouter`) qui s'ouvre toujours sur l'import par URL et affiche en haut un sélecteur URL / Fichier / Photo. Les trois écrans d'import (`UrlImportScreen`, `FileImportScreen`, `PhotoImportScreen`) ne changent ni de comportement ni de signature ; ils sont simplement hébergés dans un nouvel écran conteneur `AddTabScreen`. Le mode affiché est un état unique, détenu au niveau de `setContent` dans `MainActivity`, ce qui permet de le remettre sur « URL » à chaque appui sur l'onglet et d'y ramener un lien partagé. Aucune donnée n'est touchée : pas de migration, pas de nouvelle table.

## Déjà fait (2026-11-22) : partage vers l'app

Livré avant cette feature, à ne pas replanifier (FR-005b) :

- `app/src/main/java/com/example/recipelibrary/data/share/SharedText.kt` classe le texte partagé : `PageLink` (lien de page : import par URL pré-rempli et lancé tout de suite via `UrlImportScreen(sharedUrl, onSharedUrlConsumed)`), `ImageLink` (lien d'image : photo de recette), `RecipeJson` (écran `add`), `RecipeText` (import texte), `Unknown` (toast « Rien à importer dans ce partage »).
- Une image partagée ouvre une boîte de dialogue : « Lire la recette » (OCR puis `ocr_review`) ou « Photo d'une recette ».
- Tests JVM : `app/src/test/java/com/example/recipelibrary/share/SharedTextTest.kt`.
- Aujourd'hui, un lien de page partagé fait `navigateToTab("url_import")`. **Cette feature doit conserver ce comportement** : le lien doit atterrir sur le mode URL de l'onglet Ajouter (voir [contracts/share.md](./contracts/share.md)).

## Technical Context

**Language/Version**: Kotlin 1.9 (JVM 17).

**Primary Dependencies**: Jetpack Compose (Material 1.5), Navigation Compose 2.7 ; aucune nouvelle dépendance.

**Storage**: aucun changement (Room reste en version 7). Les préférences d'import (`HermesPrefs`) et la file d'attente ne sont pas touchées.

**Testing**: JUnit en JVM (`app/src/test`) pour la logique pure ; Compose UI tests (`app/src/androidTest`) lancés par `am instrument` sur le téléphone.

**Target Platform**: Android minSdk 24, targetSdk 34, Galaxy S25.

**Project Type**: Application mobile (module unique `app`).

**Performance Goals**: changer de mode sans latence perceptible ; l'écran URL n'est jamais recréé à la bascule.

**Constraints**: les trois imports fonctionnent exactement comme aujourd'hui (FR-004) ; Material 1.5 n'a pas de bouton segmenté ; jamais `connectedAndroidTest`.

**Scale/Scope**: 1 nouveau fichier d'écran, 1 icône, modifications localisées dans `MainActivity.kt`.

## Constitution Check

`.specify/memory/constitution.md` est encore le modèle non renseigné : aucune règle à vérifier, aucune porte bloquante. Règles de fait du projet respectées : aucune donnée modifiée, tests existants non affaiblis, logique pure testée en JVM.

## Project Structure

### Documentation (this feature)

```text
specs/005-onglet-ajouter/
├── spec.md
├── plan.md
├── research.md
├── quickstart.md
└── contracts/
    ├── navigation-ui.md   # routes, barre du bas, sélecteur, tags de test
    └── share.md           # contrat de partage vers l'onglet Ajouter
```

Pas de `data-model.md` : aucune entité ni donnée persistée n'est ajoutée ou modifiée (seul l'état d'affichage `AddMode` existe, en mémoire).

### Source Code (repository root)

```text
app/src/main/java/com/example/recipelibrary/
├── MainActivity.kt            # NAV_ITEMS (4), MAIN_ROUTES, route "ajouter", état addMode, partage
├── ui/AddTabScreen.kt         # NOUVEAU : enum AddMode + AddTabScreen (sélecteur + 3 emplacements)
├── ui/UrlImportScreen.kt      # inchangé
├── ui/FileImportScreen.kt     # inchangé
└── ui/PhotoImportScreen.kt    # inchangé
app/src/main/res/drawable/ic_nav_add.xml                                  # NOUVEAU : icône « + »
app/src/androidTest/java/com/example/recipelibrary/AddTabScreenTest.kt    # NOUVEAU
```

**Structure Decision**: écran conteneur « à emplacements » (slots) : `AddTabScreen(mode, onModeChange, url = {...}, file = {...}, photo = {...})`. Il ne connaît ni `ImportQueue`, ni l'OCR, ni la navigation ; `MainActivity` lui fournit les trois écrans existants avec leurs callbacks actuels. Il est donc testable seul par un Compose UI test.

## Conception

### 1. Barre du bas

- `NAV_ITEMS` : `list` Recettes, `ajouter` Ajouter (icône `ic_nav_url` réutilisée, décision du 2026-11-22), `search` Recherche, `menu` Menu.
- `MAIN_ROUTES` = `list`, `ajouter`, `search`, `menu`. Les routes `url_import`, `file`, `photo` disparaissent (plus aucun appelant, aucun test ne les utilise).
- Les routes `queue`, `ocr_review`, `add`, `detail/{id}`, `edit/{id}` ne changent pas ; n'étant pas dans `MAIN_ROUTES`, la barre y reste masquée comme aujourd'hui.
- Avec 4 boutons au lieu de 6 (`weight(1f)`), la mise en page existante convient ; aucune autre retouche de style.

### 2. Destination `ajouter` et état du mode

- `AddMode { URL, FILE, PHOTO }` ; l'état `addMode` est un `rememberSaveable` posé dans `setContent` (au niveau de `navController`), donc conservé à la rotation (edge case de la spec).
- Appui sur « Ajouter » dans la barre : `addMode = URL`, puis `navigateToTab("ajouter")` si on n'y est pas déjà. L'appui sur l'onglet **déjà affiché** doit aussi remettre sur URL (FR-002) : le lambda `onTabSelected` ne peut donc plus se contenter de `if (route != currentRoute)`, il traite ce cas pour `ajouter`.
- Le restore d'état de `navigateToTab` (`restoreState = true`) ne doit pas faire revenir un ancien mode : le mode vient de `addMode` (réinitialisé au toucher), pas de l'état sauvé de la destination.
- Retour arrière : changer de mode ne pousse rien dans la pile ; le retour quitte l'onglet comme pour les autres (vers `list`), conformément à la spec.
- Barre du haut : « Ajouter » (remplace « Depuis URL », « Fichier », « Photo »).

### 3. Conserver un import en cours en changeant de mode (scénario 4)

`UrlImportScreen` garde son état de chargement dans `remember` et lance l'import dans son `rememberCoroutineScope` : s'il sort de la composition, l'import en cours est annulé. `AddTabScreen` garde donc l'emplacement `url` **toujours composé** et ne le masque que visuellement : quand le mode actif n'est pas URL, il reçoit une taille nulle (via `Modifier.layout`) et ne capte plus les touches. Fichier et Photo (sans état à conserver) ne sont composés que lorsqu'ils sont actifs. Variantes écartées dans [research.md](./research.md). Quitter l'onglet Ajouter par la barre garde le comportement actuel : l'écran sort de la composition.

### 4. Sélecteur

`TabRow` Material 1.5 à trois `Tab` (URL, Fichier, Photo) sous la barre du haut, mode actif mis en évidence aux couleurs existantes (`ActiveLabel`, `ActiveSurface`). Tags de test et libellés dans [contracts/navigation-ui.md](./contracts/navigation-ui.md).

### 5. Partage vers l'app

Voir [contracts/share.md](./contracts/share.md). `handleSharedText` et le dialogue d'image ne changent pas ; seul change le point d'entrée dans l'UI :

- `LaunchedEffect(sharedUrl)` : si `sharedUrl != null`, `addMode = URL` puis navigation vers `ajouter` si `currentRoute != "ajouter"`. Si on est déjà sur Ajouter en mode Fichier ou Photo, la mise à `URL` suffit à faire apparaître l'écran URL, qui consomme alors `sharedUrl` (pré-remplissage et lancement automatique inchangés).
- Fichier recette partagé (JSON) et texte de recette : ouvrent déjà l'écran `add`, sans passer par un onglet d'import ; inchangé.
- Image partagée : dialogue puis `ocr_review` ou photo de recette ; inchangé.

Écart avec la spec (scénario 5 : « Fichier pour un fichier, Photo pour une image ») : dans le code actuel, un fichier ou une image partagés n'ouvrent ni l'onglet Fichier ni l'onglet Photo. Le plan garde ce comportement existant ; seul le lien de page cible l'onglet Ajouter. Voir la question ouverte Q1 dans [research.md](./research.md).

### 6. Icône

`ic_nav_add.xml` : « + » dans le style des icônes de navigation existantes. `ic_nav_url`, `ic_nav_file`, `ic_nav_photo` : supprimées seulement si plus aucune référence (à vérifier à l'implémentation).

## Tests impactés

Recherche faite dans `app/src/androidTest` et `app/src/test` : aucun test ne lance `MainActivity`, ne passe par la barre du bas, ni ne référence les routes `url_import`, `file` ou `photo`. L'occurrence « url_import » de `UrlImportScreenTest.kt` est le **nom d'un fichier de préférences**, pas une route : à ne pas renommer. Les tests d'import travaillent sur les composables seuls et ne changent pas.

| Test | Impact | Action |
|------|--------|--------|
| `UrlImportScreenTest` | écran inchangé (signature et comportement) | aucune ; doit rester vert tel quel |
| `TextImportScreenTest`, `ImportQueueScreenTest`, `FileTextExtractorTest` | inchangés | aucune |
| `SharedTextTest` (JVM) | classification inchangée | aucune ; garde-fou du contrat de partage |
| `MenuScreenTest`, `SearchScreenTest`, `HomeScreenTest`, etc. | écrans inchangés | aucune |

Nouveau test (ajouté ; aucun test existant modifié ni affaibli) :

- `AddTabScreenTest` (androidTest, Compose UI) : au premier affichage le contenu URL est visible et l'onglet « URL » sélectionné ; toucher « Fichier » puis « Photo » affiche le contenu correspondant et masque les autres ; l'état d'un contenu placé dans l'emplacement URL (champ saisi ou compteur) survit à un aller-retour URL, Fichier, URL ; le sélecteur reste visible dans les trois modes.
- Pas de test JVM dédié : `AddMode` n'a pas de logique pure à tester (si une conversion mode/clé sauvegardable apparaît, ajouter un `AddModeTest`).
- Navigation de bout en bout (barre à 4 boutons, « Ajouter » ouvre l'URL, lien partagé atterrit sur URL) : vérifiée à la main sur le téléphone, scénarios dans [quickstart.md](./quickstart.md), car aucun test existant n'instrumente `MainActivity`.

## Risques

- Appui sur « Ajouter » alors qu'on est déjà sur Ajouter en mode Fichier ou Photo : doit revenir à URL (point 2).
- Lien partagé reçu pendant qu'un import URL est en cours : `UrlImportScreen` l'ignore déjà si `loading` (comportement actuel, inchangé).
- Contenu URL masqué par taille nulle : vérifier qu'il ne garde ni focus ni clavier quand un autre mode est actif.

## Complexity Tracking

Aucune violation ; pas de dépendance, de couche ni de migration ajoutées.
