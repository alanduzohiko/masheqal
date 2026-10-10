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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.masheqal.app.util.LocationUtils
import kotlinx.coroutines.launch
import com.masheqal.app.data.SettingsState
import com.masheqal.app.ui.MasheqalTheme
import com.masheqal.app.ui.MasheqalRoot

class MainActivity : ComponentActivity() {
    private val locationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            lifecycleScope.launch { LocationUtils.current(this@MainActivity) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by (application as MasheqalApp).settings.state.collectAsState(initial = SettingsState())
            LaunchedEffect(settings.language) { applyLanguage(settings.language) }
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
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            locationLauncher.launch(permissions)
        } else {
            lifecycleScope.launch { LocationUtils.current(this) }
        }
    }

    private fun applyLanguage(code: String) {
        val locales = LocaleListCompat.forLanguageTags(code)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
