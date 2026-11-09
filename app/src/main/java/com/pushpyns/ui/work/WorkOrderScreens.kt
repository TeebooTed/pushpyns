package com.pushpyns.ui.work

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pushpyns.R
import com.pushpyns.domain.geo.formatAddress
import com.pushpyns.domain.money.Money
import com.pushpyns.domain.pdf.WorkOrderPdfComposer
import com.pushpyns.domain.terms.TermsCopy
import com.pushpyns.domain.time.formatDateTime
import com.pushpyns.domain.validation.WorkOrderFieldError
import com.pushpyns.ui.components.EmptyState
import com.pushpyns.ui.components.SignaturePad
import com.pushpyns.ui.components.renderSignaturePng
import com.pushpyns.ui.pyns.EditorEvent
import com.pushpyns.ui.pyns.PhotoStrip
import com.pushpyns.ui.util.sharePdf
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
fun WorkOrderListRoute(
    onMenu: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: WorkOrderListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WorkOrderListScreen(state, onMenu, viewModel::onQuery, viewModel::onFilter, onCreate, onOpen)
}

@Composable
fun WorkOrderListScreen(
    state: WorkListUiState,
    onMenu: () -> Unit,
    onQuery: (String) -> Unit,
    onFilter: (OrderFilter) -> Unit,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.work_orders)) },
                navigationIcon = {
                    IconButton(onClick = onMenu) {
                        Icon(Icons.Outlined.Menu, contentDescription = stringResource(R.string.open_menu))
                    }
                },
            )
        },
        floatingActionButton = {
            Button(onClick = onCreate) { Text(stringResource(R.string.new_work_order)) }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_orders)) },
                singleLine = true,
            )
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filter == OrderFilter.OPEN,
                    onClick = { onFilter(OrderFilter.OPEN) },
                    label = { Text(stringResource(R.string.work_open)) },
                )
                FilterChip(
                    selected = state.filter == OrderFilter.COMPLETE,
                    onClick = { onFilter(OrderFilter.COMPLETE) },
                    label = { Text(stringResource(R.string.work_complete_tab)) },
                )
            }
            when {
                state.loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                state.orders.isEmpty() ->
                    EmptyState(
                        title =
                        stringResource(
                            if (state.filter ==
                                OrderFilter.OPEN
                            ) {
                                R.string.work_empty_open
                            } else {
                                R.string.work_empty_done
                            },
                        ),
                        body = stringResource(R.string.work_empty_open_body),
                    )
                else ->
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        state.orders.forEach { order ->
                            Card(
                                onClick = { onOpen(order.id) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(order.name, style = MaterialTheme.typography.titleMedium)
                                    Text(order.customerName, color = MaterialTheme.colorScheme.primary)
                                    val place = formatAddress(order.address, order.city, order.state, order.zip)
                                    if (place.isNotBlank()) Text(place, style = MaterialTheme.typography.bodySmall)
                                    order.scheduledAt?.let {
                                        Text(formatDateTime(it), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(88.dp))
                    }
            }
        }
    }
}

@Composable
fun WorkOrderEditorRoute(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onContinue: (Long) -> Unit,
    viewModel: WorkOrderEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            viewModel.onCaptureResult(ok)
        }
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.prepareCapture()?.let { camera.launch(Uri.parse(it)) }
        }
    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) viewModel.importPhoto(uri.toString())
        }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Saved -> if (event.continueFlow) onContinue(event.id) else onSaved(event.id)
                is EditorEvent.Message -> snackbar.showSnackbar(event.text)
            }
        }
    }
    WorkOrderEditorScreen(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onJob = viewModel::onJob,
        onCustomer = viewModel::onCustomer,
        onDescription = viewModel::onDescription,
        onNotes = viewModel::onNotes,
        onAddress = viewModel::onAddress,
        onCity = viewModel::onCity,
        onState = viewModel::onState,
        onZip = viewModel::onZip,
        onLat = viewModel::onLat,
        onLng = viewModel::onLng,
        onSchedule = viewModel::onSchedule,
        onLink = viewModel::onLinkPyn,
        onSaveAsPyn = viewModel::onSaveAsPyn,
        onAddLine = viewModel::addLine,
        onRemoveLine = viewModel::removeLine,
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
            gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemovePhoto = viewModel::removePhoto,
        onSave = { viewModel.save(continueToTerms = false) },
        onContinue = { viewModel.save(continueToTerms = true) },
    )
}

