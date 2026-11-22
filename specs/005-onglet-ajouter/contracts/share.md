# Contrat : partage vers l'app et onglet Ajouter

État : **partage déjà implémenté (2026-11-22)**, décrit ici pour garantir que l'onglet Ajouter le conserve. Seule la première ligne du tableau change de cible (onglet Ajouter au lieu de la route `url_import`).

## Classification du texte partagé (inchangé)

Source : `app/src/main/java/com/example/recipelibrary/data/share/SharedText.kt`, testée par `app/src/test/java/com/example/recipelibrary/share/SharedTextTest.kt`.

| Contenu reçu | Résultat `SharedText` | Destination |
|--------------|----------------------|-------------|
| lien de page (Instagram, Chrome, TikTok, YouTube…) | `PageLink` | **onglet Ajouter, mode URL**, lien pré-rempli et import lancé automatiquement |
| lien d'image | `ImageLink` | photo de recette (toast « Image reçue — ouvre une recette pour l'appliquer ») |
| JSON de recette | `RecipeJson` | écran `add` pré-rempli (toast si illisible) |
| texte de recette | `RecipeText` | écran `add` via import texte |
| autre | `Unknown` | toast « Rien à importer dans ce partage » |
| image partagée (`image/*`) | (dialogue) | « Lire la recette » : OCR puis `ocr_review` ; « Photo d'une recette » : photo pour une recette |

## Exigences pour cette feature

1. Un `PageLink` renseigne `sharedUrl` ; `LaunchedEffect(sharedUrl)` fait alors `addMode = URL` et navigue vers `ajouter` via `navigateToTab` si la route courante n'est pas `ajouter` (aujourd'hui : `navigateToTab("url_import")`).
2. `UrlImportScreen` doit être composé pour consommer le lien (`sharedUrl`, `onSharedUrlConsumed = { sharedUrl = null }`) : il est toujours composé dans `AddTabScreen` ; si l'utilisatrice était sur Fichier ou Photo, le passage forcé en mode URL le rend visible.
3. Comportement de `UrlImportScreen` inchangé : colle le lien, lance l'import, ignore le lien si un import est déjà en cours.
4. Aucun changement à `SharedText`, `handleSharedText`, au dialogue d'image, ni aux filtres d'intention du manifeste.

## Vérification

- JVM : `SharedTextTest` reste vert, non modifié.
- Manuel (quickstart, scénarios 8 et 9) : partager un lien depuis Chrome ou Instagram : l'onglet Ajouter s'ouvre sur « URL » avec le lien et l'import démarré, depuis chaque écran de départ, y compris depuis Ajouter en mode Photo.
