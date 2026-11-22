# Quickstart: Mes produits Chronodrive (US1 à US3)

Les commandes sont celles de la feature 002.

## 1. Tests JVM

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Java\jdk-17" :app:testDebugUnitTest
```

Attendu : tous verts, dont `FavoritesFileParserTest` (relevé réel : 120 produits, [contrat](./contracts/favorites-file.md)), `ProductMatcherTest` (« eau » ≠ « gateau », « oeuf » = « oeufs », banane × 2, emmental râpé × 2), `ChronodriveLinksTest`.

## 2. Construire les APK

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Java\jdk-17" :app:assembleDebug :app:assembleDebugAndroidTest
```

## 3. Tests sur le téléphone (sans perte de données)

1. Sauvegarder `url_import.xml` et vérifier qu'il n'est pas vide (même commande qu'en 002, quickstart §3).
2. Installer **sans désinstaller** : `adb install -r` des deux APK.
3. `adb shell svc power stayon usb`, puis `adb shell am instrument -w com.example.recipelibrary.test/androidx.test.runner.AndroidJUnitRunner` (jamais `connectedAndroidTest`).
4. `adb shell svc power stayon false`, restaurer `url_import.xml` et vérifier sa taille.

Attendu : tous verts, dont `DatabaseMigrationTest` (8 → 9 : recettes, menu, coches et ajouts à la main identiques, deux tables créées vides), `ChronodriveDaoTest`, `ChronodriveProductsScreenTest`, `ShoppingListScreenTest`.

## 4. Vérification de la migration sur une copie

Avant d'installer : copier la base du téléphone (`run-as … cat databases/recipes.db*`, puis `adb pull`). Après la première ouverture, recopier et comparer : même nombre de recettes, d'entrées de menu, de coches et d'ajouts à la main, contenu identique, version 9 (SC-005). Ne jamais travailler sur la base du téléphone elle-même.

## 5. Parcours à la main

1. **US1** : placer `favoris-releve-2026-11-22.txt` sur le téléphone ; « Mes produits Chronodrive » → « Importer mes favoris » : « 120 produits Chronodrive importés », liste par rayon. Réimporter le même fichier : « 0 ajouté, 120 déjà connus ». Importer un fichier modifié (3 produits en plus, 1 en moins) : « 3 ajoutés, 119 déjà connus, 1 plus en favori ». Fichier vide : message, rien de changé.
2. **US2** : menu avec « banane » → « Chrono » : boîte avec les deux bananes (365920, 305254) ; en choisir une : la fiche s'ouvre dans l'application Chronodrive en moins de 3 s ; revenir, cocher. Second « Chrono » sur « banane » : ouverture directe sans question. Article à un seul favori (par exemple « ciboulette ») : retenu sans question. Article sans favori : comportement 002 (nom copié, favoris ouverts). Application Chronodrive absente ou lien refusé : fiche dans le navigateur.
3. **US3** : « Mes produits Chronodrive » : changer « banane » de 365920 à 305254 puis « Chrono » ; oublier « banane » : « Chrono » repropose les deux. Appui long sur un article : « Choisir mon produit Chronodrive » liste tous les favoris.
4. **SC-002 / SC-003** : sur sa liste réelle, au moins 50 % des articles ont un candidat, aucun rattachement absurde (vérifié avec elle) ; second passage sans question.
5. Vérifier que recettes, menu, coches et réglages d'import sont inchangés (SC-005).

## 6. Fin de phase

Lancer `/verifie-tests` (suite complète + relecture du diff : aucun test affaibli, ajout de `MIGRATION_8_9` au test existant signalé). Ne rien commiter sans demande.
