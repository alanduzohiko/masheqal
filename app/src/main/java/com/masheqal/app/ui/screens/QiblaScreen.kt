package com.masheqal.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.masheqal.app.R
import com.masheqal.app.domain.QiblaCalculator
import com.masheqal.app.util.LocationUtils
import kotlin.math.abs

@Composable
fun QiblaScreen(nav: NavHostController) {
    val context=androidx.compose.ui.platform.LocalContext.current
    var location by remember { mutableStateOf(LocationUtils.lastKnown(context)) }
    var azimuth by remember { mutableStateOf<Float?>(null) }
    var accuracy by remember { mutableStateOf(0) }
    val request=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){location=LocationUtils.lastKnown(context)}
    DisposableEffect(Unit){
        val sm=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor=sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener=object:SensorEventListener{
            override fun onSensorChanged(e:SensorEvent){val r=FloatArray(9);val o=FloatArray(3);SensorManager.getRotationMatrixFromVector(r,e.values);SensorManager.getOrientation(r,o);azimuth=Math.toDegrees(o[0].toDouble()).toFloat().let{(it+360)%360};}
            override fun onAccuracyChanged(s:Sensor?,a:Int){accuracy=a}
        }
        if(sensor!=null)sm.registerListener(listener,sensor,SensorManager.SENSOR_DELAY_UI)
        onDispose{sm.unregisterListener(listener)}
    }
    val bearing=location?.let{QiblaCalculator.bearingFrom(it.latitude,it.longitude)}
    Column(Modifier.fillMaxSize().padding(18.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Start){IconButton(onClick={nav.popBackStack()}){Icon(Icons.Default.ArrowBack,null)};Text(stringResource(R.string.qibla),style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(top=10.dp))}
        Spacer(Modifier.height(40.dp))
        if(location==null){Button(onClick={request.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION))}){Text(stringResource(R.string.set_location))}}
        if(azimuth==null){Text(stringResource(R.string.sensor_unavailable))} else if(bearing!=null){
            val delta=((bearing-azimuth!!+540)%360)-180
            Box(Modifier.fillMaxWidth().weight(1f),contentAlignment=Alignment.Center){Text("${delta.toInt()}°",style=MaterialTheme.typography.displayLarge,color=MaterialTheme.colorScheme.primary)}
            Text("${stringResource(R.string.qibla_bearing)}: ${bearing.toInt()}°  •  ${stringResource(R.string.qibla_accuracy)}: $accuracy", style=MaterialTheme.typography.bodyMedium)
            Text(
                if (abs(delta) < 3) stringResource(R.string.aligned)
                else if (delta > 0) stringResource(R.string.turn_right) else stringResource(R.string.turn_left),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
