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
        RoutineTemplate("language", "یادگیری زبان", "🌐", "۲۰ دقیقه • درس آماده داخل برنامه"),
        RoutineTemplate("quran", "قرآن + ترجمه + تفسیر", "📖", "مطالعه روزانه داخل برنامه"),
        RoutineTemplate("book", "مطالعه کتاب", "📚", "برنامه مطالعه و نکته روز"),
        RoutineTemplate("water", "آب", "💧", "ثبت تعداد لیوان‌های امروز", 8.0, "لیوان", 1.0, RoutineKind.COUNTER),
        RoutineTemplate("sleep", "خواب", "😴", "ساعت خواب شب گذشته را ثبت کن", 7.5, "ساعت", 0.5, RoutineKind.DECIMAL),
        RoutineTemplate("plank", "پلانک", "🧘", "۲ نوبت × ۳۰ ثانیه", 2.0, "نوبت", 1.0, RoutineKind.COUNTER)
    )
}

data class DailyPreparedContent(
    val languagePreview: String,
    val languageLesson: String,
    val quranPreview: String,
    val quranLesson: String,
    val bookPreview: String,
    val bookLesson: String
)

object PreparedDailyContent {
    private val language = listOf(
        Triple(
            "۵ واژه + جمله + تمرین کوتاه",
            """واژه‌های امروز:
Focus = تمرکز
Schedule = برنامه
Improve = بهبود دادن
Remember = به خاطر سپردن
Complete = کامل کردن

جمله نمونه:
I want to improve my daily routine.
من می‌خواهم برنامه روزانه‌ام را بهتر کنم.

تمرین:
۱) با Focus یک جمله بساز.
۲) ترجمه کن: I need to complete my work today.
۳) هر پنج واژه را یک بار با صدای بلند تکرار کن.""",
            "Focus • Schedule • Improve • Remember • Complete"
        ),
        Triple(
            "۵ واژه کاربردی برای کار و پیگیری",
            """واژه‌های امروز:
Priority = اولویت
Meeting = جلسه
Deadline = مهلت
Follow up = پیگیری
Progress = پیشرفت

جمله نمونه:
I need to follow up on this project.
باید این پروژه را پیگیری کنم.

تمرین:
سه کار امروزت را با یکی از واژه‌های بالا توصیف کن.""",
            "Priority • Meeting • Deadline • Follow up • Progress"
        ),
        Triple(
            "۵ واژه درباره عادت و سلامت",
            """واژه‌های امروز:
Healthy = سالم
Habit = عادت
Enough = کافی
Rest = استراحت
Energy = انرژی

جمله نمونه:
Good sleep gives me more energy.
خواب خوب به من انرژی بیشتری می‌دهد.

تمرین:
یک عادت سالم خودت را به انگلیسی در یک جمله بنویس.""",
            "Healthy • Habit • Enough • Rest • Energy"
        )
    )

