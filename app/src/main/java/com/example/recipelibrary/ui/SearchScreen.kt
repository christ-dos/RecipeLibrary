package com.example.recipelibrary.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recipelibrary.R
import com.example.recipelibrary.data.Recipe

private fun String.normSearchTitle(): String =
    if (length > 1 && this == this.uppercase()) lowercase().replaceFirstChar { it.uppercase() } else this

private val FALLBACK_TAGS = listOf(
    "Chocolat", "Rapide", "Végétarien", "Healthy", "Sans gluten", "Batch cooking"
)

internal fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) for (j in 1..b.length) {
        dp[i][j] = if (a[i - 1] == b[j - 1]) dp[i - 1][j - 1]
                   else 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
    }
    return dp[a.length][b.length]
}

private fun String.normalize(): String =
    java.text.Normalizer.normalize(trim().lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "")
        .replace("-", " ")
        .replace(Regex("\\s+"), " ")

internal fun bestSuggestion(query: String, recipes: List<Recipe>): String? {
    val q = query.normalize()
    if (q.length < 3) return null
    val tol = if (q.length < 6) 1 else 2

    data class Candidate(val display: String, val dist: Int, val priority: Int)

    val seen = mutableSetOf<String>()
    val candidates = mutableListOf<Candidate>()

    fun tryAdd(display: String, priority: Int) {
        val key = display.normalize()
        if (key.length < 3 || key == q || key in seen) return
        seen.add(key)
        val dist = levenshtein(q, key)
        if (dist in 1..tol) candidates.add(Candidate(display, dist, priority))
    }

    recipes.mapNotNull { it.category }.distinct().forEach { tryAdd(it, 0) }
    recipes.flatMap { it.tags }.distinct().forEach { tryAdd(it, 1) }
    recipes.flatMap { it.ingredients }.forEach { ingr ->
        ingr.split(" ").forEach { w ->
            val clean = w.trim().trimEnd('.', ',', '!', '?')
            if (clean.length >= 3) tryAdd(clean, 2)
        }
    }
    recipes.forEach { r ->
        r.title.split(" ").forEach { w ->
            val clean = w.trim().trimEnd('.', ',', '!', '?')
            if (clean.length >= 3) tryAdd(clean, 3)
        }
    }

    if (candidates.isEmpty()) return null
    val sorted = candidates.sortedWith(compareBy({ it.priority }, { it.dist }))
    val best = sorted.first()
    val second = sorted.getOrNull(1)
    if (second != null && second.priority == best.priority && second.dist == best.dist) return null
    return best.display
}

