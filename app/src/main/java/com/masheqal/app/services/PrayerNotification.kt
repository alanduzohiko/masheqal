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
    private const val PREF="prayer_schedule"
    private const val CHANNEL="prayer"
    fun scheduleToday(context: Context, times: PrayerTimes) {
        val mgr=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (index in 0..5) {
            val existingIntent = Intent(context, PrayerAlarmReceiver::class.java)
            val existing = PendingIntent.getBroadcast(context, 100 + index, existingIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (existing != null) mgr.cancel(existing)
        }
        val values=doubleArrayOf(times.fajr,times.sunrise,times.dhuhr,times.asr,times.maghrib,times.isha)
        context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString("date",times.date.toString()).apply()
        values.forEachIndexed { index, minutes ->
            if (!minutes.isFinite()) return@forEachIndexed
            val millis=times.date.atStartOfDay(ZoneId.systemDefault()).plusMinutes(minutes.toLong()).toInstant().toEpochMilli()
            if (millis <= System.currentTimeMillis()) return@forEachIndexed
            val labelRes=intArrayOf(R.string.fajr,R.string.sunrise,R.string.dhuhr,R.string.asr,R.string.maghrib,R.string.isha)[index]
            val intent=Intent(context,PrayerAlarmReceiver::class.java).putExtra("name",context.getString(labelRes))
            val pi=PendingIntent.getBroadcast(context,100+index,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            runCatching { mgr.cancel(pi) }
            try { if(android.os.Build.VERSION.SDK_INT>=23) mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pi) else mgr.setExact(AlarmManager.RTC_WAKEUP,millis,pi) } catch(_: SecurityException) { mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,millis,pi) }
        }
    }
    fun rescheduleFromPreferences(context: Context) {
        val p=context.getSharedPreferences(PREF,Context.MODE_PRIVATE); val lat=p.getString("lat",null)?.toDoubleOrNull() ?: return; val lon=p.getString("lon",null)?.toDoubleOrNull() ?: return
        val method=runCatching{PrayerMethod.valueOf(p.getString("method","MWL")!!)}.getOrDefault(PrayerMethod.MWL)
        val madhhab=runCatching{AsrMadhhab.valueOf(p.getString("madhhab","SHAFI")!!)}.getOrDefault(AsrMadhhab.SHAFI)
        val zone=ZoneId.systemDefault(); val offset=LocalDate.now().atStartOfDay(zone).offset.totalSeconds/3600.0
        scheduleToday(context,PrayerCalculator.calculate(LocalDate.now(),Coordinates(lat,lon,offset),method,madhhab))
    }
    fun storeConfig(context: Context,lat:Double,lon:Double,method:PrayerMethod,madhhab:AsrMadhhab){context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString("lat",lat.toString()).putString("lon",lon.toString()).putString("method",method.name).putString("madhhab",madhhab.name).apply()}
    fun createChannel(context: Context){ val nm=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; nm.createNotificationChannel(NotificationChannel(CHANNEL,context.getString(R.string.prayer_notification_channel),NotificationManager.IMPORTANCE_HIGH)) }
}
class PrayerAlarmReceiver: BroadcastReceiver(){
    override fun onReceive(context: Context,intent: Intent){
        if(BuildConfigCheck.isPostNotificationsRequired() && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return
        PrayerNotificationScheduler.createChannel(context)
        val name=intent.getStringExtra("name") ?: return
        val n=NotificationCompat.Builder(context,"prayer").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(context.getString(R.string.app_name)).setContentText(context.getString(R.string.prayer_notification_text, name)).setAutoCancel(true).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(name.hashCode(),n)
    }
}
object BuildConfigCheck{fun isPostNotificationsRequired()=android.os.Build.VERSION.SDK_INT>=33}
class PrayerBootReceiver: BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){if(intent.action in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_DATE_CHANGED)) PrayerNotificationScheduler.rescheduleFromPreferences(context)}}