@Composable
fun WorkOrderEditorScreen(
    state: WorkEditorUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onJob: (String) -> Unit,
    onCustomer: (String) -> Unit,
    onDescription: (String) -> Unit,
    onNotes: (String) -> Unit,
    onAddress: (String) -> Unit,
    onCity: (String) -> Unit,
    onState: (String) -> Unit,
    onZip: (String) -> Unit,
    onLat: (String) -> Unit,
    onLng: (String) -> Unit,
    onSchedule: (Long?) -> Unit,
    onLink: (Long?) -> Unit,
    onSaveAsPyn: (Boolean) -> Unit,
    onAddLine: (String, String, String) -> Unit,
    onRemoveLine: (Long) -> Unit,
    onUseLocation: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onSave: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showDate by remember { mutableStateOf(false) }
    var showLine by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (state.editing) R.string.work_order else R.string.new_work_order))
                },
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
            OrderField(
                state.jobName,
                onJob,
                stringResource(R.string.job_name),
                state.errors.errorText(WorkOrderFieldError.JOB_REQUIRED),
            )
            OrderField(
                state.customer,
                onCustomer,
                stringResource(R.string.customer),
                state.errors.errorText(WorkOrderFieldError.CUSTOMER_REQUIRED),
            )
            OrderField(state.description, onDescription, stringResource(R.string.field_description), single = false)
            OrderField(state.notes, onNotes, stringResource(R.string.field_notes), single = false)
            PynLinker(state.pyns.map { it.id to it.name }, state.linkedPynId, onLink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = state.saveAsPyn, onCheckedChange = onSaveAsPyn, enabled = state.linkedPynId == null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.save_as_pyn))
            }
            OrderField(state.address, onAddress, stringResource(R.string.field_address))
            OrderField(state.city, onCity, stringResource(R.string.field_city))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderField(
                    state.stateName,
                    onState,
                    stringResource(R.string.field_state),
                    modifier = Modifier.weight(1f),
                )
                OrderField(state.zip, onZip, stringResource(R.string.field_zip), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderField(
                    state.latitude,
                    onLat,
                    stringResource(R.string.field_lat),
                    modifier = Modifier.weight(1f),
                    error = state.errors.errorText(WorkOrderFieldError.LATITUDE, WorkOrderFieldError.LAT_LNG_PAIR),
                )
                OrderField(
                    state.longitude,
                    onLng,
                    stringResource(R.string.field_lng),
                    modifier = Modifier.weight(1f),
                    error = state.errors.errorText(WorkOrderFieldError.LONGITUDE),
                )
            }
            OutlinedButton(onClick = onUseLocation) {
                Text(stringResource(if (state.locating) R.string.locating else R.string.use_location))
            }
            Text(
                state.scheduledAt?.let { formatDateTime(it) } ?: stringResource(R.string.not_scheduled),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }) { Text(stringResource(R.string.pick_date)) }
                if (state.scheduledAt != null) {
                    TextButton(onClick = { onSchedule(null) }) { Text(stringResource(R.string.clear_schedule)) }
                }
            }
            Text(stringResource(R.string.line_items), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.optional_lines), style = MaterialTheme.typography.bodySmall)
            if (state.errors.contains(WorkOrderFieldError.LINE_ITEM)) {
                Text(stringResource(R.string.line_invalid), color = MaterialTheme.colorScheme.error)
            }
            state.lines.forEach { line ->
                val parsed =
                    com.pushpyns.domain.validation.parsedLine(
                        com.pushpyns.domain.validation
                            .LineItemInput(line.description, line.quantity, line.unitPrice),
                    )
                val total =
                    parsed?.let { Money.formatCents(Money.lineTotalCents(it.quantity, it.unitPriceCents)) } ?: "—"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line.description.ifBlank { stringResource(R.string.line_desc) })
                        Text(
                            "${line.quantity} × ${line.unitPrice} = $total",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    IconButton(onClick = { onRemoveLine(line.key) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.remove_line))
                    }
                }
            }
            TextButton(onClick = { showLine = true }) { Text(stringResource(R.string.add_line)) }
            val grand =
                state.lines
                    .mapNotNull {
                        com.pushpyns.domain.validation.parsedLine(
                            com.pushpyns.domain.validation
                                .LineItemInput(it.description, it.quantity, it.unitPrice),
                        )
                    }.sumOf { Money.lineTotalCents(it.quantity, it.unitPriceCents) }
            if (grand > 0) Text(stringResource(R.string.grand_total) + " " + Money.formatCents(grand))
            Text(stringResource(R.string.photos), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCamera) {
                    Icon(Icons.Outlined.AddAPhoto, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_camera))
                }
                OutlinedButton(onClick = onGallery) {
                    Icon(Icons.Outlined.PhotoLibrary, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_gallery))
                }
            }
            if (state.photoPaths.isNotEmpty()) {
                PhotoStrip(state.photoPaths.map { File(context.filesDir, it) }) { file ->
                    state.photoPaths.firstOrNull { file.absolutePath.endsWith(it) }?.let(onRemovePhoto)
                }
            }
            Button(onClick = onSave, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_draft))
            }
            Button(onClick = onContinue, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_and_complete))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showDate) {
        val picker = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { onSchedule(localNineAm(it)) }
                    showDate = false
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = picker) }
    }
    if (showLine) {
        LineDialog(
            onDismiss = { showLine = false },
            onSave = { d, q, p ->
                onAddLine(d, q, p)
                showLine = false
            },
        )
    }
}

