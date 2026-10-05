package ir.roozyaar.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val db = TaskDbHelper(app)
    private val routineDb = RoutineDbHelper(app)
    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    private val _routines = MutableStateFlow<List<RoutineEntry>>(emptyList())
    val routines: StateFlow<List<RoutineEntry>> = _routines.asStateFlow()

    init {
        refresh()
        refreshRoutines()
    }

    fun refresh() {
        viewModelScope.launch {
            _tasks.value = withContext(Dispatchers.IO) { db.all() }
        }
    }

    fun refreshRoutines() {
        viewModelScope.launch {
            _routines.value = withContext(Dispatchers.IO) { routineDb.entries() }
        }
    }

    fun toggleRoutine(entry: RoutineEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val newDone = !entry.done
            val value = if (entry.template.kind == RoutineKind.TOGGLE) {
                if (newDone) 1.0 else 0.0
            } else entry.value
            routineDb.save(entry.template.key, value, newDone)
            _routines.value = routineDb.entries()
        }
    }

    fun adjustRoutine(entry: RoutineEntry, delta: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val t = entry.template
            val newValue = (entry.value + delta).coerceAtLeast(0.0)
            val done = when (t.key) {
                "sleep" -> newValue > 0.0
                else -> newValue >= t.target
            }
            routineDb.save(t.key, newValue, done)
            _routines.value = routineDb.entries()
        }
    }

    fun save(draft: TaskDraft) {
        if (draft.title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = QuickParser.parse(draft.title)
            val now = System.currentTimeMillis()
            val category = draft.category ?: parsed.category
            val priority = draft.priority ?: parsed.priority
            val status = if (draft.waiting || parsed.waiting) TaskStatus.WAITING else TaskStatus.ACTIVE
            val due = draft.dueAt ?: parsed.dueAt
            val reminder = if (draft.reminderEnabled) due else null

            if (draft.id == null) {
                val base = TaskItem(
                    title = draft.title.trim(),
                    notes = draft.notes.trim(),
                    category = category,
                    priority = priority,
                    status = status,
                    contextName = draft.contextName.trim(),
                    dueAt = due,
                    reminderAt = reminder,
                    createdAt = now,
                    updatedAt = now
                )
                val id = db.insert(base)
                ReminderScheduler.schedule(getApplication(), base.copy(id = id))
            } else {
                val old = db.get(draft.id) ?: return@launch
                ReminderScheduler.cancel(getApplication(), old.id)
                val updated = old.copy(
                    title = draft.title.trim(),
                    notes = draft.notes.trim(),
                    category = category,
                    priority = priority,
                    status = if (old.status == TaskStatus.DONE) TaskStatus.DONE else status,
                    contextName = draft.contextName.trim(),
                    dueAt = due,
                    reminderAt = if (old.status == TaskStatus.DONE) null else reminder,
                    updatedAt = now
                )
                db.update(updated)
                ReminderScheduler.schedule(getApplication(), updated)
            }
            _tasks.value = db.all()
        }
    }

    fun setDone(task: TaskItem, done: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = task.copy(
                status = if (done) TaskStatus.DONE else TaskStatus.ACTIVE,
                reminderAt = null,
                updatedAt = System.currentTimeMillis()
            )
            db.update(updated)
            ReminderScheduler.cancel(getApplication(), task.id)
            if (!done) ReminderScheduler.schedule(getApplication(), updated)
            _tasks.value = db.all()
        }
    }

    fun delete(task: TaskItem) {
        viewModelScope.launch(Dispatchers.IO) {
            ReminderScheduler.cancel(getApplication(), task.id)
            db.delete(task.id)
            _tasks.value = db.all()
        }
    }
}
