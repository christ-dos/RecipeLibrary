# Data Model: Menu en planning à la semaine

## Entité modifiée : MenuEntry (`menu_entries`, feature 002)

| Champ | Type | Statut | Règle |
|---|---|---|---|
| `id` | TEXT PK | inchangé | |
| `recipe_id` | TEXT | inchangé | |
| `servings` | INTEGER? | inchangé | 1..100 |
| `times` | INTEGER, défaut 1 | inchangé | 1..10 |
| `day` | INTEGER? | inchangé | 1 = lundi … 7 = dimanche ; null = « Sans jour » / « À placer » |
| **`meal`** | INTEGER? | **nouveau** | 1 = petit-déjeuner, 2 = midi, 3 = soir ; null = sans repas |
| `position` | INTEGER | inchangé | |
| `created_at` | INTEGER | inchangé | |

```kotlin
val meal: Int? = null   // colonne `meal`, pas de defaultValue (nullable)
companion object { const val MEAL_BREAKFAST = 1; const val MEAL_LUNCH = 2; const val MEAL_DINNER = 3 }
```

**Invariants**
- `meal != null` implique `day != null` (FR-002). Garanti par `setPlacement`, `setDay(null)` et `MenuLogic.normalizePlacement` ; la lecture traite un état impossible `day = null, meal != null` comme « À placer ».
- `meal` hors 1..3 refusé par `MenuRepository` (comme `setDay` pour 1..7).
- Plusieurs entrées dans la même case et la même recette plusieurs fois : permis.

**Transitions** (une seule écriture SQL chacune)

| Action | Effet |
|---|---|
| Placer / déplacer | `day = d`, `meal = m` (m peut être null : ligne du jour, « Sans repas ») |
| « Sans jour » | `day = null`, `meal = null` |
| Changer le jour dans la vue Liste | `setDay(id, d)` : repas conservé ; `setDay(id, null)` : repas effacé |
| Retirer les jours | `day = null`, `meal = null` pour toutes les lignes |
| Ajout depuis une case | insertion avec `day`, `meal` de la case, parts par défaut (comme 002) |

Parts, `times`, `position`, `created_at` ne changent jamais lors d'un placement.

DAO (ajouts) :
```kotlin
@Query("UPDATE menu_entries SET day = :day, meal = CASE WHEN :day IS NULL THEN NULL ELSE :meal END WHERE id = :id")
suspend fun setPlacement(id: String, day: Int?, meal: Int?)
@Query("UPDATE menu_entries SET day = :day, meal = CASE WHEN :day IS NULL THEN NULL ELSE meal END WHERE id = :id")
suspend fun setDay(id: String, day: Int?)          // remplace la requête actuelle
@Query("UPDATE menu_entries SET day = NULL, meal = NULL")
suspend fun clearPlacements()
```

## Entités inchangées

`ShoppingCheck`, `ShoppingExtra`, `Recipe`, `PendingImport`.

## Préférence (hors Room)

`SharedPreferences` `menu_prefs` : `menu_view` = `"list"` (défaut) ou `"week"`.

## Types dérivés (mémoire, non stockés)

```kotlin
data class WeekPlan(
    val cells: Map<Pair<Int, Int>, List<MenuEntry>>,  // (jour 1..7, repas 1..3) -> entrées
    val noMeal: Map<Int, List<MenuEntry>>,            // jour -> entrées sans repas
    val unplaced: List<MenuEntry>                     // sans jour
)
```
Ordre dans chaque liste : `position`, puis `createdAt`. Toute entrée apparaît dans exactement une des trois parties (test JVM : aucune absente, aucune en double).

## Migration Room 9 -> 10

La base passe de la version 9 à la version 10 : 8 est prise par la feature 001 (T046), 9 par la feature 003, 10 par la 004 (voir plan.md). Avant d'implémenter, vérifier que `AppDatabase.kt` est bien en `version = 9`.

```kotlin
// Planning : colonne facultative `meal` sur les entrées de menu. Aucune donnée existante modifiée :
// les lignes existantes reçoivent NULL (pas de repas).
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE menu_entries ADD COLUMN meal INTEGER")
    }
}
```

À faire : `@Database(version = 10)` ; `DatabaseProvider.kt` et chaque `addMigrations(...)` de `DatabaseMigrationTest.kt` reçoivent `MIGRATION_9_10` (seule modification des tests existants : à signaler).

### Test de migration (obligatoire, écrit avant la migration)

`DatabaseMigrationTest.migration_9_to_10_keepsMenuEntries_mealNull`, sur le modèle de `migration_6_to_7_...` :
1. créer à la main une base à la version 9 : tables `recipes`, `pending_imports`, `menu_entries` (sans `meal`), `shopping_checks`, `shopping_extras` (+ colonnes et tables ajoutées en versions 8 et 9 par les features 001 et 003) ;
2. insérer : 2 recettes, 3 entrées de menu (une avec parts + jour, une avec `times` sans jour, une même recette en double), 2 coches, 2 articles manuels (un coché) ;
3. ouvrir avec Room et `addMigrations(... MIGRATION_9_10)` ;
4. vérifier : recettes identiques (égalité complète), les 3 entrées avec mêmes `servings`, `times`, `day`, `position`, `created_at` et `meal == null` ; coches et articles manuels identiques ; liste de courses calculée sur ces entrées identique à celle d'avant ;
5. vérifier qu'on peut écrire `setPlacement` puis relire.

### Autres tests de données

- `MenuDaoTest` : `setPlacement` (jour + repas) ; `setPlacement(null, m)` n'écrit pas de repas ; `setDay(null)` efface le repas, `setDay(d)` le garde ; `clearPlacements` ne supprime aucune ligne, coche ou article manuel et ne change pas les parts.
- `MenuLogicTest` (JVM) : `normalizePlacement`, tri par repas dans un jour, `groupByWeek` (aucune entrée perdue ni dupliquée), `dayMealLabel`.
- `MenuPlacementShoppingTest` (JVM) : mêmes recettes avec ou sans placement -> même liste de courses.
