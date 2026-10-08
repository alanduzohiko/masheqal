package com.masheqal.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.R

@Composable
fun AdhkarScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, null) }
            Icon(Icons.Default.Spa, null, Modifier.padding(top = 14.dp))
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.adhkar), style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(12.dp))
        MissingContentScreen(
            title = stringResource(R.string.adhkar),
            details = stringResource(R.string.adhkar_package_required),
            onBack = { nav.popBackStack() }
        )
    }
}
