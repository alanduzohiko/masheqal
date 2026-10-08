
package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.R

private data class AdhkarEntry(
    val title: Int,
    val subtitle: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AdhkarScreen(nav: NavHostController) {
    val entries = listOf(
        AdhkarEntry(R.string.morning, R.string.source_required, Icons.Default.WbSunny),
        AdhkarEntry(R.string.evening, R.string.source_required, Icons.Default.NightsStay),
        AdhkarEntry(R.string.hisn, R.string.source_required, Icons.Default.Shield),
        AdhkarEntry(R.string.dua, R.string.source_required, Icons.Default.FavoriteBorder)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 26.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.adhkar),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        stringResource(R.string.adhkar_package_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalIconButton(onClick = { nav.navigate("content") }) {
                    Icon(
                        Icons.Default.Inventory2,
                        stringResource(R.string.content_center)
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Card(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        stringResource(R.string.content_center),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.content_policy),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { nav.navigate("content") }) {
                        Text(stringResource(R.string.content_center))
                    }
                }
            }
        }

        item {
            SectionTitle(stringResource(R.string.adhkar))
        }

        items(entries) { entry ->
            FeatureCard(
                title = stringResource(entry.title),
                subtitle = stringResource(entry.subtitle),
                icon = entry.icon,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                nav.navigate("content")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
