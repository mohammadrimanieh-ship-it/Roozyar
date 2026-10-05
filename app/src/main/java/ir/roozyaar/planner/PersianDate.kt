package ir.roozyaar.planner

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class JalaliDate(val year: Int, val month: Int, val day: Int)

object PersianDate {
    private val months = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
    private val weekdays = mapOf(
        DayOfWeek.SATURDAY to "شنبه",
        DayOfWeek.SUNDAY to "یکشنبه",
        DayOfWeek.MONDAY to "دوشنبه",
        DayOfWeek.TUESDAY to "سه‌شنبه",
        DayOfWeek.WEDNESDAY to "چهارشنبه",
        DayOfWeek.THURSDAY to "پنجشنبه",
        DayOfWeek.FRIDAY to "جمعه"
    )

    fun fromGregorian(date: LocalDate): JalaliDate {
        val gy = date.year
        val gm = date.monthValue
        val gd = date.dayOfMonth
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + days / 31
            jd = 1 + days % 31
        } else {
            jm = 7 + (days - 186) / 30
            jd = 1 + (days - 186) % 30
        }
        return JalaliDate(jy, jm, jd)
    }

    fun header(nowMillis: Long = System.currentTimeMillis()): String {
        val date = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val j = fromGregorian(date)
        return "${weekdays[date.dayOfWeek]} ${toFa(j.day)} ${months[j.month - 1]} ${toFa(j.year)}"
    }

    fun shortDate(millis: Long): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val j = fromGregorian(zdt.toLocalDate())
        return "${toFa(j.day)} ${months[j.month - 1]}"
    }

    fun fullDate(millis: Long): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        val j = fromGregorian(zdt.toLocalDate())
        return "${weekdays[zdt.dayOfWeek]} ${toFa(j.day)} ${months[j.month - 1]} ${toFa(j.year)}"
    }

    fun time(millis: Long): String {
        val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
        return toFa(zdt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US)))
    }

    fun relativeDue(millis: Long): String {
        val zone = ZoneId.systemDefault()
        val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        val prefix = when (d) {
            today -> "امروز"
            today.plusDays(1) -> "فردا"
            today.minusDays(1) -> "دیروز"
            else -> shortDate(millis)
        }
        return "$prefix، ${time(millis)}"
    }

    fun toFa(value: Any): String = value.toString()
        .replace('0', '۰').replace('1', '۱').replace('2', '۲').replace('3', '۳').replace('4', '۴')
        .replace('5', '۵').replace('6', '۶').replace('7', '۷').replace('8', '۸').replace('9', '۹')
}
