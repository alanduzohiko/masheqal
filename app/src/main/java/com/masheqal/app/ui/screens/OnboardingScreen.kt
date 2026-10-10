package com.masheqal.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.masheqal.app.MasheqalApp
import com.masheqal.app.R
import com.masheqal.app.data.SettingsState
import kotlinx.coroutines.launch

private val setupMethods = listOf(
    "MWL" to R.string.method_mwl,
    "EGYPTIAN" to R.string.method_egyptian,
    "UMM_AL_QURA" to R.string.method_umm_al_qura,
    "KARACHI" to R.string.method_karachi,
    "ISNA" to R.string.method_isna,
    "TEHRAN" to R.string.method_tehran,
    "TURKEY" to R.string.method_turkey,
    "GULF" to R.string.method_gulf,
    "KUWAIT" to R.string.method_kuwait,
    "QATAR" to R.string.method_qatar,
    "SINGAPORE" to R.string.method_singapore,
    "FRANCE" to R.string.method_france,
    "RUSSIA" to R.string.method_russia,
    "DUBAI" to R.string.method_dubai
)

private val setupReciters = listOf(
    "ar.alafasy" to "Mishary Rashid Alafasy",
    "ar.sudais" to "Abdul Rahman Al-Sudais",
    "ar.shuraim" to "Saud Al-Shuraim",
    "ar.husary" to "Mahmoud Khalil Al-Husary",
    "ar.minshawi" to "Mohamed Siddiq Al-Minshawi",
    "ar.abdulbasit" to "Abdul Basit Abdul Samad",
    "ar.ajamy" to "Ahmed Al-Ajamy",
    "ar.muhammadjibreel" to "Muhammad Jibreel"
)

@Composable
fun OnboardingScreen(
    app: MasheqalApp,
    onRequestLocation: () -> Unit,
    onLanguage: (String) -> Unit
) {
    val settings by app.settings.state.collectAsState(initial = SettingsState())
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("مەشخەڵ", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.onboarding_welcome),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LinearProgressIndicator(
            progress = { (step + 1) / 4f },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).animateContentSize(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedContent(targetState = step, label = "onboarding-step") { activeStep ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (activeStep) {
                            0 -> {
                                IconBadge(Icons.Default.Language, emphasized = true, modifier = Modifier.size(52.dp))
                                Text(stringResource(R.string.onboarding_language), style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.onboarding_language_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                listOf("ckb" to "کوردی", "ar" to "العربية", "en" to "English").forEach { (code, label) ->
                                    Card(
                                        onClick = {
                                            scope.launch { app.settings.setLanguage(code) }
                                            onLanguage(code)
                                        },
                                        modifier = Modifier.fillMaxWidth().animateContentSize(),
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (settings.language == code) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                        )
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                            if (settings.language == code) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                            1 -> {
                                IconBadge(Icons.Default.LocationOn, emphasized = true, modifier = Modifier.size(52.dp))
                                Text(stringResource(R.string.onboarding_location), style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.onboarding_location_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(
                                    Modifier.fillMaxWidth().background(
                                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)),
                                        RoundedCornerShape(22.dp)
                                    ).padding(18.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(stringResource(R.string.onboarding_location_optional), style = MaterialTheme.typography.titleMedium)
                                        Text(stringResource(R.string.onboarding_location_privacy), style = MaterialTheme.typography.bodyMedium)
                                        Button(onClick = onRequestLocation, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Default.MyLocation, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.onboarding_use_location))
                                        }
                                    }
                                }
                            }
                            2 -> {
                                IconBadge(Icons.Default.Schedule, emphasized = true, modifier = Modifier.size(52.dp))
                                Text(stringResource(R.string.onboarding_prayer_setup), style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.prayer_method), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 230.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(setupMethods) { (code, titleId) ->
                                        Card(
                                            onClick = { scope.launch { app.settings.setPrayerMethod(code) } },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(15.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (settings.prayerMethod == code) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                                            )
                                        ) {
                                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = settings.prayerMethod == code, onClick = { scope.launch { app.settings.setPrayerMethod(code) } })
                                                Text(stringResource(titleId), style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                                Text(stringResource(R.string.madhhab), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = settings.madhhab == "SHAFI",
                                        onClick = { scope.launch { app.settings.setMadhhab("SHAFI") } },
                                        label = { Text(stringResource(R.string.madhhab_shafi)) }
                                    )
                                    FilterChip(
                                        selected = settings.madhhab == "HANAFI",
                                        onClick = { scope.launch { app.settings.setMadhhab("HANAFI") } },
                                        label = { Text(stringResource(R.string.madhhab_hanafi)) }
                                    )
                                }
                            }
                            else -> {
                                IconBadge(Icons.Default.RecordVoiceOver, emphasized = true, modifier = Modifier.size(52.dp))
                                Text(stringResource(R.string.onboarding_reciter_setup), style = MaterialTheme.typography.headlineSmall)
                                Text(stringResource(R.string.onboarding_reciter_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 350.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(setupReciters) { (id, name) ->
                                        Card(
                                            onClick = { scope.launch { app.settings.setReciter(id) } },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(15.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (settings.reciter == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
                                            )
                                        ) {
                                            Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(selected = settings.reciter == id, onClick = { scope.launch { app.settings.setReciter(id) } })
                                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step > 0) {
                OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ArrowBack, null)
                    Spacer(Modifier.width(5.dp))
                    Text(stringResource(R.string.onboarding_back))
                }
            }
            Button(
                onClick = {
                    if (step < 3) step++ else {
                        scope.launch { app.settings.setOnboardingCompleted(true) }
                    }
                },
                modifier = Modifier.weight(1.5f)
            ) {
                Text(stringResource(if (step == 3) R.string.onboarding_finish else R.string.onboarding_continue))
                Spacer(Modifier.width(8.dp))
                Icon(if (step == 3) Icons.Default.Check else Icons.Default.ArrowForward, null)
            }
        }
        Text(
            stringResource(R.string.onboarding_optional_note),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