@Composable
private fun PynLinker(options: List<Pair<Long, String>>, selected: Long?, onLink: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.first == selected }?.second ?: stringResource(R.string.no_pyn)
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.link_pyn)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.no_pyn)) },
                onClick = {
                    onLink(null)
                    open = false
                },
            )
            options.forEach { (id, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = {
                    onLink(id)
                    open = false
                })
            }
        }
    }
}

@Composable
private fun LineDialog(onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var description by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_line)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text(stringResource(R.string.line_desc)) },
                )
                OutlinedTextField(qty, { qty = it }, label = { Text(stringResource(R.string.qty)) })
                OutlinedTextField(price, { price = it }, label = { Text(stringResource(R.string.price)) })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(description, qty, price) },
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun OrderField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    error: String? = null,
    modifier: Modifier = Modifier,
    single: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = single,
        minLines = if (single) 1 else 3,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
    )
}

@Composable
private fun Set<WorkOrderFieldError>.errorText(vararg keys: WorkOrderFieldError): String? {
    val hit = keys.firstOrNull { it in this } ?: return null
    return stringResource(
        when (hit) {
            WorkOrderFieldError.JOB_REQUIRED -> R.string.job_required
            WorkOrderFieldError.CUSTOMER_REQUIRED -> R.string.customer_required
            WorkOrderFieldError.LATITUDE -> R.string.invalid_lat
            WorkOrderFieldError.LONGITUDE -> R.string.invalid_lng
            WorkOrderFieldError.LAT_LNG_PAIR -> R.string.lat_lng_pair
            WorkOrderFieldError.LINE_ITEM -> R.string.line_invalid
        },
    )
}