@Composable
fun SearchScreen(
    recipes: List<Recipe>,
    onRecipeClick: (Recipe) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val recentSearches = remember { mutableStateListOf<String>() }
    val focusRequester = remember { FocusRequester() }

    val categories = recipes.mapNotNull { it.category }.distinct().sorted()

    // Filtres rapides = tags les plus utilisés, avec fallback sur la liste prédéfinie
    val quickFilters = recipes
        .flatMap { it.tags }
        .filter { it.isNotBlank() }
        .groupingBy { it }
        .eachCount()
        .entries
        .sortedByDescending { it.value }
        .take(6)
        .map { it.key }
        .ifEmpty { FALLBACK_TAGS }

    val results = if (query.isBlank()) emptyList()
    else {
        val q = query.trim().lowercase()
        recipes.filter { r ->
            r.title.lowercase().contains(q) ||
            r.category?.lowercase()?.contains(q) == true ||
            r.tags.any { it.lowercase().contains(q) } ||
            r.ingredients.any { it.lowercase().contains(q) } ||
            r.steps.any { it.lowercase().contains(q) }
        }
    }

    val suggestion = remember(query, recipes.size) {
        if (results.isEmpty()) bestSuggestion(query, recipes) else null
    }

    fun addToRecent(q: String) {
        val t = q.trim()
        if (t.isBlank()) return
        recentSearches.remove(t)
        recentSearches.add(0, t)
        while (recentSearches.size > 5) recentSearches.removeAt(recentSearches.size - 1)
    }

    fun applySearch(q: String) {
        query = q
        addToRecent(q)
    }

    Column(modifier = Modifier.fillMaxSize().background(ColorScreenBg)) {

        // ── Search bar ────────────────────────────────────────────────────────
        Column(modifier = Modifier.background(ColorScreenBg)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(Color.White, RoundedCornerShape(16.dp))
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text("Titre, ingrédient, tag…", color = ColorInactiveText, fontSize = 15.sp)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = ColorTitleViolet,
                            modifier = Modifier.size(26.dp))
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, null, tint = ColorInactiveText)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        backgroundColor = Color.Transparent,
                        cursorColor = ColorTitleViolet
                    ),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
            if (suggestion != null) {
                SpellSuggestionBanner(
                    suggestion = suggestion,
                    onApply = { applySearch(it) },
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                "Recherche dans le titre, les ingrédients, les tags et la catégorie",
                fontSize = 11.sp,
                color = ColorInactiveText,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 8.dp)
            )
        }

        // ── Browse mode ──────────────────────────────────────────────────────
        if (query.isBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Parcourir par catégorie
                if (categories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Parcourir par catégorie",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ColorTitleViolet
                        )
                        categories.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                row.forEach { cat ->
                                    CategoryTileSearch(
                                        category = cat,
                                        onClick = { applySearch(cat) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                // Filtres rapides (tags des vraies recettes)
                if (quickFilters.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Filtres rapides",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ColorTitleViolet
                        )
                        quickFilters.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { tag ->
                                    QuickFilterChip(
                                        tag = tag,
                                        onClick = { applySearch(tag) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }

                // Recherches récentes
                if (recentSearches.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Recherches récentes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = ColorTitleViolet
                        )
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            backgroundColor = Color.White,
                            elevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                recentSearches.forEachIndexed { i, search ->
                                    if (i > 0) Divider(color = Color(0xFFF3F4F6))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { applySearch(search) }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.History, null,
                                            tint = ColorInactiveText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            search,
                                            modifier = Modifier.weight(1f),
                                            fontSize = 14.sp,
                                            color = Color(0xFF374151)
                                        )
                                        Icon(
                                            Icons.Default.KeyboardArrowRight, null,
                                            tint = ColorInactiveText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        // ── Results mode ─────────────────────────────────────────────────────
        } else if (results.isEmpty() && suggestion == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Aucune recette ne correspond à votre recherche.",
                    color = ColorInactiveText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else if (results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(results, key = { it.id }) { r ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clickable {
                                addToRecent(query)
                                onRecipeClick(r)
                            },
                        elevation = 2.dp,
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = Color.White
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = r.imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.ic_recipe_placeholder),
                                error = painterResource(R.drawable.ic_recipe_placeholder),
                                fallback = painterResource(R.drawable.ic_recipe_placeholder),
                                modifier = Modifier
                                    .width(80.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            )
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    r.title.normSearchTitle(),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF1F2937)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (r.category != null) "${categoryEmoji(r.category)} ${r.category}"
                                    else "${r.ingredients.size} ingrédient(s)",
                                    fontSize = 12.sp,
                                    color = ColorInactiveText,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
internal fun SpellSuggestionBanner(
    suggestion: String,
    onApply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClickLabel = "Utiliser la suggestion $suggestion") { onApply(suggestion) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search_suggestion),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(48.dp)
        )
        Text("Vous vouliez dire : ", fontSize = 13.sp, color = ColorInactiveText)
        Text(
            suggestion,
            fontSize = 13.sp,
            color = ColorTitleViolet,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun CategoryTileSearch(
    category: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White,
        elevation = 2.dp,
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            Text(categoryEmoji(category), fontSize = 34.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                category,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = ColorTitleViolet,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun QuickFilterChip(
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                Icons.Default.Label, null,
                tint = ColorTitleViolet,
                modifier = Modifier.size(13.dp)
            )
            Text(
                tag,
                fontSize = 12.sp,
                color = Color(0xFF374151),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
