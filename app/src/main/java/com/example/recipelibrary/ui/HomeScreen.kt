package com.example.recipelibrary.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recipelibrary.R
import com.example.recipelibrary.data.Recipe

private fun String.normTitle(): String =
    if (length > 1 && this == this.uppercase()) lowercase().replaceFirstChar { it.uppercase() } else this

private val DeleteRed = Color(0xFFCC0000)

@Composable
fun HomeScreen(
    recipes: List<Recipe>,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onDeleteRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Nombre d'entrées du menu qui visent la recette (avertissement avant suppression, FR-005). */
    menuCountOf: (String) -> Int = { 0 },
) {
    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Toutes") }
    var recipeToDelete by remember { mutableStateOf<Recipe?>(null) }
    val context = LocalContext.current

    val categories = listOf("Toutes") + recipes.mapNotNull { it.category }.distinct().sorted()

    val filtered = recipes.filter { r ->
        val q = query.trim().lowercase()
        val matchesQ = q.isEmpty() || r.title.lowercase().contains(q)
        val matchesCat = selectedCategory == "Toutes" || r.category == selectedCategory
        matchesQ && matchesCat
    }

    val suggestion = remember(query, recipes.size) {
        if (query.trim().length >= 3) bestSuggestion(query, recipes) else null
    }

    // Delete confirmation dialog
    recipeToDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = { Text("Supprimer cette recette ?") },
            text = {
                val n = menuCountOf(recipe.id)
                Text(
                    "\"${recipe.title.normTitle()}\" sera définitivement supprimée." +
                        if (n > 0) " Elle est au menu ($n fois) et en sera retirée." else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteRecipe(recipe.id)
                    recipeToDelete = null
                }) {
                    Text("Supprimer", color = DeleteRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ColorScreenBg)
    ) {
        // Search field
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = {
                Text("Rechercher par titre…", color = ColorInactiveText, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = ColorInactiveText)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Effacer", tint = ColorInactiveText)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = ColorTitleViolet,
                unfocusedBorderColor = ColorNavDivider,
                cursorColor = ColorTitleViolet,
                backgroundColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics { testTag = "home_search" }
        )

        // Suggestion orthographique — visible sous la barre de recherche si aucun résultat exact
        if (filtered.isEmpty() && suggestion != null) {
            SpellSuggestionBanner(
                suggestion = suggestion,
                onApply = { query = it },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Filter chips
        if (categories.size > 1) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    val count = if (cat == "Toutes") recipes.size
                                else recipes.count { it.category == cat }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) Color(0xFFF3ECFF) else Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) ColorTitleViolet else ColorNavDivider
                        ),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        val label = if (cat == "Toutes") "✨ Toutes ($count)"
                                    else "${categoryEmoji(cat)} $cat ($count)"
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) ColorTitleViolet else ColorInactiveText
                        )
                    }
                }
            }
        }

        // Grid / empty state
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (query.isNotEmpty()) "Aucun résultat pour « $query »"
                        else "Aucune recette pour l'instant.",
                        color = ColorInactiveText
                    )
                    if (query.isEmpty()) {
                        Text(
                            "Appuyez sur + pour en ajouter une.",
                            style = MaterialTheme.typography.body2,
                            color = ColorInactiveText
                        )
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)
            ) {
                items(filtered, key = { it.id }) { recipe ->
                    RecipeGridCard(
                        recipe = recipe,
                        modifier = Modifier.semantics { testTag = "recipe_card" },
                        onView = { onNavigateToDetail(recipe.id) },
                        onEdit = { onNavigateToEdit(recipe.id) },
                        onShare = {
                            val text = buildString {
                                val emoji = categoryEmoji(recipe.category)
                                append("$emoji ${recipe.title.normTitle()}\n")
                                recipe.category?.let { append("$it\n") }
                                recipe.description?.let { d ->
                                    if (d.isNotBlank()) append("\n$d\n")
                                }
                                if (recipe.ingredients.isNotEmpty()) {
                                    append("\n📋 Ingrédients :\n")
                                    recipe.ingredients.forEach { append("• $it\n") }
                                }
                                if (recipe.steps.isNotEmpty()) {
                                    append("\n👨‍🍳 Préparation :\n")
                                    recipe.steps.forEachIndexed { i, step ->
                                        append("${i + 1}. $step\n")
                                    }
                                }
                                recipe.notes?.let { n ->
                                    if (n.isNotBlank()) append("\n💡 Notes : $n\n")
                                }
                                recipe.sourceUrl?.let { append("\nSource : $it") }
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                                putExtra(Intent.EXTRA_SUBJECT, recipe.title.normTitle())
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Partager la recette")
                            )
                        },
                        onDelete = { recipeToDelete = recipe }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeGridCard(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        elevation = 2.dp
    ) {
        Column {
            // Image 16:9 with 3-dot overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_recipe_placeholder),
                    error = painterResource(R.drawable.ic_recipe_placeholder),
                    fallback = painterResource(R.drawable.ic_recipe_placeholder),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                )

                // 3-dot button overlaid on image
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .clickable { menuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Actions",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = { menuExpanded = false; onView() }) {
                            Text("Voir la recette")
                        }
                        DropdownMenuItem(onClick = { menuExpanded = false; onEdit() }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                Text("Modifier")
                            }
                        }
                        DropdownMenuItem(onClick = { menuExpanded = false; onShare() }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Text("Partager")
                            }
                        }
                        Divider()
                        DropdownMenuItem(onClick = { menuExpanded = false; onDelete() }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = DeleteRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Supprimer", color = DeleteRed)
                            }
                        }
                    }
                }
            }

            // Title + category
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Text(
                    text = recipe.title.normTitle(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF1F2937),
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(2.dp))
                val cat = recipe.category
                Text(
                    text = if (cat != null) "${categoryEmoji(cat)} $cat"
                           else "${recipe.ingredients.size} ingrédients",
                    fontSize = 11.sp,
                    color = ColorInactiveText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
