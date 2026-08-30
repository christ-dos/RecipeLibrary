package com.example.recipelibrary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipelibrary.data.Recipe
import com.example.recipelibrary.data.RecipeRepository
import kotlinx.coroutines.launch

class RecipeDetailViewModel(private val repo: RecipeRepository) : ViewModel() {
    suspend fun get(id: String): Recipe? = repo.get(id)

    fun save(recipe: Recipe) = viewModelScope.launch { repo.upsert(recipe) }
}
