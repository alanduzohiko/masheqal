package com.masheqal.app.ui.screens

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.SVG
import com.masheqal.app.R
import com.masheqal.app.data.MushafAyahRegion
import com.masheqal.app.data.MushafPageStore
import com.masheqal.app.data.MushafViewBox
import kotlinx.coroutines.CancellationException

private data class MushafUiState(
    val svg: SVG? = null,
    val viewBox: MushafViewBox? = null,
    val ayahRegions: List<MushafAyahRegion> = emptyList(),
    val loading: Boolean = true,
    val fromCache: Boolean = false,
    val failed: Boolean = false
)

@Composable
internal fun MushafPageImage(
    page: Int,
    modifier: Modifier = Modifier,
    onAyahSelected: (surah: Int, ayah: Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current.applicationContext
    val latestOnAyahSelected by rememberUpdatedState(onAyahSelected)
    var retry by remember(page) { mutableIntStateOf(0) }
    var state by remember(page) { mutableStateOf(MushafUiState()) }

    LaunchedEffect(page, retry) {
        state = MushafUiState(loading = true)
        try {
            val loaded = MushafPageStore.load(context, page)
            state = MushafUiState(
                svg = loaded.svg,
                viewBox = loaded.viewBox,
                ayahRegions = loaded.ayahRegions,
                loading = false,
                fromCache = loaded.fromCache
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            state = MushafUiState(loading = false, failed = true)
        }
    }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val svg = state.svg
        val highlightColor = MaterialTheme.colorScheme.primary
        if (svg != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(345f / 550f)
                    .pointerInput(page, state.viewBox, state.ayahRegions) {
                        detectTapGestures { tap ->
                            val box = state.viewBox ?: return@detectTapGestures
                            val documentPoint = box.mapCanvasPoint(
                                tap.x,
                                tap.y,
                                size.width.toFloat(),
                                size.height.toFloat()
                            ) ?: return@detectTapGestures
                            val hit = state.ayahRegions.firstOrNull {
                                it.contains(documentPoint.x, documentPoint.y)
                            }
                            hit?.let {
                                latestOnAyahSelected(it.surahNumber, it.ayahNumber)
                            }
                        }
                    }
            ) {
                drawIntoCanvas { canvas ->
                    svg.renderToCanvas(
                        canvas.nativeCanvas,
                        RectF(0f, 0f, size.width, size.height)
                    )
                }
            }
            Text(
                text = stringResource(
                    if (state.fromCache) R.string.mushaf_available_offline
                    else R.string.mushaf_saved_offline
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (state.ayahRegions.isEmpty()) {
                Text(
                    stringResource(R.string.mushaf_ayah_actions_need_connection),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp)
                )
                TextButton(onClick = { retry++ }) {
                    Text(stringResource(R.string.retry))
                }
            }
        } else if (state.loading) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.mushaf_loading),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else if (state.failed) {
            Text(
                text = stringResource(R.string.mushaf_load_error),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { retry++ }, modifier = Modifier.padding(top = 12.dp)) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
