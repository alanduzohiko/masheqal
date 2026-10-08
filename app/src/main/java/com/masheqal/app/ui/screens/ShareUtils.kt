package com.masheqal.app.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

fun shareText(context: Context, text: String) {
    val chooser = Intent.createChooser(
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        },
        null
    )
    ContextCompat.startActivity(context, chooser, null)
}
