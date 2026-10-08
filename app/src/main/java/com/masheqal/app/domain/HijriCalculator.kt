package com.masheqal.app.domain

import java.time.LocalDate
import kotlin.math.floor

data class HijriDate(val day: Int, val month: Int, val year: Int)
object HijriCalculator {
    fun fromGregorian(date: LocalDate): HijriDate {
        val jd = gregorianToJd(date.year, date.monthValue, date.dayOfMonth)
        return islamicFromJd(jd)
    }
    private fun islamicFromJd(jd: Double): HijriDate {
        val l = floor(jd) - 1948440 + 10632
        val n = floor((l - 1) / 10631)
        val l2 = l - 10631 * n + 354
        val j = (floor((10985 - l2) / 5316) * floor((50 * l2) / 17719)) + (floor(l2 / 5670) * floor((43 * l2) / 15238))
        val l3 = l2 - floor((30 - j) / 15) * floor((17719 * j) / 50) - floor(j / 16) * floor((15238 * j) / 43) + 29
        val m = floor(24 * l3 / 709)
        val d = l3 - floor(709 * m / 24)
        val y = 30 * n + j - 30
        return HijriDate(d.toInt(), m.toInt(), y.toInt())
    }
    private fun gregorianToJd(y0: Int, m0: Int, d: Int): Double {
        var y=y0; var m=m0; if (m <= 2) { y--; m+=12 }
        val a=floor(y/100.0); val b=2-a+floor(a/4.0)
        return floor(365.25*(y+4716))+floor(30.6001*(m+1))+d+b-1524.5
    }
}