private fun localNineAm(pickerMillis: Long): Long {
    val date = Instant.ofEpochMilli(pickerMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return date
        .atTime(9, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
}

@Composable
fun WorkOrderDetailRoute(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onComplete: (Long) -> Unit,
    onPyn: (Long) -> Unit,
    viewModel: WorkOrderDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event == "deleted") onBack() else snackbar.showSnackbar(event)
        }
    }
    val order = state.order
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.work_order)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    if (order != null) {
                        IconButton(onClick = { confirm = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.delete))
                        }
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
            order == null ->
                EmptyState(
                    stringResource(R.string.work_missing),
                    stringResource(R.string.work_empty_open_body),
                    Modifier.padding(padding),
                )
            else ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(order.name, style = MaterialTheme.typography.headlineSmall)
                    Text(order.customerName, color = MaterialTheme.colorScheme.primary)
                    Text(
                        if (order.status == com.pushpyns.domain.model.WorkOrderStatus.COMPLETE) {
                            stringResource(R.string.status_complete)
                        } else {
                            stringResource(R.string.status_open)
                        },
                    )
                    Text(formatAddress(order.address, order.city, order.state, order.zip))
                    if (order.description.isNotBlank()) Text(order.description)
                    if (order.notes.isNotBlank()) Text(order.notes)
                    order.scheduledAt?.let { Text(formatDateTime(it)) }
                    order.linkedPynName?.let { name ->
                        TextButton(onClick = { order.pynId?.let(onPyn) }) {
                            Text(stringResource(R.string.view_linked_pyn) + ": " + name)
                        }
                    }
                    order.lineItems.forEach { item ->
                        val line = Money.lineTotalCents(item.quantity, item.unitPriceCents)
                        Text("${item.description}  ${Money.displayQuantity(item.quantity)}  ${Money.formatCents(line)}")
                    }
                    val total = WorkOrderPdfComposer().grandTotalCents(order)
                    if (total > 0) Text(stringResource(R.string.grand_total) + " " + Money.formatCents(total))
                    if (order.photos.isNotEmpty()) {
                        PhotoStrip(order.photos.map { File(context.filesDir, it.relativePath) })
                    }
                    order.termsAcceptedAt?.let {
                        Text(
                            stringResource(R.string.terms_accepted) + " " + formatDateTime(it),
                        )
                    }
                    order.completedAt?.let { Text(stringResource(R.string.completed_label) + " " + formatDateTime(it)) }
                    if (order.status == com.pushpyns.domain.model.WorkOrderStatus.OPEN) {
                        OutlinedButton(onClick = { onEdit(order.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.edit))
                        }
                        Button(onClick = { onComplete(order.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.mark_complete))
                        }
                    } else {
                        Button(
                            onClick = {
                                val path = order.pdfPath
                                val file = path?.let { File(context.filesDir, it) }
                                if (file != null && file.exists()) {
                                    context.sharePdf(file, context.getString(R.string.pdf_subject))
                                } else {
                                    viewModel.recreatePdf()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.share_pdf)) }
                        OutlinedButton(onClick = viewModel::recreatePdf, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.recreate_pdf))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.delete_work_title)) },
            text = { Text(stringResource(R.string.delete_work_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
fun TermsAcceptRoute(onBack: () -> Unit, onAccepted: (Long) -> Unit, viewModel: WorkOrderCompleteViewModel = hiltViewModel()) {
    val order by viewModel.order.collectAsStateWithLifecycle()
    var checked by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.terms_title)) },
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            order?.let {
                Text(it.customerName, style = MaterialTheme.typography.titleMedium)
                Text(it.name)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.terms_intro))
            TermsCopy.sections.forEach { section ->
                Spacer(Modifier.height(12.dp))
                Text(section.title, style = MaterialTheme.typography.titleSmall)
                Text(section.body)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                Text(stringResource(R.string.customer_agrees))
            }
            Button(
                onClick = { viewModel.acceptTerms { onAccepted(viewModel.workOrderId) } },
                enabled = checked,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.agree_continue)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SignatureRoute(onBack: () -> Unit, onDone: (Long) -> Unit, viewModel: WorkOrderCompleteViewModel = hiltViewModel()) {
    val state by viewModel.signature.collectAsStateWithLifecycle()
    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var size by remember { mutableStateOf(IntSize(1080, 600)) }
    LaunchedEffect(Unit) { viewModel.done.collect { onDone(it) } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.signature_title)) },
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
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.signing_for))
            Text("${state.customer} · ${state.job}", style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.signature_help), style = MaterialTheme.typography.bodySmall)
            SignaturePad(strokes = strokes, onStrokesChange = { strokes = it }, onSized = { size = it })
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { strokes = emptyList() }) { Text(stringResource(R.string.clear)) }
                Button(
                    onClick = {
                        if (strokes.isEmpty() || size.width == 0) return@Button
                        viewModel.completeWith(renderSignaturePng(strokes, size.width, size.height))
                    },
                    enabled = strokes.isNotEmpty() && !state.saving,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.complete_pdf)) }
            }
        }
    }
}

@Composable
fun SignedRoute(onDone: () -> Unit, viewModel: WorkOrderDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val order = state.order
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.signed_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.signed_body))
            order?.let {
                Text(it.customerName, style = MaterialTheme.typography.titleLarge)
                Text(it.name)
                it.completedAt?.let { at -> Text(formatDateTime(at)) }
                val total = WorkOrderPdfComposer().grandTotalCents(it)
                if (total > 0) Text(Money.formatCents(total))
            }
            Button(
                onClick = {
                    val path = order?.pdfPath ?: return@Button
                    val file = File(context.filesDir, path)
                    if (file.exists()) context.sharePdf(file, context.getString(R.string.pdf_subject))
                },
                enabled = order?.pdfPath != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.share_pdf)) }
            OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.back_to_orders))
            }
        }
    }
}
