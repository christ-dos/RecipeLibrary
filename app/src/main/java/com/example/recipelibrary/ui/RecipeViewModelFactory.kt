package com.example.recipelibrary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.recipelibrary.data.RecipeRepository

class RecipeViewModelFactory(private val repo: RecipeRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RecipeListViewModel::class.java) -> RecipeListViewModel(repo) as T
            modelClass.isAssignableFrom(RecipeDetailViewModel::class.java) -> RecipeDetailViewModel(repo) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
