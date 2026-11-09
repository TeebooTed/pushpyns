package com.pushpyns.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pushpyns.R
import com.pushpyns.domain.model.UseType
import com.pushpyns.ui.components.PushpynsMark

@Composable
fun ProfileRoute(editing: Boolean, onDone: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(editing) { viewModel.load(editing) }
    LaunchedEffect(Unit) { viewModel.saved.collect { onDone() } }
    ProfileScreen(
        state = state,
        onName = viewModel::onName,
        onBusiness = viewModel::onBusiness,
        onUseType = viewModel::onUseType,
        onSave = viewModel::save,
    )
}

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onName: (String) -> Unit,
    onBusiness: (String) -> Unit,
    onUseType: (UseType) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PushpynsMark(Modifier.height(96.dp).fillMaxWidth(0.45f), hole = MaterialTheme.colorScheme.background)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(if (state.editing) R.string.edit_profile else R.string.profile_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(R.string.tagline),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.profile_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.name,
            onValueChange = onName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.your_name)) },
            singleLine = true,
            isError = state.nameError,
            supportingText =
            if (state.nameError) {
                { Text(stringResource(R.string.name_required)) }
            } else {
                null
            },
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.businessName,
            onValueChange = onBusiness,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.business_name)) },
            supportingText = { Text(stringResource(R.string.business_name_hint)) },
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.use_for), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        val options = listOf(UseType.PERSONAL, UseType.BUSINESS, UseType.BOTH)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.useType == type,
                    onClick = { onUseType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) {
                    Text(
                        stringResource(
                            when (type) {
                                UseType.PERSONAL -> R.string.use_personal
                                UseType.BUSINESS -> R.string.use_business
                                UseType.BOTH -> R.string.use_both
                            },
                        ),
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onSave,
            enabled = !state.saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(stringResource(R.string.continue_label))
        }
        Spacer(Modifier.height(24.dp))
    }
}
