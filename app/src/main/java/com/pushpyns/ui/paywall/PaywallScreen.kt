package com.pushpyns.ui.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pushpyns.R
import com.pushpyns.ui.components.PushpynsMark

@Composable
fun PaywallRoute(onBack: () -> Unit, onUnlocked: () -> Unit, viewModel: PaywallViewModel = hiltViewModel()) {
    val pro by viewModel.isPro.collectAsStateWithLifecycle()
    PaywallScreen(
        isPro = pro,
        onBack = onBack,
        onEnable = {
            viewModel.setEnabled(true)
            onUnlocked()
        },
        onDisable = { viewModel.setEnabled(false) },
    )
}

@Composable
fun PaywallScreen(isPro: Boolean, onBack: () -> Unit, onEnable: () -> Unit, onDisable: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.paywall_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PushpynsMark(Modifier.height(84.dp).fillMaxWidth(0.4f), hole = MaterialTheme.colorScheme.background)
            Text(stringResource(R.string.paywall_body), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(R.string.paywall_price))
            Text(
                stringResource(if (isPro) R.string.paywall_enabled else R.string.paywall_off),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.paywall_not_purchase), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            if (isPro) {
                OutlinedButton(onClick = onDisable, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.paywall_disable))
                }
            } else {
                Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.paywall_enable))
                }
            }
        }
    }
}
