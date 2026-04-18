package io.shahinhasanov.broker.ui.declarations

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shahinhasanov.broker.data.model.Declaration
import io.shahinhasanov.broker.data.model.DeclarationLine
import io.shahinhasanov.broker.data.model.DeclarationStatus
import io.shahinhasanov.broker.data.model.StatusEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclarationDetailScreen(
    onBack: () -> Unit,
    onOpenApproval: (String) -> Unit,
    viewModel: DeclarationDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val declaration = state.declaration

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(declaration?.reference ?: "Declaration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (declaration == null) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text(if (state.isRefreshing) "Loading..." else "Not found") }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HeaderCard(declaration) }
            item { Text("Line items", style = MaterialTheme.typography.titleMedium) }
            items(declaration.lines, key = DeclarationLine::lineNumber) { LineRow(it) }
            item { Text("Attachments", style = MaterialTheme.typography.titleMedium) }
            if (declaration.attachments.isEmpty()) {
                item { Text("None", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(declaration.attachments) { Text("- $it", style = MaterialTheme.typography.bodyMedium) }
            }
            item { Text("History", style = MaterialTheme.typography.titleMedium) }
            items(declaration.history) { HistoryRow(it) }
            if (declaration.status == DeclarationStatus.DRAFT) {
                item {
                    Button(
                        onClick = { onOpenApproval(declaration.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Review for approval") }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(declaration: Declaration) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(declaration.reference, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                StatusChip(declaration.status)
            }
            Spacer(Modifier.height(8.dp))
            Text("Declarant: ${declaration.declarantName}", style = MaterialTheme.typography.bodyLarge)
            Text("Operator: ${declaration.operator}", style = MaterialTheme.typography.bodyMedium)
            Text("Lines: ${declaration.lineCount}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LineRow(line: DeclarationLine) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("${line.lineNumber}. ${line.description}", style = MaterialTheme.typography.bodyLarge)
        Text("HS ${line.hsCode} - ${line.originCountry}", style = MaterialTheme.typography.bodyMedium)
        Text("${line.quantity} ${line.unit} - ${line.customsValue} ${line.currency}",
            style = MaterialTheme.typography.labelMedium)
        HorizontalDivider()
    }
}

@Composable
private fun HistoryRow(event: StatusEvent) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        StatusChip(event.status)
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(event.actor, style = MaterialTheme.typography.bodyMedium)
            Text(event.occurredAt.toString(), style = MaterialTheme.typography.labelMedium)
            event.note?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
