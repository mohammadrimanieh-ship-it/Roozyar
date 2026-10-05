package ir.roozyaar.planner

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object QuickParser {
    data class Result(
        val category: TaskCategory,
        val priority: TaskPriority,
        val waiting: Boolean,
        val dueAt: Long?
    )

    fun parse(raw: String, nowMillis: Long = System.currentTimeMillis()): Result {
        val text = normalizeDigits(raw).replace('‌', ' ')
        val zone = ZoneId.systemDefault()
        val now = java.time.Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDateTime()

        val category = when {
            listOf("بورس", "سهم", "کارگزاری", "صندوق", "بازار").any(text::contains) -> TaskCategory.STOCK
            listOf("پروژه", "ساختمان", "کارگاه", "پیمانکار", "مهندس", "مصالح").any(text::contains) -> TaskCategory.PROJECTS
            listOf("خانه", "خونه", "خرید", "قبض").any(text::contains) -> TaskCategory.HOME
            text.contains("همسر") -> TaskCategory.SPOUSE
            listOf("شخص", "فلانی", "آقا", "خانم").any(text::contains) -> TaskCategory.PEOPLE
            listOf("بعدا", "بعداً", "ایده").any(text::contains) -> TaskCategory.LATER
            else -> TaskCategory.PERSONAL
        }

        val priority = when {
            text.contains("فوری") -> TaskPriority.URGENT
            text.contains("مهم") -> TaskPriority.HIGH
            else -> TaskPriority.NORMAL
        }
        val waiting = text.contains("منتظر") || text.contains("جواب")

        val targetDate: LocalDate? = when {
            text.contains("پس فردا") || text.contains("پس‌فردا") -> now.toLocalDate().plusDays(2)
            text.contains("فردا") -> now.toLocalDate().plusDays(1)
            text.contains("امروز") -> now.toLocalDate()
            else -> null
        }

        val timeRegex = Regex("ساعت\\s*(\\d{1,2})(?:[:٫.](\\d{1,2}))?")
        val match = timeRegex.find(text)
        val parsedTime = match?.let {
            val h = it.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 9
            val m = it.groupValues.getOrNull(2)?.takeIf(String::isNotBlank)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
            LocalTime.of(h, m)
        }

        val due = when {
            targetDate != null -> LocalDateTime.of(targetDate, parsedTime ?: LocalTime.of(9, 0))
            parsedTime != null -> {
                var dt = LocalDateTime.of(now.toLocalDate(), parsedTime)
                if (!dt.isAfter(now)) dt = dt.plusDays(1)
                dt
            }
            else -> null
        }?.atZone(zone)?.toInstant()?.toEpochMilli()

        return Result(category, priority, waiting, due)
    }

    private fun normalizeDigits(s: String): String = s
        .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3').replace('۴', '4')
        .replace('۵', '5').replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
        .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3').replace('٤', '4')
        .replace('٥', '5').replace('٦', '6').replace('٧', '7').replace('٨', '8').replace('٩', '9')
}
