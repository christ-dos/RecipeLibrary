# Quickstart : valider « Calories par part »

Voir [data-model.md](./data-model.md) et [contracts/](./contracts/).

## Prérequis

- JDK 17 ; téléphone branché en USB avec débogage USB ; PC avec Hermes et Ollama (llama3.1).
- Sauvegarder les réglages de l'app avant les tests sur téléphone :
  `adb shell "run-as com.example.recipelibrary cat shared_prefs/url_import.xml > /data/local/tmp/url_import.xml"` puis `adb pull` vers le dossier de sauvegarde, et **vérifier que le fichier n'est pas vide**.

## 1. Tests unitaires (sans téléphone)

```
.\gradlew.bat "-Dorg.gradle.java.home=C:\Program Files\Java\jdk-17" :app:testDebugUnitTest
```

Attendu : tous verts, dont l'analyseur de quantités, l'appariement, le calcul, l'estimation des parts, le contrôle de `foods.json`, et le jeu des 20 recettes de référence (SC-001 : au moins 18 sur 20 à ±15 %).

## 2. Tests Hermes

```
C:\Users\Kikine\AppData\Local\hermes\hermes-agent\venv\Scripts\python.exe -m pytest C:\Users\Kikine\AppData\Local\hermes\test_recipe_extractor.py
```

Attendu : tests existants (42) + nouveaux (validation et nettoyage de la réponse de `/estimate`, champs ajoutés à `/extract`) verts.

## 3. Installation et tests sur téléphone (données conservées)

```
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w com.example.recipelibrary.test/androidx.test.runner.AndroidJUnitRunner
```

Puis restaurer les réglages (`run-as … cp /data/local/tmp/url_import.xml shared_prefs/url_import.xml`). Jamais `connectedAndroidTest` (désinstalle l'app et efface les recettes).

Attendu : tous verts, dont la migration 5 → 6 (recettes identiques), la fiche (« ≈ X kcal / part », parts estimées), le détail et la correction d'un poids, le champ « Nombre de parts ».

## 4. Vérification manuelle

1. Ouvrir une recette existante : « ≈ X kcal / part » s'affiche ; le nombre de recettes est inchangé (SC-006).
2. Ouvrir le détail : poids, origine, kcal par ingrédient ; protéines, glucides, lipides par part ; lignes non comptées avec raison.
3. Corriger le poids d'une ligne non comptée : le total change tout de suite (moins de 15 s du début à la fin, SC-005), et la valeur est toujours là après fermeture de l'app. Modifier ensuite le titre de cette recette dans le formulaire : la correction est toujours là.
4. PC allumé : importer un lien avec « pour 6 personnes » et « 2 pommes » → 6 parts, pommes comptées « estimé ».
5. Importer un texte avec « 1 noix de beurre », PC joignable → la ligne arrive déjà estimée ; PC éteint → la recette s'enregistre, puis est estimée au retour du PC.
6. Rattrapage : écran de la file → « Estimation des quantités : n / N » jusqu'à la fin ; le PC se rendort 15 min après.

## Mesures

- SC-003 (≥ 85 % des lignes comptées pour un import par lien) : _à remplir par T036_.
- SC-002 (≥ 70 % avant rattrapage, ≥ 85 % après) : _à remplir par T043_.
