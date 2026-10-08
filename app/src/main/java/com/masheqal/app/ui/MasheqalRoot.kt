package com.masheqal.app.ui

import android.content.Intent
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.ui.screens.*

private data class NavItem(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String)

@Composable
fun MasheqalRoot(app: MasheqalApp, intent: Intent?, onRequestLocation: () -> Unit, onLanguage: (String) -> Unit) {
    val nav = rememberNavController()
    val bottom = listOf(
        NavItem("home", Icons.Default.Home, androidx.compose.ui.res.stringResource(R.string.home)),
        NavItem("quran", Icons.Default.MenuBook, androidx.compose.ui.res.stringResource(R.string.quran)),
        NavItem("prayer", Icons.Default.Schedule, androidx.compose.ui.res.stringResource(R.string.prayer)),
        NavItem("adhkar", Icons.Default.Spa, androidx.compose.ui.res.stringResource(R.string.adhkar)),
        NavItem("library", Icons.Default.LibraryBooks, androidx.compose.ui.res.stringResource(R.string.library))
    )
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route ?: "home"
    val base = route.substringBefore("/")
    Scaffold(bottomBar = {
        if (base in bottom.map { it.route }) NavigationBar {
            bottom.forEach { item ->
                NavigationBarItem(
                    selected = base == item.route,
                    onClick = { nav.navigate(item.route) { launchSingleTop=true; restoreState=true } },
                    icon={Icon(item.icon,null)}, label={Text(item.label)}
                )
            }
        }
    }) { padding ->
        NavHost(nav, startDestination="home", modifier=Modifier.padding(padding)) {
            composable("home") { HomeScreen(app,nav,onRequestLocation) }
            composable("quran") { QuranScreen(app,nav) }
            composable("quran/surah/{surah}", arguments=listOf(navArgument("surah"){type=NavType.IntType})) { back -> QuranReaderScreen(app,nav,back.arguments?.getInt("surah") ?: 1,1) }
            composable("quran/ref/{surah}/{ayah}", arguments=listOf(navArgument("surah"){type=NavType.IntType},navArgument("ayah"){type=NavType.IntType})) { back -> QuranReaderScreen(app,nav,back.arguments?.getInt("surah") ?: 1,back.arguments?.getInt("ayah") ?: 1) }
            composable("quran/page/{page}", arguments=listOf(navArgument("page"){type=NavType.IntType})) { back -> QuranPageScreen(app,nav,back.arguments?.getInt("page") ?: 1) }
            composable("search") {
                val sharedText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty() else ""
                SearchScreen(app, nav, sharedText)
            }
            composable("prayer") { PrayerScreen(app,nav,onRequestLocation) }
            composable("qibla") { QiblaScreen(nav) }
            composable("tasbih") { TasbihScreen(app,nav) }
            composable("adhkar") { AdhkarScreen(nav) }
            composable("library") { LibraryScreen(app,nav) }
            composable("saved") { SavedScreen(app,nav) }
            composable("notes") { NotesScreen(app,nav) }
            composable("khatmah") { KhatmahScreen(app,nav) }
            composable("settings") { SettingsScreen(app,nav,onLanguage) }
            composable("content") { ContentCenterScreen(nav) }
            composable("calendar") { CalendarScreen(nav) }
        }
    }
    LaunchedEffect(intent) {
        val uri = intent?.data
        if (uri != null) {
            when (uri.host) {
                "quran" -> {
                    val p = uri.pathSegments
                    val s=p.getOrNull(0)?.toIntOrNull(); val a=p.getOrNull(1)?.toIntOrNull()
                    if (s != null && a != null) nav.navigate("quran/ref/$s/$a") else if (s != null) nav.navigate("quran/surah/$s")
                }
                "prayer" -> nav.navigate("prayer")
                "qibla" -> nav.navigate("qibla")
                "adhkar" -> nav.navigate("adhkar")
                "tasbih" -> nav.navigate("tasbih")
            }
        }
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") nav.navigate("search")
        if (intent?.action == "OPEN_PRAYER") nav.navigate("prayer")
    }
}
