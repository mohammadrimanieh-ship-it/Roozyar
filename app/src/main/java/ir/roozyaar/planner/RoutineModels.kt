package ir.roozyaar.planner

import java.time.LocalDate

enum class RoutineKind { TOGGLE, COUNTER, DECIMAL }

data class RoutineTemplate(
    val key: String,
    val title: String,
    val emoji: String,
    val subtitle: String,
    val target: Double = 1.0,
    val unit: String = "",
    val step: Double = 1.0,
    val kind: RoutineKind = RoutineKind.TOGGLE
)

data class RoutineEntry(
    val template: RoutineTemplate,
    val value: Double = 0.0,
    val done: Boolean = false
)

object DefaultRoutines {
    val all = listOf(
        RoutineTemplate(
            key = "language",
            title = "یادگیری زبان",
            emoji = "🌐",
            subtitle = "۲۰ دقیقه • واژه، جمله و شنیداری"
        ),
        RoutineTemplate(
            key = "quran",
            title = "قرآن + ترجمه + تفسیر",
            emoji = "📖",
            subtitle = "روزانه یک بخش کوتاه و قابل انجام"
        ),
        RoutineTemplate(
            key = "book",
            title = "مطالعه کتاب",
            emoji = "📚",
            subtitle = "۱۰ صفحه از کتاب مفید فعلی"
        ),
        RoutineTemplate(
            key = "water",
            title = "آب",
            emoji = "💧",
            subtitle = "ثبت تعداد لیوان‌های امروز",
            target = 8.0,
            unit = "لیوان",
            step = 1.0,
            kind = RoutineKind.COUNTER
        ),
        RoutineTemplate(
            key = "sleep",
            title = "خواب",
            emoji = "😴",
            subtitle = "ساعت خواب شب گذشته را ثبت کن",
            target = 7.5,
            unit = "ساعت",
            step = 0.5,
            kind = RoutineKind.DECIMAL
        ),
        RoutineTemplate(
            key = "plank",
            title = "پلانک",
            emoji = "🧘",
            subtitle = "۲ نوبت × ۳۰ ثانیه",
            target = 2.0,
            unit = "نوبت",
            step = 1.0,
            kind = RoutineKind.COUNTER
        )
    )
}

data class DailyPreparedContent(
    val language: String,
    val quran: String,
    val book: String
)

object PreparedDailyContent {
    private val languageLessons = listOf(
        "Focus = تمرکز • Schedule = برنامه • Improve = بهبود • Remember = به‌خاطر سپردن • Complete = کامل کردن\nجمله: I want to improve my daily routine.",
        "Priority = اولویت • Meeting = جلسه • Deadline = مهلت • Follow up = پیگیری • Progress = پیشرفت\nجمله: I need to follow up on this project.",
        "Healthy = سالم • Habit = عادت • Enough = کافی • Rest = استراحت • Energy = انرژی\nجمله: Good sleep gives me more energy.",
        "Buy = خریدن • Need = نیاز داشتن • Choose = انتخاب کردن • Price = قیمت • Useful = مفید\nجمله: I need to buy a useful book.",
        "Learn = یاد گرفتن • Practice = تمرین کردن • Understand = فهمیدن • Repeat = تکرار کردن • Speak = صحبت کردن\nجمله: I practice English every day.",
        "Plan = برنامه‌ریزی کردن • Start = شروع کردن • Finish = تمام کردن • Delay = به‌تعویق انداختن • Important = مهم\nجمله: I will finish the important task first.",
        "Calm = آرام • Ready = آماده • Decide = تصمیم گرفتن • Check = بررسی کردن • Tomorrow = فردا\nجمله: I will check it again tomorrow."
    )

    private val quranPlan = listOf(
        "سوره حمد، آیات ۱ تا ۷ • همراه ترجمه • محور تفسیر: حمد، بندگی و درخواست هدایت",
        "سوره بقره، آیات ۱ تا ۵ • همراه ترجمه • محور تفسیر: هدایت و ویژگی‌های اهل تقوا",
        "سوره بقره، آیات ۶ تا ۱۰ • همراه ترجمه • محور تفسیر: واکنش انسان در برابر حقیقت",
        "سوره بقره، آیات ۲۱ تا ۲۵ • همراه ترجمه • محور تفسیر: دعوت به عبادت و امید",
        "سوره عصر • همراه ترجمه • محور تفسیر: ارزش زمان، ایمان و عمل صالح",
        "سوره شرح • همراه ترجمه • محور تفسیر: گشایش پس از سختی",
        "سوره ملک، آیات ۱ تا ۵ • همراه ترجمه • محور تفسیر: قدرت، آفرینش و دقت در جهان"
    )

    private val books = listOf(
        "عادت‌های اتمی — جیمز کلیر",
        "انسان در جستجوی معنا — ویکتور فرانکل",
        "روان‌شناسی پول — مورگان هاوزل",
        "تفکر، سریع و کند — دنیل کانمن",
        "هفت عادت مردمان مؤثر — استیفن کاوی",
        "چگونه دوست پیدا کنیم و در مردم نفوذ کنیم — دیل کارنگی"
    )

    fun today(date: LocalDate = LocalDate.now()): DailyPreparedContent {
        val day = date.toEpochDay()
        val lang = languageLessons[Math.floorMod(day.toInt(), languageLessons.size)]
        val quran = quranPlan[Math.floorMod(day.toInt(), quranPlan.size)]
        val book = books[Math.floorMod((day / 14).toInt(), books.size)] + " • پیشنهاد: امروز ۱۰ صفحه"
        return DailyPreparedContent(lang, quran, book)
    }
}
