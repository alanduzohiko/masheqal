
package com.masheqal.app.ui

import android.content.Intent
import android.content.ComponentName
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.masheqal.app.services.QuranPlaybackService
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.ui.screens.*

private data class NavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable
fun MasheqalRoot(
    app: MasheqalApp,
    intent: Intent?,
    onRequestLocation: () -> Unit,
    onLanguage: (String) -> Unit
) {
    val nav = rememberNavController()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val items = listOf(
        NavItem("home", Icons.Default.Home, androidx.compose.ui.res.stringResource(R.string.home)),
        NavItem("quran", Icons.Default.MenuBook, androidx.compose.ui.res.stringResource(R.string.quran)),
        NavItem("prayer", Icons.Default.Schedule, androidx.compose.ui.res.stringResource(R.string.prayer)),
        NavItem("adhkar", Icons.Default.Spa, androidx.compose.ui.res.stringResource(R.string.adhkar)),
        NavItem("library", Icons.Default.LibraryBooks, androidx.compose.ui.res.stringResource(R.string.library))
    )
    val current by nav.currentBackStackEntryAsState()
    val baseRoute = (current?.destination?.route ?: "home").substringBefore("/")
    val showBar = items.any { it.route == baseRoute }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                GlobalMiniPlayer(nav)
                if (showBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 5.dp
                    ) {
                        items.forEach { item ->
                            NavigationBarItem(
                                selected = item.route == baseRoute,
                                onClick = {
                                    nav.navigate(item.route) {
                                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(animationSpec = tween(220)) +
                    slideInHorizontally(
                        initialOffsetX = { width -> if (isRtl) -width / 18 else width / 18 },
                        animationSpec = tween(220)
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(150)) +
                    slideOutHorizontally(
                        targetOffsetX = { width -> if (isRtl) width / 24 else -width / 24 },
                        animationSpec = tween(150)
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(180)) +
                    slideInHorizontally(
                        initialOffsetX = { width -> if (isRtl) width / 18 else -width / 18 },
                        animationSpec = tween(180)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(140)) +
                    slideOutHorizontally(
                        targetOffsetX = { width -> if (isRtl) -width / 24 else width / 24 },
                        animationSpec = tween(140)
                    )
            }
        ) {
            composable("home") { HomeScreen(app, nav, onRequestLocation) }
            composable("audio") { QuranAudioScreen(app, nav) }
            composable("quran") { QuranScreen(app, nav) }
            composable(
                "quran/surah/{surah}",
                arguments = listOf(navArgument("surah") { type = NavType.IntType })
            ) { back ->
                QuranReaderScreen(app, nav, back.arguments?.getInt("surah") ?: 1, 1)
            }
            composable(
                "quran/ref/{surah}/{ayah}",
                arguments = listOf(
                    navArgument("surah") { type = NavType.IntType },
                    navArgument("ayah") { type = NavType.IntType }
                )
            ) { back ->
                QuranReaderScreen(
                    app,
                    nav,
                    back.arguments?.getInt("surah") ?: 1,
                    back.arguments?.getInt("ayah") ?: 1
                )
            }
            composable(
                "quran/page/{page}",
                arguments = listOf(navArgument("page") { type = NavType.IntType })
            ) { back ->
                QuranPageScreen(app, nav, back.arguments?.getInt("page") ?: 1)
            }
            composable("search") {
                val sharedText = if (
                    intent?.action == Intent.ACTION_SEND && intent.type == "text/plain"
                ) {
                    intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                } else ""
                SearchScreen(app, nav, sharedText)
            }
            composable("prayer") { PrayerScreen(app, nav, onRequestLocation) }
            composable("qibla") { QiblaScreen(nav) }
            composable("tasbih") { TasbihScreen(app, nav) }
            composable("adhkar") { AdhkarScreen(app, nav) }
            composable("library") { LibraryScreen(app, nav) }
            composable("saved") { SavedScreen(app, nav) }
            composable("notes") { NotesScreen(app, nav) }
            composable("khatmah") { KhatmahScreen(app, nav) }
            composable("settings") { SettingsScreen(app, nav, onLanguage) }
            composable("content") { ContentCenterScreen(app, nav) }
            composable("calendar") { CalendarScreen(nav) }
        }
    }

    LaunchedEffect(intent) {
        val uri = intent?.data
        if (uri != null) {
            when (uri.host) {
                "quran" -> {
                    val p = uri.pathSegments
                    val surah = p.getOrNull(0)?.toIntOrNull()
                    val ayah = p.getOrNull(1)?.toIntOrNull()
                    when {
                        surah != null && ayah != null -> nav.navigate("quran/ref/${surah}/${ayah}")
                        surah != null -> nav.navigate("quran/surah/${surah}")
                    }
                }
                "prayer" -> nav.navigate("prayer")
                "qibla" -> nav.navigate("qibla")
                "adhkar" -> nav.navigate("adhkar")
                "tasbih" -> nav.navigate("tasbih")
            }
        }
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            nav.navigate("search")
        }
        if (intent?.action == "OPEN_PRAYER") {
            nav.navigate("prayer")
        }
    }
}


@androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])
@Composable
private fun GlobalMiniPlayer(nav: androidx.navigation.NavHostController) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var active = true
        val token = SessionToken(context, ComponentName(context, QuranPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching { future.get() }.onSuccess { player ->
                if (active) {
                    controller = player
                    val item = player.currentMediaItem
                    title = item?.mediaMetadata?.title?.toString().orEmpty()
                    artist = item?.mediaMetadata?.artist?.toString().orEmpty()
                    isPlaying = player.isPlaying
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            active = false
            MediaController.releaseFuture(future)
            controller = null
        }
    }

    DisposableEffect(controller) {
        val player = controller
        if (player == null) {
            onDispose { }
        } else {
            fun updateMetadata(item: MediaItem?) {
                title = item?.mediaMetadata?.title?.toString().orEmpty()
                artist = item?.mediaMetadata?.artist?.toString().orEmpty()
            }
            val listener = object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateMetadata(mediaItem)
                    isPlaying = player.isPlaying
                }

                override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                    title = mediaMetadata.title?.toString().orEmpty()
                    artist = mediaMetadata.artist?.toString().orEmpty()
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    updateMetadata(player.currentMediaItem)
                    isPlaying = player.isPlaying
                }
            }
            player.addListener(listener)
            updateMetadata(player.currentMediaItem)
            isPlaying = player.isPlaying
            onDispose { player.removeListener(listener) }
        }
    }

    val currentItem = controller?.currentMediaItem
    val show = currentItem != null && !currentItem.mediaId.startsWith("adhan-")
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(animationSpec = tween(180)),
        exit = fadeOut(animationSpec = tween(140))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { nav.navigate("audio") }
                    .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Headphones, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title.ifBlank { androidx.compose.ui.res.stringResource(R.string.audio_player_title) },
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = {
                    val player = controller ?: return@IconButton
                    if (player.isPlaying) player.pause() else player.play()
                }) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = androidx.compose.ui.res.stringResource(
                            if (isPlaying) R.string.pause else R.string.audio_play
                        )
                    )
                }
                IconButton(
                    enabled = controller?.hasNextMediaItem() == true,
                    onClick = { controller?.seekToNextMediaItem() }
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = androidx.compose.ui.res.stringResource(R.string.audio_next_surah))
                }
            }
        }
    }
}
