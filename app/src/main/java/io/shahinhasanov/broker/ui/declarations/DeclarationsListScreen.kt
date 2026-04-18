package io.shahinhasanov.broker.ui.declarations

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.shahinhasanov.broker.data.model.Declaration
import io.shahinhasanov.broker.data.model.DeclarationStatus
import io.shahinhasanov.broker.ui.theme.StatusAccepted
import io.shahinhasanov.broker.ui.theme.StatusDraft
import io.shahinhasanov.broker.ui.theme.StatusRejected
import io.shahinhasanov.broker.ui.theme.StatusReleased
import io.shahinhasanov.broker.ui.theme.StatusSubmitted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclarationsListScreen(
    onOpenDeclaration: (String) -> Unit,
    viewModel: DeclarationsListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Declarations") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            FilterRow(selected = state.filter, onFilter = viewModel::setFilter)
            Spacer(Modifier.height(4.dp))
            if (state.declarations.isEmpty()) {
                EmptyList(isRefreshing = state.isRefreshing)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.declarations, key = Declaration::id) { declaration ->
                        DeclarationRow(
                            declaration = declaration,
                            onClick = { onOpenDeclaration(declaration.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selected: DeclarationStatus?,
    onFilter: (DeclarationStatus?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onFilter(null) },
            label = { Text("All") },
            colors = FilterChipDefaults.filterChipColors()
        )
        DeclarationStatus.values().forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onFilter(status) },
                label = { Text(status.name.lowercase().replaceFirstChar { it.titlecase() }) }
            )
        }
    }
}

@Composable
private fun DeclarationRow(declaration: Declaration, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = declaration.reference,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(declaration.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${declaration.declarantName} - ${declaration.lineCount} lines",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = declaration.hsChapterSummary,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
fun StatusChip(status: DeclarationStatus) {
    val color = when (status) {
        DeclarationStatus.DRAFT -> StatusDraft
        DeclarationStatus.SUBMITTED -> StatusSubmitted
        DeclarationStatus.ACCEPTED -> StatusAccepted
        DeclarationStatus.REJECTED -> StatusRejected
        DeclarationStatus.RELEASED -> StatusReleased
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = status.name.lowercase().replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyList(isRefreshing: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (isRefreshing) "Loading..." else "No declarations in this view.",
            color = Color.Gray
        )
    }
}
