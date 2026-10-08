package com.masheqal.app.domain

import java.time.LocalDate
import kotlin.math.*

data class Coordinates(val latitude: Double, val longitude: Double, val timezoneOffsetHours: Double)

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
        require(c.latitude in -90.0..90.0) { "Latitude out of range" }
        require(c.longitude in -180.0..180.0) { "Longitude out of range" }
        val jd = julianDay(date) - c.longitude / 360.0
        val noon = solarNoon(c.longitude)
        val decl = sunDeclination(jd)
        val eqt = equationOfTime(jd)
        val noonMinutes = noon - eqt + c.timezoneOffsetHours * 60.0

        fun angleTime(angle: Double, direction: Double): Double {
            val hours = Math.toDegrees(hourAngle(c.latitude, decl, angle)) / 15.0
            return noonMinutes + direction * hours * 60.0
        }

        val sunrise = angleTime(-0.833, -1.0)
        val sunset = angleTime(-0.833, 1.0)
        var fajr = angleTime(-method.fajrAngle, -1.0)
        var isha = if (method.ishaOffset != null) sunset + method.ishaOffset else angleTime(-method.ishaAngle, 1.0)
        val asrAltitude = Math.toDegrees(
            atan(1.0 / (madhhabFactor(madhhab) + tan(Math.toRadians(abs(c.latitude - decl)))))
        )
        val asr = angleTime(asrAltitude, 1.0)

        if (highLatitudeRule != HighLatitudeRule.NONE) {
            val night = nightLength(sunset, sunrise)
            val fajrLimit = nightPortion(method.fajrAngle, highLatitudeRule, night)
            val ishaLimit = nightPortion(method.ishaAngle, highLatitudeRule, night)
            if (!fajr.isFinite() || fajr < sunrise - 24.0 * 60.0 || fajr > sunrise) {
                fajr = sunrise - fajrLimit
            }
            if (!isha.isFinite() || isha < sunset || isha > sunset + night) {
                isha = sunset + ishaLimit
            }
        }

        return PrayerTimes(
            date = date,
            fajr = normalize(fajr),
            sunrise = normalize(sunrise),
            dhuhr = normalize(noonMinutes),
            asr = normalize(asr),
            maghrib = normalize(sunset),
            isha = normalize(isha)
        )
    }

    private fun nightLength(sunset: Double, sunrise: Double): Double =
        ((sunrise + 1440.0 - sunset) % 1440.0).takeIf { it > 0.0 } ?: 720.0

    private fun nightPortion(angle: Double, rule: HighLatitudeRule, night: Double): Double = when (rule) {
        HighLatitudeRule.MIDDLE_OF_NIGHT -> night / 2.0
        HighLatitudeRule.ANGLE_BASED -> night * angle / 60.0
        HighLatitudeRule.ONE_SEVENTH -> night / 7.0
        HighLatitudeRule.NONE -> 0.0
    }

    private fun madhhabFactor(m: AsrMadhhab) = if (m == AsrMadhhab.HANAFI) 2.0 else 1.0
    private fun normalize(x: Double): Double = ((x % 1440.0) + 1440.0) % 1440.0

    private fun hourAngle(lat: Double, decl: Double, angle: Double): Double {
        val a = Math.toRadians(angle)
        val phi = Math.toRadians(lat)
        val d = Math.toRadians(decl)
        val cosH = (sin(a) - sin(phi) * sin(d)) / (cos(phi) * cos(d))
        return when {
            cosH < -1.0 || cosH > 1.0 -> Double.NaN
            else -> acos(cosH)
        }
    }

    private fun julianDay(date: LocalDate): Double {
        var y = date.year
        var m = date.monthValue
        val d = date.dayOfMonth
        if (m <= 2) { y--; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    private fun solarNoon(longitude: Double): Double = 720.0 - 4.0 * longitude

    private fun equationOfTime(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val l0 = normalizeAngle(280.46646 + 36000.76983 * t + 0.0003032 * t * t)
        val m = Math.toRadians(normalizeAngle(357.52911 + 35999.05029 * t - 0.0001537 * t * t))
        val e = 0.016708634 - 0.000042037 * t - 0.0000001267 * t * t
        val y = tan(Math.toRadians(23.439291 - 0.0130042 * t) / 2).pow(2)
        val l0r = Math.toRadians(l0)
        return Math.toDegrees(
            y * sin(2 * l0r) - 2 * e * sin(m) + 4 * e * y * sin(m) * cos(2 * l0r) -
                0.5 * y * y * sin(4 * l0r) - 1.25 * e * e * sin(2 * m)
        ) * 4.0
    }

    private fun sunDeclination(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val lambda = Math.toRadians(
            normalizeAngle(
                280.46646 + 36000.76983 * t +
                    1.914602 * sin(Math.toRadians(357.52911 + 35999.05029 * t))
            )
        )
        val eps = Math.toRadians(23.439291 - 0.0130042 * t)
        return Math.toDegrees(asin(sin(eps) * sin(lambda)))
    }

    private fun normalizeAngle(x: Double) = ((x % 360.0) + 360.0) % 360.0
}

object QiblaCalculator {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206
    fun bearingFrom(lat: Double, lon: Double): Double {
        require(lat in -90.0..90.0 && lon in -180.0..180.0)
        val phi1 = Math.toRadians(lat)
        val phi2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(dl)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0)
    }
}
