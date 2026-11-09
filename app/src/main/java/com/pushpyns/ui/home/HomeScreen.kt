package com.pushpyns.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pushpyns.R
import com.pushpyns.domain.geo.distanceMeters
import com.pushpyns.domain.geo.formatDistance
import com.pushpyns.domain.geo.formattedAddress
import com.pushpyns.ui.components.MapPin
import com.pushpyns.ui.components.PushpynsMark
import com.pushpyns.ui.components.PynMap

@Composable
fun HomeScreen(
    state: HomeUiState,
    onMenu: () -> Unit,
    onNewPyn: () -> Unit,
    onPynClick: (Long) -> Unit,
    onEnableLocation: () -> Unit,
    onShareLocation: () -> Unit,
    onLoadSamples: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        PynMap(
            pins = state.pyns.map { MapPin(it.id, it.latitude, it.longitude) },
            origin = state.origin,
            modifier = Modifier.fillMaxSize(),
            onPinClick = onPynClick,
        )
        Surface(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.open_menu))
                }
                PushpynsMark(Modifier.height(28.dp).width(22.dp), hole = MaterialTheme.colorScheme.surface)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.home_current), style = MaterialTheme.typography.labelMedium)
                    Text(
                        state.addressLine.ifBlank { stringResource(R.string.home_location_off) },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                    )
                }
                IconButton(onClick = onShareLocation, enabled = state.origin != null) {
                    Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share_location))
                }
                IconButton(onClick = onEnableLocation) {
                    Icon(Icons.Outlined.MyLocation, contentDescription = stringResource(R.string.home_enable_location))
                }
            }
        }
        Card(
            modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                if (state.access != LocationAccess.Granted) {
                    Text(stringResource(R.string.home_location_body), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = onEnableLocation) {
                        Text(stringResource(R.string.home_enable_location))
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Button(onClick = onNewPyn, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_new_pyn))
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.home_nearby), style = MaterialTheme.typography.titleSmall)
                if (state.pyns.isEmpty() && !state.loading) {
                    Text(
                        stringResource(R.string.home_empty_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButtonLike(stringResource(R.string.home_sample), onLoadSamples)
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.pyns.take(12), key = { it.id }) { pyn ->
                            val meters = distanceMeters(pyn, state.origin)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier =
                                Modifier
                                    .width(180.dp)
                                    .clickable { onPynClick(pyn.id) },
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(pyn.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                    Text(
                                        pyn.formattedAddress().ifBlank { stringResource(R.string.no_coordinates) },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                    )
                                    if (meters != null) {
                                        Text(formatDistance(meters), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TextButtonLike(label: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text(label) }
}
