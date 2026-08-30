Squelette Android minimal pour Recipe Library App

Usage rapide:
- Ouvrir ce dossier dans Android Studio et laisser l'IDE télécharger les dépendances.
- Ou builder en CLI si tu as Gradle et le SDK installés:
  - Windows PowerShell:
    .\gradlew assembleDebug
    & "$env:ANDROID_HOME\platform-tools\adb.exe" install -r .\app\build\outputs\apk\debug\app-debug.apk

Notes:
- Ce squelette est volontairement minimal: il fournit une base Jetpack Compose + Activity principale.
- Vérifie/ajuste les versions Kotlin/Compose dans build.gradle.kts si nécessaire.
