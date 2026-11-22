# Quickstart : valider l'onglet Ajouter

Voir [contracts/navigation-ui.md](./contracts/navigation-ui.md) et [contracts/share.md](./contracts/share.md).

## Prérequis

- Téléphone branché (Galaxy S25), débogage USB actif.
- Application installée depuis la branche `005-onglet-ajouter` (installation habituelle du projet, jamais `connectedAndroidTest`).

## Tests automatiques

1. Tests JVM (`testDebugUnitTest`), dont `SharedTextTest` qui doit rester vert sans modification.
2. Tests instrumentés par `am instrument` sur le téléphone : `AddTabScreenTest` (nouveau) et toute la suite existante, dont `UrlImportScreenTest`, `TextImportScreenTest`, `ImportQueueScreenTest`, qui doivent passer sans modification.
3. Lancer ensuite `/verifie-tests` (aucun test affaibli, désactivé ou réaligné).

## Scénarios manuels (téléphone)

1. **Barre** : au démarrage, 4 boutons : Recettes, Ajouter, Recherche, Menu (scénario 1).
2. **Ouverture** : depuis Recettes, Recherche et Menu, toucher « Ajouter » : import par URL, sélecteur sur « URL » (scénario 2).
3. **Modes** : toucher « Fichier » puis « Photo » : mêmes écrans et mêmes possibilités qu'avant (choix de fichier ; appareil photo et galerie) ; importer un fichier et une photo jusqu'à l'écran de relecture (scénario 3, FR-004).
4. **Réinitialisation** : sur Ajouter en mode Photo, toucher à nouveau « Ajouter » : retour sur URL (FR-002).
5. **Import conservé** : lancer un import par URL, passer à « Fichier » puis revenir à « URL » : l'import est toujours en cours ou son résultat est là (scénario 4).
6. **Retour arrière** : sur Fichier ou Photo, retour : on revient à l'écran précédent de l'app, pas à « URL » ; après rotation, le mode choisi reste affiché.
7. **File d'attente** : mettre un lien en file (PC éteint) : le lien « File d'attente » de l'écran URL ouvre toujours l'écran de la file ; la notification ouvre toujours la file.
8. **Partage d'un lien** : depuis Chrome, puis Instagram, partager une page vers l'app : l'écran Ajouter s'ouvre sur « URL », lien collé et import lancé. Refaire en étant sur Ajouter en mode Photo, puis sur Menu (scénario 6, FR-005b).
9. **Autres partages** (non-régression) : partager un texte sans lien : toast « Rien à importer dans ce partage » ; partager une image : boîte « Lire la recette » / « Photo d'une recette » ; partager un fichier JSON de recette : écran d'ajout pré-rempli.
10. **Données** : recettes, file d'attente, réglages d'import et menu identiques avant et après (FR-006).
