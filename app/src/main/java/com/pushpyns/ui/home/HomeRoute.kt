package com.pushpyns.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pushpyns.R
import com.pushpyns.domain.share.locationShareText
import com.pushpyns.ui.util.shareText

@Composable
fun HomeRoute(
    onMenu: () -> Unit,
    onNewPyn: (Double?, Double?) -> Unit,
    onPyn: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val permission =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            val granted =
                result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            viewModel.onPermissionResult(granted)
        }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }
    Box(Modifier.fillMaxSize()) {
        HomeScreen(
            state = state,
            onMenu = onMenu,
            onNewPyn = { onNewPyn(state.origin?.latitude, state.origin?.longitude) },
            onPynClick = onPyn,
            onEnableLocation = {
                permission.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            },
            onShareLocation = {
                context.shareText(
                    locationShareText(state.addressLine, state.origin),
                    context.getString(R.string.share_location),
                )
            },
            onLoadSamples = viewModel::loadSamples,
        )
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
