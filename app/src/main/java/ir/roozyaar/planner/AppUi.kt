package ir.roozyaar.planner

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

private val Bg = Color(0xFF0E1624)
private val Surface1 = Color(0xFF172235)
private val Surface2 = Color(0xFF202E43)
private val Accent = Color(0xFF4FD1A5)
private val Warning = Color(0xFFF5B85B)
private val Danger = Color(0xFFFF6B6B)
private val Muted = Color(0xFF9EACC0)

private val AppColors = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    background = Bg,
    onBackground = Color.White,
    surface = Surface1,
    onSurface = Color.White,
    surfaceVariant = Surface2,
    onSurfaceVariant = Color(0xFFD8E0EB),
    error = Danger
)

private enum class MainScreen { TODAY, ROUTINE, PROJECTS, CALENDAR, COMPLETED, REPORTS }
private enum class CompletedFilter(val label: String) { TODAY("امروز"), WEEK("این هفته"), MONTH("این ماه"), ALL("همه") }

@Composable
fun RoozYaarRoot(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val routines by viewModel.routines.collectAsState()
    var screen by remember { mutableStateOf(MainScreen.TODAY) }
    var editorTask by remember { mutableStateOf<TaskItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var categoryFilter by remember { mutableStateOf<TaskCategory?>(null) }
    var waitingOnly by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = AppColors) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Bg,
                floatingActionButtonPosition = FabPosition.End,
                floatingActionButton = {
                    if (screen != MainScreen.PROJECTS) {
                        FloatingActionButton(
                            onClick = { editorTask = null; showEditor = true },
                            containerColor = Accent,
                            contentColor = Bg
                        ) { Text("＋", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = Surface1) {
                        NavItem("✓", "امروز", screen == MainScreen.TODAY) { screen = MainScreen.TODAY }
                        NavItem("◉", "روتین", screen == MainScreen.ROUTINE) {
                            viewModel.refreshRoutines(); screen = MainScreen.ROUTINE
                        }
                        NavItem("🏗", "پروژه", screen == MainScreen.PROJECTS) {
                            viewModel.refreshProjects(); screen = MainScreen.PROJECTS
                        }
                        NavItem("▦", "تقویم", screen == MainScreen.CALENDAR) { screen = MainScreen.CALENDAR }
                        NavItem("☑", "انجام‌شده", screen == MainScreen.COMPLETED) { screen = MainScreen.COMPLETED }
                        NavItem("▥", "گزارش", screen == MainScreen.REPORTS) { screen = MainScreen.REPORTS }
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    when (screen) {
                        MainScreen.TODAY -> TodayScreen(
                            tasks = tasks,
                            categoryFilter = categoryFilter,
                            waitingOnly = waitingOnly,
                            onFilterCategory = { categoryFilter = it; waitingOnly = false },
                            onWaiting = { waitingOnly = it; if (it) categoryFilter = null },
                            onDone = viewModel::setDone,
                            onEdit = { editorTask = it; showEditor = true }
                        )
                        MainScreen.ROUTINE -> RoutineScreen(routines, viewModel::toggleRoutine, viewModel::adjustRoutine)
                        MainScreen.PROJECTS -> ProjectsScreen(viewModel)
                        MainScreen.CALENDAR -> CalendarScreen(tasks, viewModel::setDone) { editorTask = it; showEditor = true }
                        MainScreen.COMPLETED -> CompletedScreen(tasks, viewModel::setDone) { editorTask = it; showEditor = true }
                        MainScreen.REPORTS -> ReportsScreen(tasks, routines, viewModel.projects.collectAsState().value)
                    }
                }
            }

            if (showEditor) {
                TaskEditorDialog(
                    task = editorTask,
                    onDismiss = { showEditor = false },
                    onSave = { viewModel.save(it); showEditor = false },
                    onDelete = editorTask?.let { task -> { viewModel.delete(task); showEditor = false } }
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: String, label: String, selected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Text(icon, fontSize = 16.sp) },
        label = { Text(label, fontSize = 9.sp, maxLines = 1) }
    )
}

@Composable
private fun LiveHeader() {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    Surface(shape = RoundedCornerShape(22.dp), color = Surface1, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("My Planner", fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text(PersianDate.header(now), color = Muted, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(PersianDate.time(now), color = Accent, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                Text("ساعت دستگاه", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ReminderStatusCard() {
    val context = LocalContext.current
    val notificationsOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val exactOk = ReminderScheduler.canScheduleExactly(context)

    if (notificationsOk && exactOk) return

    Surface(shape = RoundedCornerShape(18.dp), color = Warning.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("یادآوری نیاز به دسترسی دارد", fontWeight = FontWeight.Bold, color = Warning)
                Text(
                    when {
                        !notificationsOk -> "اعلان‌های My Planner خاموش است."
                        else -> "دسترسی «Alarms & reminders» فعال نیست؛ یادآوری دقیق ممکن است دیر برسد."
                    },
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            TextButton(onClick = {
                val intent = if (!notificationsOk) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                }
                runCatching { context.startActivity(intent) }
            }) { Text("تنظیمات") }
        }
    }
}

@Composable
private fun TodayScreen(
    tasks: List<TaskItem>,
    categoryFilter: TaskCategory?,
    waitingOnly: Boolean,
    onFilterCategory: (TaskCategory?) -> Unit,
    onWaiting: (Boolean) -> Unit,
    onDone: (TaskItem, Boolean) -> Unit,
    onEdit: (TaskItem) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val endToday = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    val open = tasks.filter { it.status != TaskStatus.DONE }
    val urgentCount = open.count { it.priority == TaskPriority.URGENT }
    val dueCount = open.count { it.dueAt != null && it.dueAt <= endToday }
    val waitingCount = open.count { it.status == TaskStatus.WAITING }

    val filtered = open.filter {
        when {
            waitingOnly -> it.status == TaskStatus.WAITING
            categoryFilter != null -> it.category == categoryFilter
            else -> true
        }
    }
    val next = filtered.filter { it.status == TaskStatus.ACTIVE }
        .minWithOrNull(compareBy<TaskItem> { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.priority.ordinal })
    val dueNow = filtered.filter { it.status == TaskStatus.ACTIVE && it.dueAt != null && it.dueAt <= endToday }
    val future = filtered.filter { it.status == TaskStatus.ACTIVE && it.dueAt != null && it.dueAt > endToday }
    val noDate = filtered.filter { it.status == TaskStatus.ACTIVE && it.dueAt == null }
    val waiting = filtered.filter { it.status == TaskStatus.WAITING }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { LiveHeader() }
        item { ReminderStatusCard() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("فوری", urgentCount, Danger, Modifier.weight(1f))
                StatCard("امروز", dueCount, Warning, Modifier.weight(1f))
                StatCard("منتظر", waitingCount, Accent, Modifier.weight(1f))
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = categoryFilter == null && !waitingOnly, onClick = { onFilterCategory(null) }, label = { Text("همه") }) }
                item { FilterChip(selected = waitingOnly, onClick = { onWaiting(!waitingOnly) }, label = { Text("⏳ منتظر دیگران") }) }
                items(TaskCategory.entries) { cat ->
                    FilterChip(
                        selected = categoryFilter == cat,
                        onClick = { onFilterCategory(if (categoryFilter == cat) null else cat) },
                        label = { Text("${cat.emoji} ${cat.label}") }
                    )
                }
            }
        }
        if (next != null && !waitingOnly) {
            item {
                Text("کار بعدی پیشنهادی", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(8.dp))
                NextTaskCard(next, onDone, onEdit)
            }
        }
        if (dueNow.isNotEmpty()) {
            item { SectionTitle("امروز و عقب‌افتاده", dueNow.size) }
            items(dueNow, key = { it.id }) { TaskCard(it, onDone, onEdit) }
        }
        if (future.isNotEmpty()) {
            item { SectionTitle("آینده", future.size) }
            items(future, key = { it.id }) { TaskCard(it, onDone, onEdit) }
        }
        if (waiting.isNotEmpty()) {
            item { SectionTitle("منتظر دیگران", waiting.size) }
            items(waiting, key = { it.id }) { TaskCard(it, onDone, onEdit) }
        }
        if (noDate.isNotEmpty()) {
            item { SectionTitle("بدون زمان", noDate.size) }
            items(noDate, key = { it.id }) { TaskCard(it, onDone, onEdit) }
        }
        if (filtered.isEmpty()) item { EmptyState("فعلاً کاری اینجا نیست", "با دکمه + اولین کار را ثبت کن.") }
    }
}

@Composable
private fun StatCard(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = Surface1) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(PersianDate.toFa(count), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 12.sp, color = Muted)
        }
    }
}

