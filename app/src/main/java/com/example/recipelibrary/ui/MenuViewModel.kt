package com.example.recipelibrary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.menu.MenuEntry
import com.example.recipelibrary.data.menu.MenuLogic
import com.example.recipelibrary.data.menu.MenuRepository
import com.example.recipelibrary.data.menu.ShoppingExtra
import com.example.recipelibrary.data.nutrition.FoodTable
import com.example.recipelibrary.data.nutrition.IngredientParser
import com.example.recipelibrary.data.nutrition.NutritionCalculator
import com.example.recipelibrary.data.shopping.ShoppingItem
import com.example.recipelibrary.data.shopping.ShoppingListBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Menu et liste de courses (spec 002). Les recettes ne sont jamais écrites ici : la saisie
 * volontaire des parts d'une recette passe par l'écran, qui l'enregistre comme les autres modifications.
 */
class MenuViewModel(
    private val repo: MenuRepository,
    recipes: Flow<List<Recipe>>
) : ViewModel() {

    private val foodTable = MutableStateFlow<FoodTable?>(null)

    fun setFoodTable(table: FoodTable?) { if (table != null) foodTable.value = table }

    val entries: StateFlow<List<MenuEntry>> =
        repo.observeEntries().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recipesById: StateFlow<Map<String, Recipe>> =
        recipes.map { list -> list.associateBy { it.id } }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val extras: StateFlow<List<ShoppingExtra>> =
        repo.observeExtras().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Liste calculée hors du fil principal ; null tant que la table d'aliments n'est pas chargée. */
    val shopping: StateFlow<List<ShoppingItem>?> =
        combine(entries, recipesById, foodTable, repo.observeCheckedKeys()) { e, r, table, checked ->
            if (table == null) null
            else {
                val keys = checked.toSet()
                buildList(e, r, table).map { if (it.key in keys) it.copy(checked = true) else it }
            }
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Entrées par recette : sert à l'avertissement avant suppression d'une recette (FR-005). */
    fun menuCount(recipeId: String): Int = entries.value.count { it.recipeId == recipeId }

    fun add(recipe: Recipe) = viewModelScope.launch {
        val estimated = foodTable.value?.let { NutritionCalculator(it).estimate(recipe).servings }
        repo.add(recipe, MenuLogic.defaultServings(recipe, estimated))
    }

    fun setServings(entryId: String, n: Int) = viewModelScope.launch { repo.setServings(entryId, n) }
    fun setTimes(entryId: String, n: Int) = viewModelScope.launch { repo.setTimes(entryId, n) }
    fun setDay(entryId: String, day: Int?) = viewModelScope.launch { repo.setDay(entryId, day) }
    fun remove(entryId: String) = viewModelScope.launch { repo.remove(entryId) }
    fun clear() = viewModelScope.launch { repo.clear() }

    /** Appelé à l'ouverture de l'onglet Menu : les coches de plus de 3 jours s'effacent. */
    fun expireChecks() = viewModelScope.launch { repo.expireChecks() }
    fun check(key: String, checked: Boolean) = viewModelScope.launch { if (checked) repo.check(key) else repo.uncheck(key) }
    fun addExtra(text: String) = viewModelScope.launch { repo.addExtra(text) }
    fun setExtraText(id: String, text: String) = viewModelScope.launch { repo.setExtraText(id, text) }
    fun setExtraChecked(id: String, checked: Boolean) = viewModelScope.launch { repo.setExtraChecked(id, checked) }
    fun removeExtra(id: String) = viewModelScope.launch { repo.removeExtra(id) }

    class Factory(private val repo: MenuRepository, private val recipes: Flow<List<Recipe>>) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MenuViewModel(repo, recipes) as T
    }

    companion object {
        /** Parts de chaque recette : indiquées, sinon estimées par le calcul des calories. */
        fun buildList(entries: List<MenuEntry>, recipesById: Map<String, Recipe>, table: FoodTable): List<ShoppingItem> {
            val calculator = NutritionCalculator(table)
            return ShoppingListBuilder.build(entries, recipesById, IngredientParser(table)) { r ->
                r.servings?.takeIf { MenuLogic.isValidServings(it) } ?: calculator.estimate(r).servings
            }
        }
    }
}
