package com.masheqal.app.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.masheqal.app.R
import com.masheqal.app.domain.QiblaCalculator
import com.masheqal.app.util.CurrentLocation
import com.masheqal.app.util.LocationUtils
import com.masheqal.app.util.LocationChoiceStore
import com.masheqal.app.util.PlaceLookup
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun QiblaScreen(nav: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var location by remember { mutableStateOf(LocationChoiceStore.loadManual(context) ?: LocationUtils.lastKnown(context)) }
    var placeLabel by remember { mutableStateOf(location?.placeName) }
    var showCityPicker by remember { mutableStateOf(false) }
    var azimuth by remember { mutableStateOf<Float?>(null) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }
    var locationRefreshing by remember { mutableStateOf(false) }
    var locationRefreshFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val refreshLocation: () -> Unit = remember(context, scope) {
        {
            scope.launch {
                locationRefreshing = true
                try {
                    val fresh = runCatching { LocationUtils.current(context) }.getOrNull()
                    val cached = fresh ?: LocationUtils.lastKnown(context)
                    if (cached != null) {
                        LocationChoiceStore.clearManual(context)
                        location = cached
                    } else {
                        location = LocationChoiceStore.loadManual(context)
                    }
                    locationRefreshFailed = cached == null && location == null
                } finally {
                    locationRefreshing = false
                }
            }
        }
    }

    val requestLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        ) {
            refreshLocation()
        } else {
            location = LocationChoiceStore.loadManual(context) ?: LocationUtils.lastKnown(context)
            locationRefreshFailed = location == null
        }
    }

    LaunchedEffect(Unit) {
        val manual = LocationChoiceStore.loadManual(context)
        if (manual != null) location = manual else refreshLocation()
    }

    LaunchedEffect(location?.latitude, location?.longitude, location?.placeName) {
        val current = location
        placeLabel = current?.placeName
        if (current != null && current.placeName.isNullOrBlank()) {
            placeLabel = runCatching {
                PlaceLookup.reverseGeocode(context, current.latitude, current.longitude)?.displayName
            }.getOrNull()
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotation = FloatArray(9)
                val adjustedRotation = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)

                @Suppress("DEPRECATION")
                val displayRotation = (
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                ).defaultDisplay.rotation
                val axes = when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(rotation, axes.first, axes.second, adjustedRotation)
                SensorManager.getOrientation(adjustedRotation, orientation)
                azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
                sensorAccuracy = value
            }
        }
        if (sensor != null) sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val bearing = location?.let { QiblaCalculator.bearingFrom(it.latitude, it.longitude) }
    val distanceKm = location?.let { QiblaCalculator.distanceFromKm(it.latitude, it.longitude) }
    val magneticDeclination = remember(location) {
        location?.let {
            GeomagneticField(
                it.latitude.toFloat(), it.longitude.toFloat(), 0f, System.currentTimeMillis()
            ).declination.toDouble()
        } ?: 0.0
    }
    val delta = if (bearing != null && azimuth != null) {
        QiblaCalculator.signedDeltaFromMagneticHeading(
            trueBearingDegrees = bearing,
            magneticAzimuthDegrees = azimuth!!.toDouble(),
            magneticDeclinationDegrees = magneticDeclination
        )
    } else null
    val animatedDelta by animateFloatAsState(
        targetValue = (delta ?: 0.0).toFloat(),
        label = "qibla-arrow-angle"
    )
    val dialRotation = -((azimuth ?: 0f).toDouble() + magneticDeclination).toFloat()
    val sensorAccuracyText = when (sensorAccuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> R.string.qibla_accuracy_high
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> R.string.qibla_accuracy_medium
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> R.string.qibla_accuracy_low
        else -> R.string.qibla_accuracy_unreliable
    }
    val currentLocation = location

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }
            Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.qibla),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { refreshLocation() }, enabled = !locationRefreshing) {
                if (locationRefreshing) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp)
                else Icon(
                    Icons.Default.MyLocation,
                    contentDescription = stringResource(R.string.qibla_refresh_location)
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        if (currentLocation == null) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Default.LocationOn, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)
                    )
                    Text(stringResource(R.string.location_needed), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.location_ready),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            requestLocation.launch(
                                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.set_location)) }
                    TextButton(onClick = { showCityPicker = true }) {
                        Text(stringResource(R.string.choose_city))
                    }
                    if (locationRefreshFailed) {
                        Text(
                            stringResource(R.string.location_refresh_failed),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else if (azimuth == null) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        Icons.Default.Explore, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)
                    )
                    Text(stringResource(R.string.sensor_unavailable), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.qibla_compass_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.qibla_bearing_value, bearing ?: 0.0),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (distanceKm != null) Text(stringResource(R.string.qibla_distance_km, distanceKm))
                }
            }
        } else if (bearing != null && delta != null) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.qibla),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                        Surface(
                            Modifier.size(274.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 5.dp
                        ) {}
                        Box(
                            Modifier.size(246.dp).border(
                                1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.48f), CircleShape
                            )
                        )
                        Box(Modifier.size(214.dp).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        Box(Modifier.fillMaxSize().rotate(dialRotation), contentAlignment = Alignment.Center) {
                            Text(
                                "N",
                                Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "E",
                                Modifier.align(Alignment.CenterEnd).padding(end = 25.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "S",
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "W",
                                Modifier.align(Alignment.CenterStart).padding(start = 25.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            Modifier.size(184.dp).background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.40f), CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = stringResource(R.string.qibla),
                                modifier = Modifier.size(102.dp).rotate(animatedDelta),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                Modifier.size(12.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondary,
                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.surface)
                            ) {}
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "${abs(delta).toInt()}°",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        when {
                            abs(delta) < 3 -> stringResource(R.string.aligned)
                            delta > 0 -> stringResource(R.string.turn_right)
                            else -> stringResource(R.string.turn_left)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.qibla_bearing_value, bearing),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (distanceKm != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.qibla_distance_km, distanceKm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.66f)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    stringResource(sensorAccuracyText),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(R.string.qibla_compass_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        currentLocation?.let { current ->
            Spacer(Modifier.height(12.dp))
            LocationQualityCard(
                location = current,
                placeName = placeLabel,
                refreshFailed = locationRefreshFailed,
                refreshing = locationRefreshing,
                onRefresh = refreshLocation
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showCityPicker) {
        CityPickerDialog(
            onDismiss = { showCityPicker = false },
            onPlaceSelected = { selected ->
                location = LocationChoiceStore.saveManual(context, selected)
                placeLabel = selected.displayName
                locationRefreshFailed = false
                showCityPicker = false
            }
        )
    }
}

@Composable
private fun LocationQualityCard(
    location: CurrentLocation,
    placeName: String?,
    refreshFailed: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (location.isPrecise) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.qibla_location_quality),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onRefresh, enabled = !refreshing) {
                    Text(stringResource(R.string.location_refresh))
                }
            }
            Spacer(Modifier.height(5.dp))
            Text(
                placeName ?: stringResource(R.string.location_city_unavailable),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                if (location.isManual) stringResource(R.string.location_manual_accuracy)
                else stringResource(
                    if (location.isPrecise) R.string.location_accuracy
                    else R.string.location_accuracy_approximate,
                    location.accuracyMeters.toInt().coerceAtLeast(1)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (location.isPrecise) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.error
            )
            if (refreshFailed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.location_refresh_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
