package com.masheqal.app.domain

import com.batoulapps.adhan.CalculationMethod as AdhanCalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates as AdhanCoordinates
import com.batoulapps.adhan.HighLatitudeRule as AdhanHighLatitudeRule
import com.batoulapps.adhan.Madhab as AdhanMadhab
import com.batoulapps.adhan.PrayerTimes as AdhanPrayerTimes
import com.batoulapps.adhan.data.DateComponents as AdhanDateComponents
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Adapter for the upstream Adhan Java calculation engine.
 *
 * Adhan performs the solar calculations in a well-tested implementation; this adapter only maps
 * this app's stable domain types to that engine and converts absolute prayer instants into local
 * wall-clock minutes for the existing UI. Do not add manual minute corrections here to make a
 * single city match a website; use explicit per-prayer adjustments once those are user-configurable.
 */
data class Coordinates(
    val latitude: Double,
    val longitude: Double,
    val timezoneOffsetHours: Double
)

enum class PrayerMethod(val label: String, val fajrAngle: Double, val ishaAngle: Double, val ishaOffset: Int? = null) {
    MWL("Muslim World League", 18.0, 17.0),
    EGYPTIAN("Egyptian", 19.5, 17.5),
    UMM_AL_QURA("Umm al-Qura", 18.5, 17.0, 90),
    KARACHI("Karachi", 18.0, 18.0),
    ISNA("ISNA", 15.0, 15.0),
    TEHRAN("Tehran", 17.7, 14.0),
    TURKEY("Turkey", 18.0, 17.0)
}

enum class AsrMadhhab { SHAFI, HANAFI }
enum class HighLatitudeRule { NONE, MIDDLE_OF_NIGHT, ANGLE_BASED, ONE_SEVENTH }

data class PrayerTimes(
    val date: LocalDate,
    val fajr: Double,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double,
    val maghrib: Double,
    val isha: Double
)

object PrayerCalculator {
    fun calculate(
        date: LocalDate,
        c: Coordinates,
        method: PrayerMethod = PrayerMethod.MWL,
        madhhab: AsrMadhhab = AsrMadhhab.SHAFI,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ONE_SEVENTH
    ): PrayerTimes {
        require(c.latitude.isFinite() && c.latitude in -90.0..90.0) { "Latitude out of range" }
        require(c.longitude.isFinite() && c.longitude in -180.0..180.0) { "Longitude out of range" }
        require(c.timezoneOffsetHours.isFinite() && c.timezoneOffsetHours in -14.0..14.0) {
            "UTC offset out of range"
        }

        val parameters = when (method) {
            PrayerMethod.MWL -> AdhanCalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            PrayerMethod.EGYPTIAN -> AdhanCalculationMethod.EGYPTIAN.parameters
            PrayerMethod.UMM_AL_QURA -> AdhanCalculationMethod.UMM_AL_QURA.parameters
            PrayerMethod.KARACHI -> AdhanCalculationMethod.KARACHI.parameters
            PrayerMethod.ISNA -> AdhanCalculationMethod.NORTH_AMERICA.parameters
            // There is no official Turkey or Tehran preset in this engine; use explicit angles,
            // and do not invent regional minute corrections without a verified source.
            PrayerMethod.TEHRAN, PrayerMethod.TURKEY ->
                CalculationParameters(method.fajrAngle, method.ishaAngle, AdhanCalculationMethod.OTHER)
        }.apply {
            madhab = when (madhhab) {
                AsrMadhhab.SHAFI -> AdhanMadhab.SHAFI
                AsrMadhhab.HANAFI -> AdhanMadhab.HANAFI
            }
            this.highLatitudeRule = when (highLatitudeRule) {
                // The upstream engine always bounds twilight calculations at high latitude.
                // NONE has no upstream equivalent, so use the engine's documented safe fallback.
                HighLatitudeRule.MIDDLE_OF_NIGHT, HighLatitudeRule.NONE ->
                    AdhanHighLatitudeRule.MIDDLE_OF_THE_NIGHT
                HighLatitudeRule.ANGLE_BASED -> AdhanHighLatitudeRule.TWILIGHT_ANGLE
                HighLatitudeRule.ONE_SEVENTH -> AdhanHighLatitudeRule.SEVENTH_OF_THE_NIGHT
            }
        }

        val engine = AdhanPrayerTimes(
            AdhanCoordinates(c.latitude, c.longitude),
            AdhanDateComponents(date.year, date.monthValue, date.dayOfMonth),
            parameters
        )
        val offset = ZoneOffset.ofTotalSeconds((c.timezoneOffsetHours * 3600.0).roundToInt())

        fun localMinute(name: String, value: Date?): Double {
            checkNotNull(value) {
                "The prayer-time engine could not calculate $name for $date at ${c.latitude}, ${c.longitude}"
            }
            val local = Instant.ofEpochMilli(value.time).atOffset(offset)
            return local.hour * 60.0 + local.minute
        }

        return PrayerTimes(
            date = date,
            fajr = localMinute("Fajr", engine.fajr),
            sunrise = localMinute("sunrise", engine.sunrise),
            dhuhr = localMinute("Dhuhr", engine.dhuhr),
            asr = localMinute("Asr", engine.asr),
            maghrib = localMinute("Maghrib", engine.maghrib),
            isha = localMinute("Isha", engine.isha)
        )
    }
}

object QiblaCalculator {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    fun bearingFrom(lat: Double, lon: Double): Double {
        require(lat.isFinite() && lat in -90.0..90.0) { "Latitude out of range" }
        require(lon.isFinite() && lon in -180.0..180.0) { "Longitude out of range" }
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaLongitude = Math.toRadians(KAABA_LON - lon)
        val y = sin(deltaLongitude)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(deltaLongitude)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0)
    }
}
