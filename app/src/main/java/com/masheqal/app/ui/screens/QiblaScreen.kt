
package com.masheqal.app.ui.screens

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
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
import kotlinx.coroutines.launch
import com.masheqal.app.domain.QiblaCalculator
import com.masheqal.app.util.LocationUtils
import kotlin.math.abs

@Composable
fun QiblaScreen(nav: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var location by remember { mutableStateOf(LocationUtils.lastKnown(context)) }
    var azimuth by remember { mutableStateOf<Float?>(null) }
    var accuracy by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val request = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scope.launch {
            location = LocationUtils.current(context) ?: LocationUtils.lastKnown(context)
        }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotation = FloatArray(9)
                val displayAdjustedRotation = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)

                // Remap device axes so the compass arrow stays correct in portrait and landscape.
                val displayRotation = (
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                ).defaultDisplay.rotation
                val (axisX, axisY) = when (displayRotation) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                SensorManager.remapCoordinateSystem(
                    rotation,
                    axisX,
                    axisY,
                    displayAdjustedRotation
                )
                SensorManager.getOrientation(displayAdjustedRotation, orientation)
                azimuth = ((Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, value: Int) {
                accuracy = value
            }
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val bearing = location?.let {
        QiblaCalculator.bearingFrom(it.latitude, it.longitude)
    }
    val delta = if (bearing != null && azimuth != null) {
        ((bearing - azimuth!! + 540) % 360) - 180
    } else {
        null
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Icon(Icons.Default.Explore, null)
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.qibla),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Spacer(Modifier.height(18.dp))

        if (location == null) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    IconBadge(Icons.Default.LocationOn, emphasized = true)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.location_needed),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.location_ready),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            request.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.set_location))
                    }
                }
            }
        } else if (azimuth == null) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    IconBadge(Icons.Default.Explore, emphasized = true)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.sensor_unavailable),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.qibla),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (bearing != null && delta != null) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.qibla),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(18.dp))

                    Box(
                        Modifier
                            .size(250.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            Modifier.size(210.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {}
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .rotate(delta.toFloat()),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${bearing.toInt()}°",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(Modifier.height(18.dp))
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${stringResource(R.string.qibla_accuracy)}: $accuracy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.70f)
                    )
                }
            }
        }
    }
}
