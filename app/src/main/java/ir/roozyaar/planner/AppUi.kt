package ir.roozyaar.planner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar

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

private enum class MainScreen { TODAY, ROUTINE, CALENDAR, CATEGORIES }

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
                floatingActionButtonPosition = FabPosition.Center,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { editorTask = null; showEditor = true },
                        containerColor = Accent,
                        contentColor = Bg
                    ) { Text("＋", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                },
                bottomBar = {
                    NavigationBar(containerColor = Surface1) {
                        NavItem("✓", "امروز", screen == MainScreen.TODAY) {
                            screen = MainScreen.TODAY
                        }
                        NavItem("◉", "روتین", screen == MainScreen.ROUTINE) {
                            viewModel.refreshRoutines()
                            screen = MainScreen.ROUTINE
                        }
                        Spacer(Modifier.width(52.dp))
                        NavItem("▦", "تقویم", screen == MainScreen.CALENDAR) {
                            screen = MainScreen.CALENDAR
                        }
                        NavItem("▤", "دسته‌ها", screen == MainScreen.CATEGORIES) {
                            screen = MainScreen.CATEGORIES
                        }
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
                        MainScreen.ROUTINE -> RoutineScreen(
                            routines = routines,
                            onToggle = viewModel::toggleRoutine,
                            onAdjust = viewModel::adjustRoutine
                        )
                        MainScreen.CALENDAR -> CalendarScreen(tasks, viewModel::setDone) {
                            editorTask = it; showEditor = true
                        }
                        MainScreen.CATEGORIES -> CategoriesScreen(tasks) { category, waiting ->
                            categoryFilter = category
                            waitingOnly = waiting
                            screen = MainScreen.TODAY
                        }
                    }
                }
            }

            if (showEditor) {
                TaskEditorDialog(
                    task = editorTask,
                    onDismiss = { showEditor = false },
                    onSave = {
                        viewModel.save(it)
                        showEditor = false
                    },
                    onDelete = editorTask?.let { task ->
                        { viewModel.delete(task); showEditor = false }
                    }
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
        icon = { Text(icon, fontSize = 20.sp) },
        label = { Text(label) }
    )
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
    val now = System.currentTimeMillis()
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
    val next = filtered
        .filter { it.status == TaskStatus.ACTIVE }
        .minWithOrNull(compareBy<TaskItem> { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.priority.ordinal })
    val dueNow = filtered.filter { it.status == TaskStatus.ACTIVE && it.dueAt != null && it.dueAt <= endToday }
    val future = filtered.filter { it.status == TaskStatus.ACTIVE && it.dueAt != null && it.dueAt > endToday }
    val noDate = filtered.filter { it.status == TaskStatus.ACTIVE && it.dueAt == null }
    val waiting = filtered.filter { it.status == TaskStatus.WAITING }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("روز‌یار", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(PersianDate.header(now), color = Muted, fontSize = 14.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("فوری", urgentCount, Danger, Modifier.weight(1f))
                StatCard("امروز", dueCount, Warning, Modifier.weight(1f))
                StatCard("منتظر", waitingCount, Accent, Modifier.weight(1f))
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = categoryFilter == null && !waitingOnly,
                        onClick = { onFilterCategory(null) },
                        label = { Text("همه") }
                    )
                }
                item {
                    FilterChip(
                        selected = waitingOnly,
                        onClick = { onWaiting(!waitingOnly) },
                        label = { Text("⏳ منتظر دیگران") }
                    )
                }
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
        if (filtered.isEmpty()) {
            item {
                EmptyState("فعلاً کاری اینجا نیست", "با دکمه + اولین کار را خیلی سریع ثبت کن.")
            }
        }
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
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().clickable { onEdit(task) }
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${task.category.emoji} ${task.category.label}", color = Accent, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                task.dueAt?.let { Text("⏰ ${PersianDate.relativeDue(it)}", color = Muted, fontSize = 13.sp) }
            }
            Button(onClick = { onDone(task, true) }, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)) {
                Text("انجام شد")
            }
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
private fun TaskCard(task: TaskItem, onDone: (TaskItem, Boolean) -> Unit, onEdit: (TaskItem) -> Unit) {
    val overdue = task.dueAt?.let { it < System.currentTimeMillis() } == true && task.status != TaskStatus.DONE
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Surface1,
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
                    if (task.priority != TaskPriority.NORMAL) {
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
                task.dueAt?.let {
                    Text(
                        (if (overdue) "عقب‌افتاده • " else "") + PersianDate.relativeDue(it),
                        color = if (overdue) Danger else Muted,
                        fontSize = 12.sp
                    )
                }
                if (task.status == TaskStatus.WAITING) {
                    Text("⏳ منتظر دیگران", color = Accent, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(
    tasks: List<TaskItem>,
    onDone: (TaskItem, Boolean) -> Unit,
    onEdit: (TaskItem) -> Unit
) {
    val dated = tasks.filter { it.status != TaskStatus.DONE && it.dueAt != null }.sortedBy { it.dueAt }
    val zone = ZoneId.systemDefault()
    val groups = dated.groupBy { Instant.ofEpochMilli(it.dueAt!!).atZone(zone).toLocalDate() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("تقویم کارها", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("کارهای زمان‌دار آینده و عقب‌افتاده", color = Muted)
        }
        if (groups.isEmpty()) item { EmptyState("کاری زمان‌بندی نشده", "برای یک کار تاریخ و ساعت تعیین کن تا اینجا دیده شود.") }
        groups.forEach { (_, itemsForDate) ->
            item { Text(PersianDate.fullDate(itemsForDate.first().dueAt!!), color = Accent, fontWeight = FontWeight.Bold) }
            items(itemsForDate, key = { it.id }) { TaskCard(it, onDone, onEdit) }
        }
    }
}

@Composable
private fun CategoriesScreen(tasks: List<TaskItem>, onOpen: (TaskCategory?, Boolean) -> Unit) {
    val open = tasks.filter { it.status != TaskStatus.DONE }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("دسته‌ها", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("کارها را بر اساس بخش‌های زندگی و پروژه‌ها جدا نگه دار.", color = Muted)
        }
        item {
            CategoryCard("⏳", "منتظر دیگران", open.count { it.status == TaskStatus.WAITING }, Accent) {
                onOpen(null, true)
            }
        }
        items(TaskCategory.entries) { cat ->
            CategoryCard(cat.emoji, cat.label, open.count { it.category == cat }, Color.White) {
                onOpen(cat, false)
            }
        }
    }
}

@Composable
private fun CategoryCard(emoji: String, label: String, count: Int, color: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Surface1,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = color)
            Text("${PersianDate.toFa(count)} کار", color = Muted)
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

    val chooseDate = {
        val zdt = Instant.ofEpochMilli(dueAt ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault())
        DatePickerDialog(context, { _, y, m, d ->
            val time = dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() } ?: LocalTime.of(9, 0)
            dueAt = LocalDateTime.of(LocalDate.of(y, m + 1, d), time)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }, zdt.year, zdt.monthValue - 1, zdt.dayOfMonth).show()
    }
    val chooseTime = {
        val zdt = Instant.ofEpochMilli(dueAt ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault())
        TimePickerDialog(context, { _, h, m ->
            val date = dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.now()
            dueAt = LocalDateTime.of(date, LocalTime.of(h, m))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }, zdt.hour, zdt.minute, true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text(if (task == null) "کار جدید" else "ویرایش کار") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.heightIn(max = 600.dp)) {
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
                        TaskPriority.entries.forEach { p ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                        }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = chooseDate, modifier = Modifier.weight(1f)) {
                            Text(dueAt?.let { PersianDate.shortDate(it) } ?: "تاریخ")
                        }
                        OutlinedButton(onClick = chooseTime, modifier = Modifier.weight(1f)) {
                            Text(dueAt?.let { PersianDate.time(it) } ?: "ساعت")
                        }
                        if (dueAt != null) TextButton(onClick = { dueAt = null }) { Text("حذف") }
                    }
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
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("توضیحات (اختیاری)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                if (onDelete != null) {
                    item {
                        TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Danger)) {
                            Text("حذف این کار")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(TaskDraft(
                        id = task?.id,
                        title = title,
                        notes = notes,
                        category = category,
                        priority = priority,
                        waiting = waiting,
                        contextName = contextName,
                        dueAt = dueAt,
                        reminderEnabled = reminderEnabled
                    ))
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun RoutineScreen(
    routines: List<RoutineEntry>,
    onToggle: (RoutineEntry) -> Unit,
    onAdjust: (RoutineEntry, Double) -> Unit
) {
    val doneCount = routines.count { it.done }
    val total = routines.size.coerceAtLeast(1)
    val progress = doneCount.toFloat() / total.toFloat()
    val prepared = remember { PreparedDailyContent.today() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 22.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("روتین امروز", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("چیزهایی که هر روز بهتره از قلم نیفتند", color = Muted)
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp), color = Surface1, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("پیشرفت امروز", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("${PersianDate.toFa(doneCount)} از ${PersianDate.toFa(routines.size)}", color = Accent)
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Accent,
                        trackColor = Surface2
                    )
                }
            }
        }

        item { Text("آماده برای امروز", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        item { PreparedCard("🌐", "زبان امروز", prepared.language) }
        item { PreparedCard("📖", "قرآن امروز", prepared.quran) }
        item { PreparedCard("📚", "کتاب پیشنهادی", prepared.book) }

        item {
            Spacer(Modifier.height(4.dp))
            Text("ثبت روتین‌ها", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        items(routines, key = { it.template.key }) { entry ->
            RoutineCard(entry, onToggle, onAdjust)
        }
    }
}

@Composable
private fun PreparedCard(emoji: String, title: String, body: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = Surface1, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("$emoji $title", fontWeight = FontWeight.Bold, color = Accent)
            Spacer(Modifier.height(7.dp))
            Text(body, color = Color.White, fontSize = 13.sp, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun RoutineCard(
    entry: RoutineEntry,
    onToggle: (RoutineEntry) -> Unit,
    onAdjust: (RoutineEntry, Double) -> Unit
) {
    val t = entry.template
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (entry.done) Accent.copy(alpha = 0.12f) else Surface1,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t.emoji, fontSize = 25.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.title, fontWeight = FontWeight.Bold)
                    Text(t.subtitle, color = Muted, fontSize = 12.sp)
                }
                if (t.kind == RoutineKind.TOGGLE) {
                    Checkbox(
                        checked = entry.done,
                        onCheckedChange = { onToggle(entry) },
                        colors = CheckboxDefaults.colors(checkedColor = Accent)
                    )
                } else if (entry.done) {
                    Text("✓", color = Accent, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
            }

            if (t.kind != RoutineKind.TOGGLE) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onAdjust(entry, -t.step) },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("−") }

                    Text(
                        if (t.key == "sleep") {
                            "${formatRoutineValue(entry.value)} ${t.unit}"
                        } else {
                            "${PersianDate.toFa(formatRoutineValue(entry.value))} / ${PersianDate.toFa(formatRoutineValue(t.target))} ${t.unit}"
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { onAdjust(entry, t.step) },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) { Text("+") }
                }
            }
        }
    }
}

private fun formatRoutineValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.US, "%.1f", value)
}