    private val quran = listOf(
        Pair(
            "سوره حمد • آیات ۱ تا ۷",
            """سوره حمد، آیات ۱ تا ۷

بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ
الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ
الرَّحْمَنِ الرَّحِيمِ
مَالِكِ يَوْمِ الدِّينِ
إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ
اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ
صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ

ترجمه روان:
ستایش مخصوص خداوند، پروردگار جهانیان است؛ بخشنده و مهربان، صاحب روز جزا. تنها تو را می‌پرستیم و تنها از تو یاری می‌خواهیم. ما را به راه راست هدایت کن.

نکته تفسیری:
محور سوره، شناخت خدا، بندگی آگاهانه و درخواست هدایت مداوم است.

تمرین امروز:
یک بار آیات را آهسته بخوان و جمله «تنها از تو یاری می‌خواهیم» را برای یک مسئله امروزت در ذهن مرور کن."""
        ),
        Pair(
            "سوره عصر • کامل",
            """وَالْعَصْرِ
إِنَّ الْإِنْسَانَ لَفِي خُسْرٍ
إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ

ترجمه روان:
سوگند به زمان؛ انسان در زیان است، مگر کسانی که ایمان دارند، کار شایسته انجام می‌دهند و یکدیگر را به حق و شکیبایی سفارش می‌کنند.

نکته تفسیری:
زمان سرمایه‌ای برگشت‌ناپذیر است و ارزش آن با ایمان، عمل درست، حقیقت‌خواهی و صبر حفظ می‌شود.

تمرین امروز:
یک کار کوچک و مشخص انتخاب کن که امروز انجامش، استفاده بهتر از زمان باشد."""
        ),
        Pair(
            "سوره شرح • کامل",
            """أَلَمْ نَشْرَحْ لَكَ صَدْرَكَ ... فَإِنَّ مَعَ الْعُسْرِ يُسْرًا، إِنَّ مَعَ الْعُسْرِ يُسْرًا

ترجمه روان:
آیا سینه‌ات را برایت گشاده نکردیم؟ ... پس همراه سختی، آسانی است؛ آری همراه سختی، آسانی است.

نکته تفسیری:
آیه از وجود گشایش در دل سختی سخن می‌گوید، نه فقط بعد از آن. نگاه فعال و امیدوارانه بخشی از پیام سوره است.

تمرین امروز:
یک مسئله سخت فعلی را بنویس و یک «گشایش کوچکِ موجود» در همان مسئله پیدا کن."""
        )
    )

    private val books = listOf(
        Pair(
            "عادت‌های اتمی — جیمز کلیر • ۱۰ صفحه",
            """برنامه امروز:
۱۰ صفحه از «عادت‌های اتمی» بخوان.

نکته کاربردی روز:
تغییرهای بسیار کوچک وقتی تکرار شوند، اثر مرکب ایجاد می‌کنند. به جای تمرکز فقط روی هدف نهایی، روی سیستم روزانه تمرکز کن.

تمرین:
یک عادت را انتخاب کن و نسخه دو دقیقه‌ای آن را تعریف کن؛ کاری که شروعش آن‌قدر آسان باشد که بهانه‌ای برای انجام ندادنش نماند.

بعد از مطالعه، روتین را «انجام شد» بزن."""
        ),
        Pair(
            "انسان در جستجوی معنا — ویکتور فرانکل • ۱۰ صفحه",
            """برنامه امروز:
۱۰ صفحه از «انسان در جستجوی معنا» بخوان.

نکته کاربردی روز:
انسان همیشه کنترل کامل شرایط را ندارد، اما می‌تواند روی نوع پاسخ خود به شرایط کار کند.

تمرین:
یک موقعیت خارج از کنترل امروزت را بنویس و مشخص کن چه بخش کوچکی از واکنش خودت هنوز در اختیار توست."""
        ),
        Pair(
            "روان‌شناسی پول — مورگان هاوزل • ۱۰ صفحه",
            """برنامه امروز:
۱۰ صفحه از «روان‌شناسی پول» بخوان.

نکته کاربردی روز:
رفتار مالی فقط حاصل دانش نیست؛ تجربه شخصی، صبر و تحمل ریسک هم نقش بزرگی دارند.

تمرین:
یکی از تصمیم‌های مالی اخیرت را بنویس و مشخص کن کدام بخش آن منطقی و کدام بخش احساسی بوده است."""
        )
    )

    fun today(date: LocalDate = LocalDate.now()): DailyPreparedContent {
        val day = date.toEpochDay().toInt()
        val l = language[Math.floorMod(day, language.size)]
        val q = quran[Math.floorMod(day, quran.size)]
        val b = books[Math.floorMod(day / 7, books.size)]
        return DailyPreparedContent(
            languagePreview = "${l.first} • ${l.third}",
            languageLesson = l.second,
            quranPreview = "${q.first} • ترجمه و نکته تفسیری داخل برنامه",
            quranLesson = q.second,
            bookPreview = b.first,
            bookLesson = b.second
        )
    }
}
