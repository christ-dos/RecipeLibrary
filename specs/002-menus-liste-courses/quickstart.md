# Quickstart: Menus et liste de courses

## 1. Tests JVM

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Java\jdk-17" :app:testDebugUnitTest
```

Attendu : tous verts, dont `ShoppingListBuilderTest` (10 menus de référence, SC-002), `QuantityFormatTest`, `ChronodriveSearchTest`, `ShoppingTextTest`.

## 2. Construire les APK

```powershell
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Java\jdk-17" :app:assembleDebug :app:assembleDebugAndroidTest
```

## 3. Tests sur le téléphone (sans perte de données)

1. Sauvegarder les réglages et vérifier qu'ils ne sont pas vides :
   `adb shell "run-as com.example.recipelibrary cat shared_prefs/url_import.xml > /data/local/tmp/url_import.xml; wc -c /data/local/tmp/url_import.xml"`
2. Installer **sans désinstaller** : `adb install -r` des deux APK.
3. Écran allumé : `adb shell svc power stayon usb`.
4. `adb shell am instrument -w com.example.recipelibrary.test/androidx.test.runner.AndroidJUnitRunner` — jamais `connectedAndroidTest`.
5. `adb shell svc power stayon false`, puis restaurer `url_import.xml` et vérifier sa taille.

Attendu : tous verts, dont `DatabaseMigrationTest` (6 → 7 : recettes identiques, trois tables vides), `MenuScreenTest`, `ShoppingListScreenTest`.

## 4. Vérification de la migration sur une copie

Après la première ouverture de l'app, copier la base (`run-as … cat databases/recipes.db* > /data/local/tmp/…`, puis `adb pull`) et comparer avec la sauvegarde : même nombre de recettes, contenu identique, version 7 (SC-004). Ne jamais travailler sur la base du téléphone elle-même.

## 5. Parcours à la main (SC-001, SC-006)

1. Onglet Menu → ajouter 5 recettes, dont une deux fois et une avec un jour → chronométrer jusqu'à la liste affichée (< 2 min).
2. Vue Courses : vérifier les regroupements (farine, œufs, lait) et l'origine d'un article.
3. Cocher un article, ajouter « papier toilette », partager vers une application de notes : texte conforme à `contracts/shopping-text.md`.
4. « Chronodrive » sur 10 articles reconnus : l'application Chronodrive s'ouvre sans fenêtre de magasin ; coller le nom dans sa recherche et noter combien montrent le bon type de produit en tête (≥ 9). Revenir : l'article est mis en évidence.
5. Supprimer une recette présente au menu : le message prévient, l'entrée disparaît du menu.
