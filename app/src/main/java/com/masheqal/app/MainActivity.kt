package com.masheqal.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.masheqal.app.data.SettingsState
import com.masheqal.app.ui.MasheqalTheme
import com.masheqal.app.ui.MasheqalRoot

class MainActivity : ComponentActivity() {
    private val locationLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            val savedLanguage = (application as MasheqalApp).settings.state.first().language
            applyLanguage(savedLanguage)
        }
        setContent {
            val settings by (application as MasheqalApp).settings.state.collectAsState(initial = SettingsState())
            MasheqalTheme(theme = settings.theme) {
                if (settings.keepScreenAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                MasheqalRoot(
                    app = application as MasheqalApp,
                    intent = intent,
                    onRequestLocation = { requestLocation() },
                    onLanguage = { code -> applyLanguage(code) }
                )
            }
        }
    }

    private fun requestLocation() {
        val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) locationLauncher.launch(permissions)
    }

    private fun applyLanguage(code: String) {
        val locales = LocaleListCompat.forLanguageTags(com.masheqal.app.data.normalizeAppLanguage(code))
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
