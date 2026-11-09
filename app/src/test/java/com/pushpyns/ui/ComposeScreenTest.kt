package com.pushpyns.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.pushpyns.domain.model.PynSort
import com.pushpyns.samplePyn
import com.pushpyns.ui.paywall.PaywallScreen
import com.pushpyns.ui.pyns.PynListRow
import com.pushpyns.ui.pyns.PynListScreen
import com.pushpyns.ui.pyns.PynListUiState
import com.pushpyns.ui.theme.PushpynsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class ComposeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pynListShowsPlacesAndSearchField() {
        compose.setContent {
            PushpynsTheme {
                PynListScreen(
                    state =
                    PynListUiState(
                        query = "",
                        sort = PynSort.NAME,
                        pyns =
                        listOf(
                            samplePyn(1, "Ferry Building"),
                            samplePyn(2, "Yosemite View"),
                        ),
                        loading = false,
                    ),
                    rows =
                    listOf(
                        PynListRow.Item(samplePyn(1, "Ferry Building")),
                        PynListRow.Item(samplePyn(2, "Yosemite View")),
                    ),
                    onMenu = {},
                    onQuery = {},
                    onSort = {},
                    onToggle = {},
                    onPyn = {},
                    onCreate = {},
                    snackbar = androidx.compose.material3.SnackbarHostState(),
                )
            }
        }
        compose.onNodeWithText("Ferry Building").assertIsDisplayed()
        compose.onNodeWithText("Yosemite View").assertIsDisplayed()
        compose.onNodeWithText("Search pyns").assertIsDisplayed()
    }

    @Test
    fun paywallEnableIsThePreviewSwitch() {
        var enabled = false
        compose.setContent {
            PushpynsTheme {
                PaywallScreen(isPro = false, onBack = {}, onEnable = { enabled = true }, onDisable = {})
            }
        }
        compose.onNodeWithText("Enable Pro preview").assertIsDisplayed().performClick()
        assertThat(enabled).isTrue()
        compose.onNodeWithText("Planned pricing", substring = true).assertExists()
        compose.onNodeWithText("not a purchase", substring = true).assertExists()
    }

    @Test
    fun searchFieldAcceptsInput() {
        var query = ""
        compose.setContent {
            PushpynsTheme {
                PynListScreen(
                    state = PynListUiState(loading = false, pyns = emptyList()),
                    rows = emptyList(),
                    onMenu = {},
                    onQuery = { query = it },
                    onSort = {},
                    onToggle = {},
                    onPyn = {},
                    onCreate = {},
                    snackbar = androidx.compose.material3.SnackbarHostState(),
                )
            }
        }
        compose.onNodeWithText("Search pyns").performTextInput("dome")
        assertThat(query).isEqualTo("dome")
    }
}
