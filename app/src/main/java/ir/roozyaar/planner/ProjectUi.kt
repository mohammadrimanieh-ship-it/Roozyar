package ir.roozyaar.planner

import android.content.Intent
import android.net.Uri
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun ProjectsScreen(viewModel: TaskViewModel) {
    val projects by viewModel.projects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    var editor by remember { mutableStateOf<ProjectItem?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = projects.firstOrNull { it.id == selectedId }

    if (selected != null) {
        ProjectDetailScreen(
            project = selected,
            tasks = tasks,
            viewModel = viewModel,
            onBack = { selectedId = null },
            onEdit = { editor = selected; showEditor = true }
        )
    } else {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("پروژه‌ها", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text("ساخت‌وساز و پروژه‌های مختلف را جدا مدیریت کن", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = { editor = null; showEditor = true }) { Text("+ پروژه") }
                }
            }
            if (projects.isEmpty()) {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏗️", fontSize = 36.sp)
                            Text("هنوز پروژه‌ای نداری", fontWeight = FontWeight.Bold)
                            Text("اولین پروژه ساخت‌وساز یا شخصی را بساز.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(projects, key = { it.id }) { p ->
                val projectTasks = tasks.filter { it.category == TaskCategory.PROJECTS && it.contextName == p.name }
                val open = projectTasks.count { it.status != TaskStatus.DONE }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().clickable { selectedId = p.id }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text("🏗️ ${p.name}", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
                            Text(p.status, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                        if (p.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(p.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(7.dp))
                        Text("${p.category} • ${PersianDate.toFa(open)} کار باز", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showEditor) {
        ProjectEditorDialog(
            project = editor,
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.saveProject(it)
                showEditor = false
            },
            onDelete = editor?.let { p ->
                {
                    viewModel.deleteProject(p)
                    if (selectedId == p.id) selectedId = null
                    showEditor = false
                }
            }
        )
    }
}

@Composable
private fun ProjectDetailScreen(
    project: ProjectItem,
    tasks: List<TaskItem>,
    viewModel: TaskViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val photos by viewModel.projectPhotos.collectAsState()
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TaskItem?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            pendingUri = uri
        }
    }

    LaunchedEffect(project.id) { viewModel.loadProjectPhotos(project.id) }
    val projectTasks = tasks.filter { it.category == TaskCategory.PROJECTS && it.contextName == project.name }
    val openTasks = projectTasks.filter { it.status != TaskStatus.DONE }
    val doneTasks = projectTasks.filter { it.status == TaskStatus.DONE }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 20.dp, 18.dp, 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("بازگشت") }
                Column(Modifier.weight(1f)) {
                    Text(project.name, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${project.category} • ${project.status}", color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = onEdit) { Text("ویرایش") }
            }
            if (project.description.isNotBlank()) {
                Text(project.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { taskToEdit = null; showTaskDialog = true }, modifier = Modifier.weight(1f)) {
                    Text("+ کار پروژه")
                }
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.weight(1f)) {
                    Text("+ عکس")
                }
            }
        }

        if (pendingUri != null) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    adjustViewBounds = true
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                }
                            },
                            update = { it.setImageURI(pendingUri) },
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            label = { Text("توضیح عکس (اختیاری)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row {
                            Button(onClick = {
                                viewModel.addProjectPhoto(project.id, pendingUri.toString(), caption.trim())
                                pendingUri = null
                                caption = ""
                            }) { Text("ذخیره عکس") }
                            TextButton(onClick = { pendingUri = null; caption = "" }) { Text("انصراف") }
                        }
                    }
                }
            }
        }

        item {
            Text("کارهای پروژه", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${PersianDate.toFa(openTasks.size)} باز • ${PersianDate.toFa(doneTasks.size)} انجام‌شده", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        if (projectTasks.isEmpty()) {
            item { Text("هنوز کاری برای این پروژه ثبت نشده.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(openTasks, key = { it.id }) { task ->
            ProjectTaskCard(task, onDone = { viewModel.setDone(task, true) }, onEdit = {
                taskToEdit = task; showTaskDialog = true
            })
        }
        if (doneTasks.isNotEmpty()) {
            item { Text("انجام‌شده‌ها", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
            items(doneTasks, key = { it.id }) { task ->
                ProjectTaskCard(task, onDone = { viewModel.setDone(task, false) }, onEdit = {
                    taskToEdit = task; showTaskDialog = true
                })
            }
        }

        item { Text("عکس‌های پروژه (${PersianDate.toFa(photos.size)})", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        if (photos.isEmpty()) {
            item { Text("هنوز عکسی برای این پروژه ثبت نشده.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(photos, key = { it.id }) { photo ->
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    AndroidView(
                        factory = { ctx ->
                            ImageView(ctx).apply {
                                adjustViewBounds = true
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                        },
                        update = { image ->
                            runCatching { image.setImageURI(Uri.parse(photo.uri)) }
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp, max = 280.dp)
                    )
                    if (photo.caption.isNotBlank()) {
                        Spacer(Modifier.height(7.dp))
                        Text(photo.caption, fontWeight = FontWeight.SemiBold)
                    }
                    Text(PersianDate.fullDate(photo.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Row {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(photo.uri)).apply {
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                )
                            }
                        }) { Text("مشاهده") }
                        TextButton(onClick = { viewModel.deleteProjectPhoto(project.id, photo.id) }) { Text("حذف") }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        ProjectTaskDialog(
            projectName = project.name,
            task = taskToEdit,
            onDismiss = { showTaskDialog = false; taskToEdit = null },
            onSave = { draft ->
                viewModel.save(draft)
                showTaskDialog = false
                taskToEdit = null
            },
            onDelete = taskToEdit?.let { t ->
                {
                    viewModel.delete(t)
                    showTaskDialog = false
                    taskToEdit = null
                }
            }
        )
    }
}

@Composable
private fun ProjectTaskCard(task: TaskItem, onDone: () -> Unit, onEdit: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.status == TaskStatus.DONE, onCheckedChange = { onDone() })
            Column(Modifier.weight(1f)) {
                Text(task.title, fontWeight = FontWeight.SemiBold)
                if (task.notes.isNotBlank()) Text(task.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                task.dueAt?.let { Text(PersianDate.relativeDue(it), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) }
            }
        }
    }
}

@Composable
private fun ProjectTaskDialog(
    projectName: String,
    task: TaskItem?,
    onDismiss: () -> Unit,
    onSave: (TaskDraft) -> Unit,
    onDelete: (() -> Unit)?
) {
    var title by remember(task?.id) { mutableStateOf(task?.title ?: "") }
    var notes by remember(task?.id) { mutableStateOf(task?.notes ?: "") }
    var priority by remember(task?.id) { mutableStateOf(task?.priority ?: TaskPriority.NORMAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (task == null) "کار جدید پروژه" else "ویرایش کار پروژه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("عنوان کار") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth())
                Text("اولویت", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p.label) })
                    }
                }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("حذف این کار") }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        TaskDraft(
                            id = task?.id,
                            title = title,
                            notes = notes,
                            category = TaskCategory.PROJECTS,
                            priority = priority,
                            waiting = task?.status == TaskStatus.WAITING,
                            contextName = projectName,
                            dueAt = task?.dueAt,
                            reminderEnabled = task?.reminderAt != null
                        )
                    )
                }
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun ProjectEditorDialog(
    project: ProjectItem?,
    onDismiss: () -> Unit,
    onSave: (ProjectItem) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(project?.id) { mutableStateOf(project?.name ?: "") }
    var description by remember(project?.id) { mutableStateOf(project?.description ?: "") }
    var category by remember(project?.id) { mutableStateOf(project?.category ?: "ساخت‌وساز") }
    var status by remember(project?.id) { mutableStateOf(project?.status ?: "فعال") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (project == null) "پروژه جدید" else "ویرایش پروژه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("نام پروژه") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Text("نوع پروژه", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("ساخت‌وساز", "خانه", "شخصی", "کاری", "سایر")) { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                    }
                }
                Text("وضعیت", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("فعال", "متوقف", "تمام‌شده")) { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) })
                    }
                }
                if (onDelete != null) TextButton(onClick = onDelete) { Text("حذف پروژه") }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    val now = System.currentTimeMillis()
                    onSave(
                        ProjectItem(
                            id = project?.id ?: 0L,
                            name = name.trim(),
                            description = description.trim(),
                            category = category,
                            status = status,
                            createdAt = project?.createdAt ?: now,
                            updatedAt = now
                        )
                    )
                }
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
