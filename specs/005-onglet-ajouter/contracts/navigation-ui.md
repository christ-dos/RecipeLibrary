# Contrat : navigation et interface de l'onglet Ajouter

## Barre du bas

| Ordre | Route | Libellé | Icône |
|-------|-------|---------|-------|
| 1 | `list` | Recettes | `ic_nav_home` |
| 2 | `ajouter` | Ajouter | `ic_nav_add` (illustration fournie par l'utilisatrice le 2026-11-22, réduite à 192 px) |
| 3 | `search` | Recherche | `ic_nav_search` |
| 4 | `menu` | Menu | `ic_nav_menu` |

`MAIN_ROUTES` = `list`, `ajouter`, `search`, `menu` (la barre n'est affichée que sur ces routes).

## Routes

| Route | Statut |
|-------|--------|
| `ajouter` | nouvelle ; barre du haut « Ajouter » ; contient `AddTabScreen` |
| `url_import`, `file`, `photo` | **supprimées** (plus de destination ; aucun appelant restant) |
| `list`, `search`, `menu`, `detail/{id}`, `edit/{id}`, `add`, `queue`, `ocr_review` | inchangées |

Navigation vers un onglet : toujours `navController.navigateToTab(route)` (jamais `navigate` direct, cf. commentaire existant). Le `popUpTo("url_import") { inclusive = true }` de l'import par URL devient `popUpTo("ajouter") { inclusive = true }`.

## Comportement

- Toucher « Ajouter » (depuis n'importe quel écran, **y compris s'il est déjà affiché**) : `addMode = URL`, puis navigation vers `ajouter` si besoin.
- Changer de mode par le sélecteur : change `addMode` seulement ; ne modifie pas la pile de navigation.
- Retour arrière sur `ajouter` : comme les autres onglets (vers `list`), quel que soit le mode.
- `addMode` : `rememberSaveable` au niveau de `setContent` ; survit à la rotation.

## Écran `AddTabScreen`

```text
AddTabScreen(
    mode: AddMode,                 // URL | FILE | PHOTO
    onModeChange: (AddMode) -> Unit,
    url:   @Composable () -> Unit, // UrlImportScreen(...) tel quel, toujours composé
    file:  @Composable () -> Unit, // FileImportScreen(...) tel quel
    photo: @Composable () -> Unit, // PhotoImportScreen(...) tel quel
)
```

Les callbacks des trois écrans d'import restent ceux de `MainActivity` aujourd'hui (`onImport` vers `add`, `onQueue`, `onFilePicked` vers `add`, `onGalleryPicked` vers OCR puis `ocr_review`, etc.). Aucune signature de `UrlImportScreen`, `FileImportScreen`, `PhotoImportScreen` ne change.

## Sélecteur et tags de test

| Élément | Libellé | `testTag` |
|---------|---------|-----------|
| Rangée du sélecteur | (aucun) | `add_mode_selector` |
| Onglet URL | « URL » | `add_mode_url` |
| Onglet Fichier | « Fichier » | `add_mode_file` |
| Onglet Photo | « Photo » | `add_mode_photo` |
| Zone du contenu actif | (aucun) | `add_mode_content` |

Le mode actif est exposé comme sélectionné (`selected = true` de `Tab`). Les libellés du sélecteur sont exactement « URL », « Fichier », « Photo ». Les `contentDescription` des icônes de la barre restent égaux au libellé (aucun tag n'existe aujourd'hui sur la barre ; en ajouter n'est pas nécessaire, les tests passent par le libellé).
