package com.masheqal.app.ui.screens

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

@Composable
fun shareText(text: String) {
    val context = LocalContext.current
    val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,text) }, null)
    ContextCompat.startActivity(context, chooser, null)
}
