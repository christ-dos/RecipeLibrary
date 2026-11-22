# Quickstart: Menu en planning à la semaine

À réaliser **après** les étapes 1 et 2 de la feature 003 (voir plan.md : numéros de version de base de données dans l'ordre de réalisation).

## 1. Préparer

1. Sauvegarder les réglages et la base du téléphone avant de réinstaller (comme en 002) :
   - copier `url_import.xml` (réglages d'import) ; le remettre après les tests ;
   - sauvegarde de la base `recipes.db` (copie via `adb` / sauvegarde de l'app) pour vérifier la migration sur de vraies données.
2. Lire `AppDatabase.kt` : vérifier que la version actuelle est **9** (8 = feature 001 T046, 9 = feature 003) ; la 004 passe à **10** (`MIGRATION_9_10`). Si ce n'est pas 9, s'arrêter : les migrations précédentes ne sont pas livrées.

## 2. Ordre de développement (tests d'abord)

1. `MenuLogicTest` (JVM) puis `MenuLogic` : repas, tri, `groupByWeek`, `normalizePlacement`, `dayMealLabel`.
2. `MenuPlacementShoppingTest` (JVM) : liste de courses identique avec ou sans placement.
3. `DatabaseMigrationTest.migration_9_to_10_...` puis `MIGRATION_9_10` ; `MenuDaoTest` puis DAO et repository.
4. `SearchPickModeTest` puis mode choix dans `SearchScreen` et route `search_pick`.
5. `MenuScreenTest` (vue Liste puis vue Semaine) puis `MenuScreen.kt`, `MenuViewModel.kt`, câblage de `MainActivity.kt`.

## 3. Commandes (jamais `connectedAndroidTest`)

```text
./gradlew testDebugUnitTest
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk          # -r : garde les données
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.example.recipelibrary.MenuScreenTest \
  com.example.recipelibrary.test/androidx.test.runner.AndroidJUnitRunner
```
Répéter avec `MenuDaoTest`, `DatabaseMigrationTest`, `SearchPickModeTest`. Les tests instrumentés utilisent des bases de test séparées (comme les existants) et ne touchent pas la base réelle.

## 4. Essai sur le téléphone (avec les données réelles)

1. Mise à jour par `adb install -r` : ouvrir l'onglet Menu, vérifier que toutes les entrées d'avant sont là avec leurs parts et jours (SC-004), et que la liste de courses est identique à celle d'avant (coches et articles manuels compris).
2. Vue Liste : les entrées existantes n'ont pas de repas ; en choisir un (« Lundi · Midi »), vérifier le tri.
3. Basculer sur « Semaine » : les entrées avec jour sont dans « Sans repas » de leur jour, les autres dans « À placer ». Quitter l'app, la rouvrir : la vue Semaine est retrouvée.
4. Toucher une case vide (mercredi / soir) : la page Recherche s'ouvre avec le bandeau « Choisis une recette pour Mercredi · Soir » et les photos ; taper un mot, toucher une recette : retour à la Semaine, recette dans la case, présente dans la liste de courses.
5. « Annuler » et Retour système : rien n'est ajouté.
6. Déplacer une recette (titre, case, valider), la passer « Sans jour », la retirer : liste de courses cohérente à chaque étape.
7. « Retirer les jours » + confirmation : tout dans « À placer », rien retiré du menu, liste de courses inchangée.
8. Onglet Recherche de la barre du bas : un clic sur une recette ouvre toujours la fiche.

## 5. Avant de déclarer terminé

Lancer `/verifie-tests`. Signaler explicitement : (a) les cas de `MenuScreenTest` réécrits pour le mode choix, (b) l'ajout de `MIGRATION_9_10` aux listes `addMigrations` des tests existants. Aucun commit sans demande.