@Composable
private fun NextTaskCard(task: TaskItem, onDone: (TaskItem, Boolean) -> Unit, onEdit: (TaskItem) -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Accent.copy(alpha = 0.13f),
        modifier = Modifier.fillMaxWidth().clickable { onEdit(task) }
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${task.category.emoji} ${task.category.label}", color = Accent, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                task.dueAt?.let { Text("⏰ ${PersianDate.relativeDue(it)}", color = Muted, fontSize = 13.sp) }
            }
            Button(onClick = { onDone(task, true) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("انجام شد") }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(PersianDate.toFa(count), color = Muted)
    }
}

@Composable
fun TaskCard(task: TaskItem, onDone: (TaskItem, Boolean) -> Unit, onEdit: (TaskItem) -> Unit) {
    val overdue = task.dueAt?.let { it < System.currentTimeMillis() } == true && task.status != TaskStatus.DONE
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (task.status == TaskStatus.DONE) Accent.copy(alpha = 0.10f) else Surface1,
        modifier = Modifier.fillMaxWidth().clickable { onEdit(task) }
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = task.status == TaskStatus.DONE,
                onCheckedChange = { onDone(task, it) },
                colors = CheckboxDefaults.colors(checkedColor = Accent)
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (task.priority != TaskPriority.NORMAL && task.status != TaskStatus.DONE) {
                        val c = if (task.priority == TaskPriority.URGENT) Danger else Warning
                        Text(task.priority.label, color = c, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(task.category.emoji).append(' ').append(task.category.label)
                        if (task.contextName.isNotBlank()) append(" • ").append(task.contextName)
                    },
                    color = Muted,
                    fontSize = 12.sp
                )
                if (task.status == TaskStatus.DONE) {
                    task.completedAt?.let { Text("✓ انجام‌شده در ${PersianDate.fullDate(it)} • ${PersianDate.time(it)}", color = Accent, fontSize = 12.sp) }
                } else {
                    task.dueAt?.let {
                        Text((if (overdue) "عقب‌افتاده • " else "") + PersianDate.relativeDue(it), color = if (overdue) Danger else Muted, fontSize = 12.sp)
                    }
                    if (task.status == TaskStatus.WAITING) Text("⏳ منتظر دیگران", color = Accent, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(tasks: List<TaskItem>, onDone: (TaskItem, Boolean) -> Unit, onEdit: (TaskItem) -> Unit) {
    val dated = tasks.filter { it.status != TaskStatus.DONE && it.dueAt != null }.sortedBy { it.dueAt }
    val zone = ZoneId.systemDefault()
    val groups = dated.groupBy { Instant.ofEpochMilli(it.dueAt!!).atZone(zone).toLocalDate() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("تقویم شمسی کارها", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("تاریخ‌ها با تقویم شمسی واقعی نمایش داده می‌شوند.", color = Muted)
        }
        if (groups.isEmpty()) item { EmptyState("کاری زمان‌بندی نشده", "برای یک کار تاریخ و ساعت تعیین کن تا اینجا دیده شود.") }
        groups.forEach { (_, itemsForDate) ->
            item { Text(PersianDate.fullDate(itemsForDate.first().dueAt!!), color = Accent, fontWeight = FontWeight.Bold) }
            items(itemsForDate, key = { it.id }) { TaskCard(it, onDone, onEdit) }
        }
    }
}

@Composable
private fun CompletedScreen(tasks: List<TaskItem>, onDone: (TaskItem, Boolean) -> Unit, onEdit: (TaskItem) -> Unit) {
    var filter by remember { mutableStateOf(CompletedFilter.WEEK) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val daysFromSaturday = (today.dayOfWeek.value - DayOfWeek.SATURDAY.value + 7) % 7
    val weekStart = today.minusDays(daysFromSaturday.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

    val done = tasks.filter { it.status == TaskStatus.DONE }.sortedByDescending { it.completedAt ?: it.updatedAt }
    val filtered = done.filter { task ->
        val completed = task.completedAt ?: task.updatedAt
        when (filter) {
            CompletedFilter.TODAY -> completed in todayStart until tomorrowStart
            CompletedFilter.WEEK -> completed >= weekStart
            CompletedFilter.MONTH -> PersianDate.sameJalaliMonth(completed)
            CompletedFilter.ALL -> true
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("انجام‌شده‌ها", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("کارهای انجام‌شده حذف نمی‌شوند و اینجا می‌مانند.", color = Muted)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(CompletedFilter.entries) { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
                }
            }
        }
        if (filtered.isEmpty()) item { EmptyState("هنوز کاری ثبت نشده", "وقتی کاری را انجام‌شده علامت بزنی، اینجا دیده می‌شود.") }
        items(filtered, key = { it.id }) { TaskCard(it, onDone, onEdit) }
    }
}

@Composable
private fun ReportsScreen(tasks: List<TaskItem>, routines: List<RoutineEntry>, projects: List<ProjectItem>) {
    val zone = ZoneId.systemDefault()
    val now = System.currentTimeMillis()
    val today = LocalDate.now(zone)
    val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val daysFromSaturday = (today.dayOfWeek.value - DayOfWeek.SATURDAY.value + 7) % 7
    val weekStart = today.minusDays(daysFromSaturday.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

    val total = tasks.size
    val done = tasks.filter { it.status == TaskStatus.DONE }
    val open = tasks.filter { it.status != TaskStatus.DONE }
    val completedToday = done.count { (it.completedAt ?: it.updatedAt) in todayStart until tomorrowStart }
    val completedWeek = done.count { (it.completedAt ?: it.updatedAt) >= weekStart }
    val completedMonth = done.count { PersianDate.sameJalaliMonth(it.completedAt ?: it.updatedAt, now) }
    val overdue = open.count { it.dueAt != null && it.dueAt < now }
    val completionPercent = if (total == 0) 0 else ((done.size * 100.0) / total).toInt()
    val routineDone = routines.count { it.done }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("گزارش‌ها", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("خلاصه عملکرد کارها، روتین‌ها و پروژه‌ها", color = Muted)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportMetric("امروز", completedToday, Accent, Modifier.weight(1f))
                ReportMetric("این هفته", completedWeek, Warning, Modifier.weight(1f))
                ReportMetric("این ماه", completedMonth, Color.White, Modifier.weight(1f))
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = Surface1, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("وضعیت کل کارها", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(12.dp))
                    ReportLine("کل کارهای ثبت‌شده", total)
                    ReportLine("انجام‌شده", done.size)
                    ReportLine("باز و در انتظار", open.size)
                    ReportLine("عقب‌افتاده", overdue, Danger)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { completionPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(9.dp),
                        color = Accent,
                        trackColor = Surface2
                    )
                    Spacer(Modifier.height(7.dp))
                    Text("درصد انجام کل: ${PersianDate.toFa(completionPercent)}٪", color = Accent, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ReportMetric("روتین امروز", routineDone, Accent, Modifier.weight(1f))
                ReportMetric("پروژه فعال", projects.count { it.status == "فعال" }, Warning, Modifier.weight(1f))
                ReportMetric("کل پروژه", projects.size, Color.White, Modifier.weight(1f))
            }
        }
        item { Text("بر اساس دسته‌بندی", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        items(TaskCategory.entries) { cat ->
            val allCat = tasks.filter { it.category == cat }
            val doneCat = allCat.count { it.status == TaskStatus.DONE }
            CategoryReportCard(cat, allCat.size, doneCat)
        }
    }
}

@Composable
private fun ReportMetric(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(18.dp), color = Surface1, modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(PersianDate.toFa(count), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ReportLine(label: String, value: Int, valueColor: Color = Color.White) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(PersianDate.toFa(value), color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryReportCard(category: TaskCategory, total: Int, done: Int) {
    val p = if (total == 0) 0f else done.toFloat() / total.toFloat()
    Surface(shape = RoundedCornerShape(18.dp), color = Surface1, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("${category.emoji} ${category.label}", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("${PersianDate.toFa(done)} / ${PersianDate.toFa(total)}", color = Accent)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { p }, modifier = Modifier.fillMaxWidth().height(6.dp), color = Accent, trackColor = Surface2)
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Surface(shape = RoundedCornerShape(22.dp), color = Surface1, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✓", color = Accent, fontSize = 34.sp)
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text(body, color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskEditorDialog(
    task: TaskItem?,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var notes by remember(task?.id) { mutableStateOf(task?.notes ?: "") }
    var contextName by remember(task?.id) { mutableStateOf(task?.contextName ?: "") }
    var category by remember(task?.id) { mutableStateOf<TaskCategory?>(task?.category) }
    var priority by remember(task?.id) { mutableStateOf<TaskPriority?>(task?.priority) }
    var waiting by remember(task?.id) { mutableStateOf(task?.status == TaskStatus.WAITING) }
    var dueAt by remember(task?.id) { mutableStateOf(task?.dueAt) }
    var reminderEnabled by remember(task?.id) { mutableStateOf(task?.reminderAt != null || task == null) }
    var showJalaliPicker by remember { mutableStateOf(false) }

    val chooseTime = {
        val zdt = Instant.ofEpochMilli(dueAt ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault())
        TimePickerDialog(context, { _, h, m ->
            val date = dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.now()
            dueAt = LocalDateTime.of(date, LocalTime.of(h, m)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }, zdt.hour, zdt.minute, true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(if (task == null) "کار جدید" else "ویرایش کار") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 620.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("چی رو نباید فراموش کنم؟") },
                        placeholder = { Text("مثلاً: فردا ساعت ۹ پیگیری پیمانکار") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    Text("می‌توانی «امروز»، «فردا»، «ساعت ۹»، «فوری» و… را داخل متن بنویسی.", color = Muted, fontSize = 11.sp)
                }
                item {
                    Text("دسته‌بندی", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = category == null, onClick = { category = null }, label = { Text("خودکار") })
                        TaskCategory.entries.forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text("${cat.emoji} ${cat.label}") })
                        }
                    }
                }
                item {
                    Text("اولویت", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        FilterChip(selected = priority == null, onClick = { priority = null }, label = { Text("خودکار") })
                        TaskPriority.entries.forEach { p -> FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) }) }
                    }
                }
                item {
                    OutlinedTextField(
                        value = contextName,
                        onValueChange = { contextName = it },
                        label = { Text("نام پروژه یا شخص (اختیاری)") },
                        placeholder = { Text("مثلاً: پروژه قصرالدشت / علی") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("تاریخ و ساعت یادآوری", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showJalaliPicker = true }, modifier = Modifier.weight(1f)) {
                            Text(dueAt?.let { PersianDate.shortDate(it) } ?: "تقویم شمسی")
                        }
                        OutlinedButton(onClick = chooseTime, modifier = Modifier.weight(1f)) {
                            Text(dueAt?.let { PersianDate.time(it) } ?: "ساعت")
                        }
                    }
                    if (dueAt != null) TextButton(onClick = { dueAt = null }) { Text("حذف تاریخ و ساعت") }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                        Spacer(Modifier.width(8.dp))
                        Text("یادآوری در زمان کار")
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = waiting, onCheckedChange = { waiting = it })
                        Spacer(Modifier.width(8.dp))
                        Text("منتظر انجام یا جواب شخص دیگری هستم")
                    }
                }
                item {
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("توضیحات (اختیاری)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
                if (onDelete != null) {
                    item { TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Danger)) { Text("حذف این کار") } }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(TaskDraft(task?.id, title, notes, category, priority, waiting, contextName, dueAt, reminderEnabled))
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )

    if (showJalaliPicker) {
        JalaliDatePickerDialog(
            initialMillis = dueAt ?: System.currentTimeMillis(),
            onDismiss = { showJalaliPicker = false },
            onSelect = { jalali ->
                val gregorian = PersianDate.toGregorian(jalali)
                val time = dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() } ?: LocalTime.of(9, 0)
                dueAt = LocalDateTime.of(gregorian, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                showJalaliPicker = false
            }
        )
    }
}

@Composable
private fun JalaliDatePickerDialog(initialMillis: Long, onDismiss: () -> Unit, onSelect: (JalaliDate) -> Unit) {
    val zone = ZoneId.systemDefault()
    val initial = PersianDate.fromGregorian(Instant.ofEpochMilli(initialMillis).atZone(zone).toLocalDate())
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.month) }
    var selectedDay by remember { mutableIntStateOf(initial.day) }

    fun previousMonth() {
        if (month == 1) { month = 12; year-- } else month--
        selectedDay = selectedDay.coerceAtMost(PersianDate.daysInMonth(year, month))
    }
    fun nextMonth() {
        if (month == 12) { month = 1; year++ } else month++
        selectedDay = selectedDay.coerceAtMost(PersianDate.daysInMonth(year, month))
    }

    val firstDow = PersianDate.toGregorian(JalaliDate(year, month, 1)).dayOfWeek
    val offset = when (firstDow) {
        DayOfWeek.SATURDAY -> 0
        DayOfWeek.SUNDAY -> 1
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
    }
    val days = PersianDate.daysInMonth(year, month)
    val cellDays = List(offset) { 0 } + (1..days).toList()
    val padded = cellDays + List((7 - cellDays.size % 7) % 7) { 0 }
    val rows = padded.chunked(7)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("انتخاب تاریخ شمسی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = ::nextMonth) { Text("‹", fontSize = 28.sp) }
                    Text(
                        "${PersianDate.monthName(month)} ${PersianDate.toFa(year)}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = ::previousMonth) { Text("›", fontSize = 28.sp) }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("ش", "ی", "د", "س", "چ", "پ", "ج").forEach { dayName ->
                        Text(dayName, color = Muted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    }
                }
                rows.forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        week.forEach { day ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (day == 0) {
                                    Spacer(Modifier.size(38.dp))
                                } else if (day == selectedDay) {
                                    Button(
                                        onClick = { selectedDay = day },
                                        modifier = Modifier.size(38.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
                                    ) { Text(PersianDate.toFa(day), fontSize = 11.sp) }
                                } else {
                                    OutlinedButton(
                                        onClick = { selectedDay = day },
                                        modifier = Modifier.size(38.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text(PersianDate.toFa(day), fontSize = 11.sp) }
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = {
                    val t = PersianDate.fromGregorian(LocalDate.now())
                    year = t.year; month = t.month; selectedDay = t.day
                }) { Text("امروز") }
            }
        },
        confirmButton = { Button(onClick = { onSelect(JalaliDate(year, month, selectedDay)) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) { Text("انتخاب") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun RoutineScreen(routines: List<RoutineEntry>, onToggle: (RoutineEntry) -> Unit, onAdjust: (RoutineEntry, Double) -> Unit) {
    val doneCount = routines.count { it.done }
    val total = routines.size.coerceAtLeast(1)
    val progress = doneCount.toFloat() / total.toFloat()
    val prepared = remember { PreparedDailyContent.today() }
    var detailKey by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("روتین امروز", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("روی زبان، قرآن یا کتاب بزن و همان‌جا انجامش بده.", color = Muted)
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = Surface1, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("پیشرفت امروز", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${PersianDate.toFa(doneCount)} از ${PersianDate.toFa(routines.size)}", color = Accent)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Accent, trackColor = Surface2)
                }
            }
        }
        item { Text("انجام مستقیم", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item { PreparedCard("🌐", "زبان امروز", prepared.languagePreview) { detailKey = "language" } }
        item { PreparedCard("📖", "قرآن امروز", prepared.quranPreview) { detailKey = "quran" } }
        item { PreparedCard("📚", "کتاب امروز", prepared.bookPreview) { detailKey = "book" } }
        item { Spacer(Modifier.height(4.dp)); Text("ثبت روتین‌ها", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        items(routines, key = { it.template.key }) { entry -> RoutineCard(entry, onToggle, onAdjust) }
    }

    val selectedRoutine = routines.firstOrNull { it.template.key == detailKey }
    if (detailKey != null && selectedRoutine != null) {
        RoutineDetailDialog(
            key = detailKey!!,
            prepared = prepared,
            alreadyDone = selectedRoutine.done,
            onDismiss = { detailKey = null },
            onComplete = {
                if (!selectedRoutine.done) onToggle(selectedRoutine)
                detailKey = null
            }
        )
    }
}

@Composable
private fun PreparedCard(emoji: String, title: String, body: String, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = Surface1, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text("$emoji $title", fontWeight = FontWeight.Bold, color = Accent, modifier = Modifier.weight(1f))
                Text("باز کردن", color = Accent, fontSize = 11.sp)
            }
            Spacer(Modifier.height(7.dp))
            Text(body, color = Color.White, fontSize = 13.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun RoutineDetailDialog(
    key: String,
    prepared: DailyPreparedContent,
    alreadyDone: Boolean,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val title = when (key) {
        "language" -> "🌐 زبان امروز"
        "quran" -> "📖 قرآن امروز"
        else -> "📚 کتاب امروز"
    }
    val body = when (key) {
        "language" -> prepared.languageLesson
        "quran" -> prepared.quranLesson
        else -> prepared.bookLesson
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp)) {
                item { Text(body, lineHeight = 24.sp) }
            }
        },
        confirmButton = {
            Button(onClick = onComplete, enabled = !alreadyDone) {
                Text(if (alreadyDone) "قبلاً انجام شده ✓" else "انجام شد")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}

@Composable
private fun RoutineCard(entry: RoutineEntry, onToggle: (RoutineEntry) -> Unit, onAdjust: (RoutineEntry, Double) -> Unit) {
    val t = entry.template
    Surface(shape = RoundedCornerShape(18.dp), color = if (entry.done) Accent.copy(alpha = 0.12f) else Surface1, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.emoji, fontSize = 25.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontWeight = FontWeight.Bold)
                    Text(t.subtitle, color = Muted, fontSize = 12.sp)
                }
                if (t.kind == RoutineKind.TOGGLE) {
                    Checkbox(checked = entry.done, onCheckedChange = { onToggle(entry) }, colors = CheckboxDefaults.colors(checkedColor = Accent))
                } else if (entry.done) Text("✓", color = Accent, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            }
            if (t.kind != RoutineKind.TOGGLE) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onAdjust(entry, -t.step) }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("−") }
                    Text(
                        if (t.key == "sleep") "${PersianDate.toFa(formatRoutineValue(entry.value))} ${t.unit}" else "${PersianDate.toFa(formatRoutineValue(entry.value))} / ${PersianDate.toFa(formatRoutineValue(t.target))} ${t.unit}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { onAdjust(entry, t.step) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text("+") }
                }
            }
        }
    }
}

private fun formatRoutineValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.1f", value)
