package com.example.recipelibrary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.RecipeRepository
import com.example.recipelibrary.data.RecipeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class RecipeListViewModel(private val repo: RecipeRepository) : ViewModel() {
    private val _recipes = MutableStateFlow(emptyList<Recipe>())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    init {
        repo.observeAll()
            .onEach { _recipes.value = it }
            .launchIn(viewModelScope)
    }

    fun upsert(recipe: Recipe, onComplete: (() -> Unit)? = null) = viewModelScope.launch {
        val optimistic = recipe.copy(tags = RecipeUtils.normalizeTags(recipe.tags))
        _recipes.update { current ->
            (current.filterNot { it.id == optimistic.id } + optimistic)
                .sortedByDescending { it.createdAt }
        }
        repo.upsert(recipe)
        onComplete?.invoke()
    }

    fun delete(id: String, onComplete: (() -> Unit)? = null) = viewModelScope.launch {
        _recipes.update { current -> current.filterNot { it.id == id } }
        repo.delete(id)
        onComplete?.invoke()
    }
}
