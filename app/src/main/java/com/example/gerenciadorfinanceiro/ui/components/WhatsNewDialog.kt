package com.example.gerenciadorfinanceiro.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gerenciadorfinanceiro.domain.model.ReleaseNote

@Composable
fun WhatsNewDialog(
    releaseNote: ReleaseNote,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.NewReleases,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Novidades da versão ${releaseNote.version}")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (releaseNote.features.isNotEmpty()) {
                    ReleaseSection(
                        title = "Novidades",
                        items = releaseNote.features,
                        bulletColor = MaterialTheme.colorScheme.primary
                    )
                }

                if (releaseNote.fixes.isNotEmpty()) {
                    ReleaseSection(
                        title = "Correções",
                        items = releaseNote.fixes,
                        bulletColor = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (releaseNote.improvements.isNotEmpty()) {
                    ReleaseSection(
                        title = "Melhorias",
                        items = releaseNote.improvements,
                        bulletColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Entendi")
            }
        }
    )
}

@Composable
private fun ReleaseSection(
    title: String,
    items: List<String>,
    bulletColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = bulletColor
        )
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "•",
                    color = bulletColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
