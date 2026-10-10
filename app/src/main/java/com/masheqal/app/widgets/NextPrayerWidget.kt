package com.masheqal.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.masheqal.app.MainActivity
import com.masheqal.app.R
import com.masheqal.app.domain.AsrMadhhab
import com.masheqal.app.domain.Coordinates
import com.masheqal.app.domain.PrayerCalculator
import com.masheqal.app.domain.PrayerMethod
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class NextPrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { ids.forEach { update(context,manager,it) } }
    companion object {
        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val views=RemoteViews(context.packageName,R.layout.widget_next_prayer)
            views.setTextViewText(R.id.widget_title,context.getString(R.string.app_name))
            val p=context.getSharedPreferences("prayer_schedule",Context.MODE_PRIVATE)
            val lat=p.getString("lat",null)?.toDoubleOrNull(); val lon=p.getString("lon",null)?.toDoubleOrNull()
            if(lat==null || lon==null){
                views.setTextViewText(R.id.widget_prayer,context.getString(R.string.location_needed)); views.setTextViewText(R.id.widget_time,"—")
            } else {
                val method=runCatching{PrayerMethod.valueOf(p.getString("method","MWL")!!)}.getOrDefault(PrayerMethod.MWL)
                val madhhab=runCatching{AsrMadhhab.valueOf(p.getString("madhhab","SHAFI")!!)}.getOrDefault(AsrMadhhab.SHAFI)
                val zone=ZoneId.systemDefault(); val offset=ZonedDateTime.now(zone).offset.totalSeconds/3600.0
                val times=PrayerCalculator.calculate(LocalDate.now(),Coordinates(lat,lon,offset),method,madhhab)
                val rows=listOf(context.getString(R.string.fajr) to times.fajr,context.getString(R.string.dhuhr) to times.dhuhr,context.getString(R.string.asr) to times.asr,context.getString(R.string.maghrib) to times.maghrib,context.getString(R.string.isha) to times.isha)
                val now=ZonedDateTime.now(); val current=now.hour*60.0+now.minute+now.second/60.0
                val next=rows.firstOrNull{it.second>=current} ?: rows.first()
                views.setTextViewText(R.id.widget_prayer,next.first); views.setTextViewText(R.id.widget_time,format(next.second))
            }
            val intent=Intent(context,MainActivity::class.java).setAction("OPEN_PRAYER")
            views.setOnClickPendingIntent(R.id.widget_title,PendingIntent.getActivity(context,20,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id,views)
        }
        fun refresh(context: Context) { val manager=AppWidgetManager.getInstance(context); val component=ComponentName(context,NextPrayerWidget::class.java); manager.getAppWidgetIds(component).forEach{update(context,manager,it)} }
        private fun format(v:Double):String{val total=kotlin.math.round(v).toInt();return java.time.LocalTime.of((total/60)%24,total%60).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.getDefault()))}
    }
}
