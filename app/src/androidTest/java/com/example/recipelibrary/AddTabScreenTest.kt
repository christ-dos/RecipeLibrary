package com.example.recipelibrary

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.recipelibrary.ui.AddMode
import com.example.recipelibrary.ui.AddTabScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Onglet « Ajouter » (spec 005, T005) : sélecteur URL / Fichier / Photo, contenus factices. */
@RunWith(AndroidJUnit4::class)
class AddTabScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private fun show() {
        rule.setContent {
            var mode by remember { mutableStateOf(AddMode.URL) }
            MaterialTheme {
                AddTabScreen(
                    mode = mode,
                    onModeChange = { mode = it },
                    url = {
                        // Un état interne, comme un import en cours dans l'écran URL
                        var count by rememberSaveable { mutableStateOf(0) }
                        Column(Modifier.testTag("content_url")) {
                            Button(onClick = { count++ }, modifier = Modifier.testTag("url_counter")) { Text("URL $count") }
                        }
                    },
                    file = { Text("Fichier", Modifier.testTag("content_file")) },
                    photo = { Text("Photo", Modifier.testTag("content_photo")) }
                )
            }
        }
    }

    @Test fun opensOnUrl_withUrlSelected() {
        show()
        rule.onNodeWithTag("add_mode_url").assertIsSelected()
        rule.onNodeWithTag("add_mode_file").assertIsNotSelected()
        rule.onNodeWithTag("content_url").assertIsDisplayed()
        rule.onNodeWithTag("content_file").assertDoesNotExist()
        rule.onNodeWithTag("content_photo").assertDoesNotExist()
    }

    @Test fun switchingModes_showsTheMatchingContent_selectorAlwaysVisible() {
        show()
        rule.onNodeWithTag("add_mode_file").performClick()
        rule.onNodeWithTag("add_mode_file").assertIsSelected()
        rule.onNodeWithTag("content_file").assertIsDisplayed()
        rule.onNodeWithTag("content_photo").assertDoesNotExist()
        rule.onNodeWithTag("add_mode_selector").assertIsDisplayed()

        rule.onNodeWithTag("add_mode_photo").performClick()
        rule.onNodeWithTag("content_photo").assertIsDisplayed()
        rule.onNodeWithTag("content_file").assertDoesNotExist()
        rule.onNodeWithTag("add_mode_selector").assertIsDisplayed()
    }

    @Test fun hiddenUrlContent_isNeitherShownNorClickable() {
        show()
        rule.onNodeWithTag("add_mode_photo").performClick()
        // Masqué : retiré de l'arbre d'accessibilité et des tests, donc ni affiché ni cliquable
        rule.onNodeWithTag("content_url").assertDoesNotExist()
        rule.onNodeWithTag("url_counter").assertDoesNotExist()
    }

    @Test fun urlState_survivesARoundTripThroughOtherModes() {
        show()
        rule.onNodeWithTag("url_counter").performClick().performClick()
        rule.onNodeWithTag("url_counter").assertTextContains("URL 2")
        rule.onNodeWithTag("add_mode_file").performClick()
        rule.onNodeWithTag("add_mode_url").performClick()
        rule.onNodeWithTag("url_counter").assertTextContains("URL 2")
    }
}
