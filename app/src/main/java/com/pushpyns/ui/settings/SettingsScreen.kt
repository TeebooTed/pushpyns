package com.pushpyns.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pushpyns.BuildConfig
import com.pushpyns.R
import com.pushpyns.domain.model.ThemeMode
import com.pushpyns.domain.terms.TermsCopy

@Composable
fun SettingsRoute(
    onMenu: () -> Unit,
    onEditProfile: () -> Unit,
    onPaywall: () -> Unit,
    onTerms: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onMenu = onMenu,
        onTheme = viewModel::setTheme,
        onDynamic = viewModel::setDynamic,
        onEditProfile = onEditProfile,
        onPaywall = onPaywall,
        onTerms = onTerms,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onMenu: () -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDynamic: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    onPaywall: () -> Unit,
    onTerms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.open_menu))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text(state.profile?.name ?: stringResource(R.string.edit_profile)) },
                supportingContent = {
                    Text(state.profile?.businessName?.ifBlank { stringResource(R.string.local_only) } ?: "")
                },
                trailingContent = {
                    androidx.compose.material3.TextButton(onClick = onEditProfile) {
                        Text(stringResource(R.string.edit))
                    }
                },
            )
            Text(
                stringResource(R.string.appearance),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip(R.string.theme_system, state.themeMode == ThemeMode.SYSTEM) { onTheme(ThemeMode.SYSTEM) }
                ThemeChip(R.string.theme_light, state.themeMode == ThemeMode.LIGHT) { onTheme(ThemeMode.LIGHT) }
                ThemeChip(R.string.theme_dark, state.themeMode == ThemeMode.DARK) { onTheme(ThemeMode.DARK) }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.dynamic_color_body)) },
                trailingContent = { Switch(checked = state.dynamicColor, onCheckedChange = onDynamic) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.pro_preview)) },
                supportingContent = {
                    Text(stringResource(if (state.pro) R.string.paywall_enabled else R.string.paywall_off))
                },
                trailingContent = {
                    androidx.compose.material3.TextButton(onClick = onPaywall) {
                        Text(stringResource(R.string.pro_badge))
                    }
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.nav_terms)) },
                modifier = Modifier,
                trailingContent = {
                    androidx.compose.material3.TextButton(
                        onClick = onTerms,
                    ) { Text(stringResource(R.string.nav_terms)) }
                },
            )
            Text(
                stringResource(R.string.about),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.about_body),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ThemeChip(label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(label)) })
}

@Composable
fun TermsRoute(onMenu: (() -> Unit)?, onBack: (() -> Unit)?) {
    ScaffoldTerms(onMenu, onBack)
}

@Composable
fun ScaffoldTerms(onMenu: (() -> Unit)?, onBack: (() -> Unit)?) {
    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terms_title)) },
                navigationIcon = {
                    if (onMenu != null) {
                        IconButton(onClick = onMenu) {
                            Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.open_menu))
                        }
                    } else if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.navigate_back),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(stringResource(R.string.terms_intro))
            TermsCopy.sections.forEach { section ->
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
                Text(section.body)
            }
        }
    }
}
