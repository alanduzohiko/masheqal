package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.masheqal.app.R
import com.masheqal.app.util.PlaceLookup
import com.masheqal.app.util.PlaceMatch
import kotlinx.coroutines.launch

@Composable
fun CityPickerDialog(onDismiss: () -> Unit, onPlaceSelected: (PlaceMatch) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<PlaceMatch>()) }
    var searching by remember { mutableStateOf(false) }
    var didSearch by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_city)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.city_search_hint))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.search_city)) },
                    singleLine = true
                )
                Button(
                    onClick = {
                        scope.launch {
                            searching = true
                            didSearch = true
                            results = PlaceLookup.searchByName(context, query)
                            searching = false
                        }
                    },
                    enabled = query.isNotBlank() && !searching,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.search_city_action)) }
                if (searching) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) { CircularProgressIndicator() }
                }
                if (didSearch && results.isEmpty() && !searching) {
                    Text(stringResource(R.string.city_not_found))
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(results, key = { "${it.displayName}:${it.latitude}:${it.longitude}" }) { place ->
                        TextButton(
                            onClick = { onPlaceSelected(place) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(place.displayName)
                                Text(
                                    stringResource(R.string.choose_city_result_hint),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
