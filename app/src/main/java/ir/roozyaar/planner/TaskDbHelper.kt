package ir.roozyaar.planner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TaskDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                notes TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL,
                priority TEXT NOT NULL,
                status TEXT NOT NULL,
                context_name TEXT NOT NULL DEFAULT '',
                due_at INTEGER,
                reminder_at INTEGER,
                completed_at INTEGER,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_tasks_due_at ON tasks(due_at)")
        db.execSQL("CREATE INDEX idx_tasks_status ON tasks(status)")
        db.execSQL("CREATE INDEX idx_tasks_completed_at ON tasks(completed_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN completed_at INTEGER")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tasks_completed_at ON tasks(completed_at)")
            db.execSQL("UPDATE tasks SET completed_at = updated_at WHERE status = 'DONE' AND completed_at IS NULL")
        }
    }

    fun insert(task: TaskItem): Long = writableDatabase.insertOrThrow("tasks", null, task.toValues(false))

    fun update(task: TaskItem) {
        writableDatabase.update("tasks", task.toValues(false), "id=?", arrayOf(task.id.toString()))
    }

    fun delete(id: Long) {
        writableDatabase.delete("tasks", "id=?", arrayOf(id.toString()))
    }

    fun get(id: Long): TaskItem? {
        readableDatabase.query("tasks", null, "id=?", arrayOf(id.toString()), null, null, null, "1").use { c ->
            return if (c.moveToFirst()) c.toTask() else null
        }
    }

    fun all(): List<TaskItem> {
        val result = mutableListOf<TaskItem>()
        readableDatabase.rawQuery(
            """
            SELECT * FROM tasks
            ORDER BY
                CASE status WHEN 'DONE' THEN 1 ELSE 0 END,
                CASE WHEN due_at IS NULL THEN 1 ELSE 0 END,
                due_at ASC,
                CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 ELSE 2 END,
                created_at DESC
            """.trimIndent(), null
        ).use { c -> while (c.moveToNext()) result += c.toTask() }
        return result
    }

    fun pendingReminders(now: Long): List<TaskItem> {
        val result = mutableListOf<TaskItem>()
        readableDatabase.query(
            "tasks", null,
            "status != ? AND reminder_at IS NOT NULL AND reminder_at > ?",
            arrayOf(TaskStatus.DONE.name, now.toString()), null, null, "reminder_at ASC"
        ).use { c -> while (c.moveToNext()) result += c.toTask() }
        return result
    }

    private fun TaskItem.toValues(includeId: Boolean): ContentValues = ContentValues().apply {
        if (includeId) put("id", id)
        put("title", title)
        put("notes", notes)
        put("category", category.name)
        put("priority", priority.name)
        put("status", status.name)
        put("context_name", contextName)
        if (dueAt == null) putNull("due_at") else put("due_at", dueAt)
        if (reminderAt == null) putNull("reminder_at") else put("reminder_at", reminderAt)
        if (completedAt == null) putNull("completed_at") else put("completed_at", completedAt)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
    }

    private fun android.database.Cursor.toTask(): TaskItem {
        fun str(name: String) = getString(getColumnIndexOrThrow(name))
        fun lng(name: String): Long? {
            val i = getColumnIndex(name)
            return if (i < 0 || isNull(i)) null else getLong(i)
        }
        return TaskItem(
            id = getLong(getColumnIndexOrThrow("id")),
            title = str("title"),
            notes = str("notes"),
            category = runCatching { TaskCategory.valueOf(str("category")) }.getOrDefault(TaskCategory.PERSONAL),
            priority = runCatching { TaskPriority.valueOf(str("priority")) }.getOrDefault(TaskPriority.NORMAL),
            status = runCatching { TaskStatus.valueOf(str("status")) }.getOrDefault(TaskStatus.ACTIVE),
            contextName = str("context_name"),
            dueAt = lng("due_at"),
            reminderAt = lng("reminder_at"),
            completedAt = lng("completed_at"),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
            updatedAt = getLong(getColumnIndexOrThrow("updated_at"))
        )
    }

    companion object {
        private const val DB_NAME = "roozyaar.db"
        private const val DB_VERSION = 2
    }
}
