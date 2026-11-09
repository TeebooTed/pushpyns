package com.pushpyns.ui.pyns

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.pushpyns.R
import com.pushpyns.domain.geo.formatCoordinates
import com.pushpyns.domain.geo.formatDistance
import com.pushpyns.domain.geo.formattedAddress
import com.pushpyns.domain.model.PynSort
import com.pushpyns.domain.share.pynShareText
import com.pushpyns.domain.time.formatDateTime
import com.pushpyns.domain.time.relativeTime
import com.pushpyns.domain.validation.PynFieldError
import com.pushpyns.ui.components.EmptyState
import com.pushpyns.ui.components.MapPin
import com.pushpyns.ui.components.PynMap
import com.pushpyns.ui.util.openDirections
import com.pushpyns.ui.util.shareText
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun PynListRoute(onMenu: () -> Unit, onPyn: (Long) -> Unit, onCreate: () -> Unit, viewModel: PynListViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    PynListScreen(
        state = state,
        rows = viewModel.rows(),
        onMenu = onMenu,
        onQuery = viewModel::onQuery,
        onSort = viewModel::onSort,
        onToggle = viewModel::toggle,
        onPyn = onPyn,
        onCreate = onCreate,
        snackbar = snackbar,
    )
}

@Composable
fun PynListScreen(
    state: PynListUiState,
    rows: List<PynListRow>,
    onMenu: () -> Unit,
    onQuery: (String) -> Unit,
    onSort: (PynSort) -> Unit,
    onToggle: (Long) -> Unit,
    onPyn: (Long) -> Unit,
    onCreate: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pyns_title)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.open_menu))
                    }
                },
                actions = {
                    IconButton(onClick = onCreate) {
                        Icon(Icons.Outlined.AddAPhoto, contentDescription = stringResource(R.string.new_pyn))
                    }
                },
            )
        },
        floatingActionButton = {
            Button(onClick = onCreate) { Text(stringResource(R.string.new_pyn)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                label = { Text(stringResource(R.string.search_pyns)) },
                singleLine = true,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SortChip(R.string.sort_category, state.sort == PynSort.CATEGORY) { onSort(PynSort.CATEGORY) }
                SortChip(R.string.sort_name, state.sort == PynSort.NAME) { onSort(PynSort.NAME) }
                SortChip(R.string.sort_distance, state.sort == PynSort.DISTANCE) { onSort(PynSort.DISTANCE) }
                SortChip(R.string.sort_date, state.sort == PynSort.DATE) { onSort(PynSort.DATE) }
            }
            if (state.sort == PynSort.DISTANCE && !state.locationAvailable) {
                Text(
                    stringResource(R.string.distance_needs_location),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            when {
                state.loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                state.pyns.isEmpty() ->
                    EmptyState(
                        title =
                        if (state.query.isBlank()) {
                            stringResource(R.string.home_empty_title)
                        } else {
                            stringResource(R.string.empty_search)
                        },
                        body = stringResource(R.string.home_empty_body),
                    )
                else ->
                    LazyColumn(contentPadding = PaddingValues(bottom = 88.dp)) {
                        items(rows, key = { row ->
                            when (row) {
                                is PynListRow.Header -> "h-${row.title}"
                                is PynListRow.Item -> "p-${row.pyn.id}"
                            }
                        }) { row ->
                            when (row) {
                                is PynListRow.Header ->
                                    Text(
                                        row.title,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                is PynListRow.Item ->
                                    PynRow(
                                        pynName = row.pyn.name,
                                        subtitle = row.pyn.formattedAddress(),
                                        whenLabel = relativeTime(row.pyn.createdAt, nowMillis),
                                        distance = state.distances[row.pyn.id]?.let(::formatDistance),
                                        expanded = row.pyn.id in state.expanded,
                                        description = row.pyn.description,
                                        notes = row.pyn.notes,
                                        coordinates =
                                        row.pyn.latitude?.let { lat ->
                                            row.pyn.longitude?.let { lng -> formatCoordinates(lat, lng) }
                                        },
                                        categories = row.pyn.categories.joinToString(", ") { it.name },
                                        onToggle = { onToggle(row.pyn.id) },
                                        onOpen = { onPyn(row.pyn.id) },
                                    )
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun SortChip(label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(label)) })
}

@Composable
private fun PynRow(
    pynName: String,
    subtitle: String,
    whenLabel: String,
    distance: String?,
    expanded: Boolean,
    description: String,
    notes: String,
    coordinates: String?,
    categories: String,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(pynName, style = MaterialTheme.typography.titleMedium)
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(whenLabel, style = MaterialTheme.typography.labelMedium)
                    if (distance != null) Text(distance, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription =
                        stringResource(
                            if (expanded) R.string.hide_details else R.string.show_details,
                        ),
                    )
                }
            }
            if (expanded) {
                if (categories.isNotBlank()) Text(categories, color = MaterialTheme.colorScheme.primary)
                if (description.isNotBlank()) Text(description, modifier = Modifier.padding(top = 4.dp))
                if (notes.isNotBlank()) Text(notes, modifier = Modifier.padding(top = 4.dp))
                if (coordinates != null) {
                    Text(
                        coordinates,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun PynDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onWorkOrder: (Long) -> Unit,
    viewModel: PynDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    val noMaps = stringResource(R.string.directions_unavailable)
    LaunchedEffect(Unit) { viewModel.deleted.collect { onBack() } }
    PynDetailScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onEdit = { onEdit(viewModel.pynId) },
        onDelete = { confirmDelete = true },
        onShare = {
            val pyn = state.pyn ?: return@PynDetailScreen
            context.shareText(pynShareText(pyn), context.getString(R.string.share_pyn))
        },
        onDirections = {
            val pyn = state.pyn ?: return@PynDetailScreen
            val lat = pyn.latitude
            val lng = pyn.longitude
            if (lat == null || lng == null) return@PynDetailScreen
            val opened = context.openDirections(lat, lng, pyn.name)
            if (!opened) scope.launch { snackbar.showSnackbar(noMaps) }
        },
        onWorkOrder = { onWorkOrder(viewModel.pynId) },
    )
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_pyn_title)) },
            text = { Text(stringResource(R.string.delete_pyn_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
fun PynDetailScreen(
    state: PynDetailUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onDirections: () -> Unit,
    onWorkOrder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pyn = state.pyn
    var menu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pyn_detail)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShare, enabled = pyn != null) {
                        Icon(Icons.Outlined.Share, contentDescription = stringResource(R.string.share))
                    }
                    IconButton(onClick = { menu = true }, enabled = pyn != null) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.more))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit)) },
                            onClick = {
                                menu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            onClick = {
                                menu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading ->
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            pyn == null ->
                EmptyState(
                    title = stringResource(R.string.pyn_missing),
                    body = stringResource(R.string.home_empty_body),
                    modifier = Modifier.padding(padding),
                )
            else ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    PynMap(
                        pins = listOf(MapPin(pyn.id, pyn.latitude, pyn.longitude)),
                        origin = null,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                    Column(Modifier.padding(16.dp)) {
                        Text(pyn.name, style = MaterialTheme.typography.headlineSmall)
                        if (pyn.categories.isNotEmpty()) {
                            Text(
                                pyn.categories.joinToString(" · ") { it.name },
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (pyn.description.isNotBlank()) Text(pyn.description)
                        if (pyn.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.field_notes), style = MaterialTheme.typography.labelLarge)
                            Text(pyn.notes)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.field_address), style = MaterialTheme.typography.labelLarge)
                        Text(pyn.formattedAddress().ifBlank { stringResource(R.string.no_coordinates) })
                        val lat = pyn.latitude
                        val lng = pyn.longitude
                        if (lat != null && lng != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.coordinates), style = MaterialTheme.typography.labelLarge)
                            Text(formatCoordinates(lat, lng))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.saved_label) + " " + formatDateTime(pyn.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (pyn.photos.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            PhotoStrip(pyn.photos.map { File(context.filesDir, it.relativePath) })
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onDirections,
                            enabled = lat != null && lng != null,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.directions)) }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onWorkOrder, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.create_work_order))
                        }
                    }
                }
        }
    }
}

