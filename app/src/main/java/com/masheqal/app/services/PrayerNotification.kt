package com.masheqal.app.services

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.masheqal.app.R
import com.masheqal.app.domain.*
import java.time.LocalDate
import java.time.ZoneId

object PrayerNotificationScheduler {
    private const val PREF = "prayer_schedule"
    private const val CHANNEL = "prayer"
    private const val CHANNEL_ADHAN = "prayer_adhan"

    fun scheduleToday(context: Context, times: PrayerTimes) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in 0..5) {
            val existingIntent = Intent(context, PrayerAlarmReceiver::class.java)
            val existing = PendingIntent.getBroadcast(
                context,
                100 + index,
                existingIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existing != null) manager.cancel(existing)
        }

        val values = doubleArrayOf(
            times.fajr, times.sunrise, times.dhuhr,
            times.asr, times.maghrib, times.isha
        )
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("date", times.date.toString())
            .apply()

        values.forEachIndexed { index, minutes ->
            if (!minutes.isFinite()) return@forEachIndexed
            val fireAt = PrayerCalculator
                .instantForLocalPrayerMinute(times.date, minutes, ZoneId.systemDefault())
                .toEpochMilli()
            if (fireAt <= System.currentTimeMillis()) return@forEachIndexed

            val labelRes = intArrayOf(
                R.string.fajr, R.string.sunrise, R.string.dhuhr,
                R.string.asr, R.string.maghrib, R.string.isha
            )[index]
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
                .putExtra("name", context.getString(labelRes))
                // Sunrise is a prayer-time marker, not a prayer call.
                .putExtra("playAdhan", index != 1)
            val pending = PendingIntent.getBroadcast(
                context,
                100 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            runCatching { manager.cancel(pending) }
            try {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending)
                } else {
                    manager.setExact(AlarmManager.RTC_WAKEUP, fireAt, pending)
                }
            } catch (_: SecurityException) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending)
            }
        }
    }

    fun isAdhanEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean("adhanEnabled", false)

    fun setAdhanEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putBoolean("adhanEnabled", enabled)
            .apply()
    }

    fun rescheduleFromPreferences(context: Context) {
        val preferences = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val latitude = preferences.getString("lat", null)?.toDoubleOrNull() ?: return
        val longitude = preferences.getString("lon", null)?.toDoubleOrNull() ?: return
        val method = runCatching {
            PrayerMethod.valueOf(preferences.getString("method", "MWL")!!)
        }.getOrDefault(PrayerMethod.MWL)
        val madhhab = runCatching {
            AsrMadhhab.valueOf(preferences.getString("madhhab", "SHAFI")!!)
        }.getOrDefault(AsrMadhhab.SHAFI)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val offset = today.atStartOfDay(zone).offset.totalSeconds / 3600.0
        scheduleToday(
            context,
            PrayerCalculator.calculate(
                today,
                Coordinates(latitude, longitude, offset),
                method,
                madhhab,
                zoneId = zone
            )
        )
    }

    fun storeConfig(
        context: Context,
        lat: Double,
        lon: Double,
        method: PrayerMethod,
        madhhab: AsrMadhhab
    ) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("lat", lat.toString())
            .putString("lon", lon.toString())
            .putString("method", method.name)
            .putString("madhhab", madhhab.name)
            .apply()
    }

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                context.getString(R.string.prayer_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        val quiet = NotificationChannel(
            CHANNEL_ADHAN,
            context.getString(R.string.prayer_adhan_channel),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        manager.createNotificationChannel(quiet)
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: return
        val playAdhan = intent.getBooleanExtra("playAdhan", false) &&
            PrayerNotificationScheduler.isAdhanEnabled(context)

        // Do not couple full adhan playback to POST_NOTIFICATIONS permission.
        if (playAdhan) {
            val playbackIntent = Intent(context, QuranPlaybackService::class.java)
                .setAction(QuranPlaybackService.ACTION_PLAY_ADHAN)
                .putExtra(QuranPlaybackService.EXTRA_PRAYER_NAME, name)
            runCatching { ContextCompat.startForegroundService(context, playbackIntent) }
        }

        if (
            BuildConfigCheck.isPostNotificationsRequired() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        PrayerNotificationScheduler.createChannel(context)
        val channel = if (playAdhan) "prayer_adhan" else "prayer"
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.prayer_notification_text, name))
            .setAutoCancel(true)
            .build()
        (
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ).notify(name.hashCode(), notification)
    }
}

object BuildConfigCheck {
    fun isPostNotificationsRequired() = android.os.Build.VERSION.SDK_INT >= 33
}

class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_DATE_CHANGED
            )
        ) {
            PrayerNotificationScheduler.rescheduleFromPreferences(context)
        }
    }
}
