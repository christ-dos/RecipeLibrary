# Research: Onglet « Ajouter » unique

Aucune inconnue technique bloquante. Décisions prises à partir de la lecture de `MainActivity.kt`, `UrlImportScreen.kt`, `FileImportScreen.kt`, `PhotoImportScreen.kt` et des tests existants.

## D1. Une destination unique `ajouter` avec mode en état, pas trois routes

- **Decision**: une seule route `ajouter` ; le mode (URL / Fichier / Photo) est un état `AddMode` détenu au niveau de `setContent`.
- **Rationale**: le sélecteur n'est pas de la navigation (le retour arrière ne doit pas parcourir les modes, cf. spec) ; l'état au niveau activité permet de le remettre sur URL au toucher de l'onglet et d'y envoyer un lien partagé sans dépendre du restore d'état de la navigation.
- **Alternatives**: (a) garder trois routes `url_import`/`file`/`photo` derrière un seul onglet : la barre devrait mapper trois routes sur un bouton, `popUpTo`/`restoreState` ramèneraient le dernier mode (contraire à FR-002) et le retour arrière traverserait les modes ; (b) argument de route `ajouter?mode=` : `restoreState` restaure l'ancien argument, ce qui complique la remise à URL.

## D2. Mode réinitialisé à chaque appui sur l'onglet

- **Decision**: tout appui sur « Ajouter », même déjà affiché, met `addMode = URL`.
- **Rationale**: FR-002 (« à chaque fois qu'on le touche »). `RecipeBottomNav` appelle `onTabSelected` pour tous les boutons ; la garde `route != currentRoute` de `MainActivity` doit laisser passer ce cas pour `ajouter`.
- **Alternatives**: se souvenir du dernier mode : rejeté, la spec veut l'URL par défaut.

## D3. Garder l'import URL en cours au changement de mode

- **Decision**: l'emplacement URL de `AddTabScreen` reste toujours composé ; masqué par une taille nulle quand un autre mode est actif.
- **Rationale**: `UrlImportScreen` lance son import dans `rememberCoroutineScope` et garde `loading` dans `remember` : la sortie de composition annule l'import. Seuls `url` et les champs serveur sont en `rememberSaveable`. Le scénario 4 de la spec impose de ne pas perdre l'import.
- **Alternatives**: (a) `when(mode)` simple : perd l'import en cours, rejeté ; (b) `SaveableStateHolder` : conserve le texte saisi mais pas l'import en vol, insuffisant ; (c) remonter l'état d'import dans un ViewModel : plus propre mais modifie `UrlImportScreen` et ses tests (hors périmètre, FR-004) ; (d) `Modifier.alpha(0f)` : l'écran resterait cliquable, rejeté.
- **Limite assumée**: quitter l'onglet Ajouter par la barre (vers Recettes, etc.) garde le comportement actuel : l'écran sort de la composition. Non régressif.

## D4. Sélecteur : `TabRow` Material 1.5

- **Decision**: `TabRow` avec 3 `Tab`.
- **Rationale**: le projet utilise `androidx.compose.material.*` (Material 1.5), qui n'a pas de `SegmentedButton`. `TabRow` est dans la dépendance existante et fournit gratuitement l'état sélectionné et l'accessibilité.
- **Alternatives**: rangée de trois boutons faits maison : plus de style à entretenir ; Material 3 : nouvelle dépendance, refusée.

## D5. Icône de l'onglet

- **Decision**: nouvelle icône vecteur `ic_nav_add.xml` (« + »), dans le style des `ic_nav_*` existantes.
- **Rationale**: spec (« icône + »). `NavItem.icon` est un `Int` de drawable et `RecipeBottomNav` utilise `painterResource` ; un drawable laisse le code de la barre inchangé.

## D6. Partage : conservé, pas replanifié

- **Decision**: `handleSharedText`, `SharedText`, le dialogue d'image et `UrlImportScreen(sharedUrl, onSharedUrlConsumed)` ne changent pas ; seul le point d'entrée UI (`LaunchedEffect(sharedUrl)`) cible `ajouter` avec `addMode = URL` au lieu de `navigateToTab("url_import")`.
- **Rationale**: livré le 2026-11-22 et couvert par `SharedTextTest`. Contrat dans [contracts/share.md](./contracts/share.md).

## D7. Pas de nouveau test d'instrumentation de `MainActivity`

- **Decision**: la navigation de bout en bout est vérifiée à la main (quickstart) ; le conteneur `AddTabScreen` est testé en Compose UI.
- **Rationale**: aucun test existant ne lance `MainActivity` ; en ajouter un demanderait de neutraliser réseau, base et notifications, hors proportion pour un petit chantier. Le conteneur à emplacements isole la logique nouvelle.

## Questions ouvertes

- **Q1** (scénario 5 de la spec) : « Fichier pour un fichier, Photo pour une image ». Aujourd'hui un fichier recette partagé ouvre l'écran `add` pré-rempli et une image partagée ouvre le dialogue (OCR ou photo de recette) : aucun ne passe par les onglets Fichier ou Photo. Le plan conserve ce comportement (il fonctionne, FR-004) et ne cible l'onglet Ajouter que pour les liens de page. À confirmer avec l'utilisatrice ; sinon reformuler le scénario 5.
- **Q2** : supprimer les icônes `ic_nav_url`, `ic_nav_file`, `ic_nav_photo` si le sélecteur est en texte seul (supposé : oui, si plus aucune référence).