@Composable
fun PhotoStrip(files: List<File>, onRemove: ((File) -> Unit)? = null) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        files.forEach { file ->
            Box {
                AsyncImage(
                    model = file,
                    contentDescription = stringResource(R.string.cd_photo),
                    modifier = Modifier.size(96.dp),
                    contentScale = ContentScale.Crop,
                )
                if (onRemove != null) {
                    IconButton(onClick = { onRemove(file) }, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.remove_photo))
                    }
                }
            }
        }
    }
}

@Composable
fun PynEditorRoute(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onManageCategories: () -> Unit,
    viewModel: PynEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            viewModel.onCaptureResult(success)
        }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viewModel.prepareCapture()?.let { camera.launch(Uri.parse(it)) }
            }
        }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) viewModel.importPhoto(uri.toString())
        }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Saved -> onSaved(event.id)
                is EditorEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }
    PynEditorScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onName = viewModel::onName,
        onDescription = viewModel::onDescription,
        onNotes = viewModel::onNotes,
        onAddress = viewModel::onAddress,
        onCity = viewModel::onCity,
        onState = viewModel::onState,
        onZip = viewModel::onZip,
        onLat = viewModel::onLat,
        onLng = viewModel::onLng,
        onToggleCategory = viewModel::toggleCategory,
        onUseLocation = viewModel::useCurrentLocation,
        onCamera = {
            val granted =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) {
                viewModel.prepareCapture()?.let { camera.launch(Uri.parse(it)) }
            } else {
                permission.launch(Manifest.permission.CAMERA)
            }
        },
        onGallery = {
            gallery.launch(
                androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onRemovePhoto = viewModel::removePhoto,
        onManageCategories = onManageCategories,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PynEditorScreen(
    state: PynEditorUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onDescription: (String) -> Unit,
    onNotes: (String) -> Unit,
    onAddress: (String) -> Unit,
    onCity: (String) -> Unit,
    onState: (String) -> Unit,
    onZip: (String) -> Unit,
    onLat: (String) -> Unit,
    onLng: (String) -> Unit,
    onToggleCategory: (Long) -> Unit,
    onUseLocation: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onManageCategories: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.titleIsEdit) R.string.edit_pyn else R.string.new_pyn)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSave, enabled = !state.saving) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Field(
                state.name,
                onName,
                stringResource(R.string.field_name),
                error = state.errors.errorText(PynFieldError.NAME_REQUIRED),
            )
            Field(state.description, onDescription, stringResource(R.string.field_description), singleLine = false)
            Field(state.notes, onNotes, stringResource(R.string.field_notes), singleLine = false)
            Field(state.address, onAddress, stringResource(R.string.field_address))
            Field(state.city, onCity, stringResource(R.string.field_city))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(state.stateName, onState, stringResource(R.string.field_state), modifier = Modifier.weight(1f))
                Field(state.zip, onZip, stringResource(R.string.field_zip), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(
                    state.latitude,
                    onLat,
                    stringResource(R.string.field_lat),
                    modifier = Modifier.weight(1f),
                    number = true,
                    error = state.errors.errorText(PynFieldError.LATITUDE, PynFieldError.LAT_LNG_PAIR),
                )
                Field(
                    state.longitude,
                    onLng,
                    stringResource(R.string.field_lng),
                    modifier = Modifier.weight(1f),
                    number = true,
                    error = state.errors.errorText(PynFieldError.LONGITUDE),
                )
            }
            OutlinedButton(onClick = onUseLocation, enabled = !state.locating) {
                Text(stringResource(if (state.locating) R.string.locating else R.string.use_location))
            }
            Text(stringResource(R.string.field_categories), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.categories.forEach { category ->
                    FilterChip(
                        selected = category.id in state.selected,
                        onClick = { onToggleCategory(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }
            TextButton(onClick = onManageCategories) { Text(stringResource(R.string.manage_categories)) }
            Text(stringResource(R.string.photos), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCamera) {
                    Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_camera))
                }
                OutlinedButton(onClick = onGallery) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_gallery))
                }
            }
            if (state.photoPaths.isNotEmpty()) {
                PhotoStrip(
                    files = state.photoPaths.map { File(context.filesDir, it) },
                    onRemove = { file ->
                        val match = state.photoPaths.firstOrNull { file.absolutePath.endsWith(it) }
                        if (match != null) onRemovePhoto(match)
                    },
                )
            }
            Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Field(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    number: Boolean = false,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
    )
}

@Composable
private fun Set<PynFieldError>.errorText(vararg keys: PynFieldError): String? {
    val hit = keys.firstOrNull { it in this } ?: return null
    return stringResource(
        when (hit) {
            PynFieldError.NAME_REQUIRED -> R.string.name_required
            PynFieldError.LATITUDE -> R.string.invalid_lat
            PynFieldError.LONGITUDE -> R.string.invalid_lng
            PynFieldError.LAT_LNG_PAIR -> R.string.lat_lng_pair
        },
    )
}
