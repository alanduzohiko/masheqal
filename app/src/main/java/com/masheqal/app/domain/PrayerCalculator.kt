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
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
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
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.ONE_SEVENTH,
        zoneId: ZoneId? = null
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
            val instant = Instant.ofEpochMilli(value.time)
            val zone = zoneId
            return if (zone != null) {
                val local = instant.atZone(zone)
                local.hour * 60.0 + local.minute
            } else {
                val local = instant.atOffset(offset)
                local.hour * 60.0 + local.minute
            }
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

    /**
     * Resolves a prayer's local wall-clock minute to an instant in the selected time zone.
     * This avoids treating a civil day as exactly 1,440 elapsed minutes on daylight-saving days.
     */
    fun instantForLocalPrayerMinute(date: LocalDate, prayerMinute: Double, zoneId: ZoneId): Instant {
        require(prayerMinute.isFinite() && prayerMinute >= 0.0 && prayerMinute < 1440.0) {
            "Prayer minute must be within the local day"
        }
        val roundedMinute = prayerMinute.roundToInt()
        require(roundedMinute in 0 until 1440) { "Rounded prayer minute must be within the local day" }
        return date.atStartOfDay().plusMinutes(roundedMinute.toLong()).atZone(zoneId).toInstant()
    }

    /**
     * Selects the next prayer from today's ordered local times. After Isha, use the next day's
     * freshly calculated Fajr rather than reusing today's Fajr with a 24-hour countdown.
     */
    fun selectNextPrayer(
        currentMinuteOfDay: Double,
        todayPrayers: List<Pair<String, Double>>,
        tomorrowFajrMinute: Double?
    ): Pair<String, Double>? {
        require(currentMinuteOfDay.isFinite() && currentMinuteOfDay >= 0.0 && currentMinuteOfDay < 1440.0) {
            "Current local minute must be within the day"
        }
        require(todayPrayers.all { (name, minute) ->
            name.isNotBlank() && minute.isFinite() && minute >= 0.0 && minute < 1440.0
        }) { "Prayer list contains an invalid name or time" }
        require(todayPrayers.zipWithNext().all { (a, b) -> a.second <= b.second }) {
            "Prayer times must be sorted chronologically"
        }
        if (todayPrayers.isEmpty()) return null

        return todayPrayers.firstOrNull { it.second >= currentMinuteOfDay }
            ?: tomorrowFajrMinute?.let { fajr ->
                require(fajr.isFinite() && fajr >= 0.0 && fajr < 1440.0) {
                    "Tomorrow's Fajr must be within the local day"
                }
                todayPrayers.first().first to fajr
            }
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

    /** Great-circle distance to the Kaaba in kilometres, using a mean Earth radius. */
    fun distanceFromKm(lat: Double, lon: Double): Double {
        require(lat.isFinite() && lat in -90.0..90.0) { "Latitude out of range" }
        require(lon.isFinite() && lon in -180.0..180.0) { "Longitude out of range" }
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val deltaPhi = phi2 - phi1
        val deltaLambda = Math.toRadians(KAABA_LON - lon)
        val sinLat = sin(deltaPhi / 2.0)
        val sinLon = sin(deltaLambda / 2.0)
        val h = (sinLat * sinLat + cos(phi1) * cos(phi2) * sinLon * sinLon).coerceIn(0.0, 1.0)
        return 6371.0088 * 2.0 * atan2(sqrt(h), sqrt(1.0 - h))
    }

    /**
     * Converts a magnetic compass heading to a signed turn toward the true-north Qibla bearing.
     * Positive means clockwise/right; negative means counter-clockwise/left.
     */
    fun signedDeltaFromMagneticHeading(
        trueBearingDegrees: Double,
        magneticAzimuthDegrees: Double,
        magneticDeclinationDegrees: Double
    ): Double {
        require(
            trueBearingDegrees.isFinite() &&
                magneticAzimuthDegrees.isFinite() &&
                magneticDeclinationDegrees.isFinite()
        ) { "Qibla heading values must be finite" }
        val bearing = ((trueBearingDegrees % 360.0) + 360.0) % 360.0
        val trueHeading = ((magneticAzimuthDegrees + magneticDeclinationDegrees) % 360.0 + 360.0) % 360.0
        return ((bearing - trueHeading + 540.0) % 360.0) - 180.0
    }
}
