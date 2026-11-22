package com.example.recipelibrary.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight

/** Mode de l'onglet « Ajouter » (spec 005). */
enum class AddMode(val label: String) { URL("URL"), FILE("Fichier"), PHOTO("Photo") }

private val Selected = Color(0xFF4A16C6)
private val Unselected = Color(0xFF625B70)

/**
 * Onglet « Ajouter » (contracts/navigation-ui.md) : sélecteur URL / Fichier / Photo au-dessus des
 * trois écrans d'import, inchangés. L'écran URL reste toujours composé (masqué à taille nulle hors
 * du mode URL) pour qu'un import en cours ne soit pas annulé quand on change de mode.
 */
@Composable
fun AddTabScreen(
    mode: AddMode,
    onModeChange: (AddMode) -> Unit,
    url: @Composable () -> Unit,
    file: @Composable () -> Unit,
    photo: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = mode.ordinal,
            backgroundColor = Color.White,
            contentColor = Selected,
            modifier = Modifier.fillMaxWidth().testTag("add_mode_selector")
        ) {
            AddMode.values().forEach { m ->
                Tab(
                    selected = m == mode,
                    onClick = { onModeChange(m) },
                    selectedContentColor = Selected,
                    unselectedContentColor = Unselected,
                    modifier = Modifier.testTag("add_mode_${m.name.lowercase()}"),
                    text = { Text(m.label, fontWeight = if (m == mode) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }
        Box(Modifier.fillMaxSize().testTag("add_mode_content")) {
            // URL : toujours composé ; hors mode URL, taille nulle et invisible pour l'accessibilité et les tests
            val hidden = mode != AddMode.URL
            Box(
                if (hidden) Modifier.layout { measurable, _ ->
                    measurable.measure(androidx.compose.ui.unit.Constraints.fixed(0, 0))
                    layout(0, 0) {}
                }.clearAndSetSemantics { } else Modifier.fillMaxSize()
            ) { url() }
            when (mode) {
                AddMode.FILE -> file()
                AddMode.PHOTO -> photo()
                AddMode.URL -> Unit
            }
        }
    }
}
