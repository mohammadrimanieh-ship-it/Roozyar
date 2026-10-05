package ir.roozyaar.planner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

class RoutineDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE routine_logs (
                day_key TEXT NOT NULL,
                habit_key TEXT NOT NULL,
                value REAL NOT NULL DEFAULT 0,
                done INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY(day_key, habit_key)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun entries(day: LocalDate = LocalDate.now()): List<RoutineEntry> {
        val values = mutableMapOf<String, Pair<Double, Boolean>>()
        readableDatabase.query(
            "routine_logs", arrayOf("habit_key", "value", "done"), "day_key=?",
            arrayOf(day.toString()), null, null, null
        ).use { c ->
            while (c.moveToNext()) {
                values[c.getString(0)] = c.getDouble(1) to (c.getInt(2) == 1)
            }
        }
        return DefaultRoutines.all.map { t ->
            val stored = values[t.key]
            RoutineEntry(t, stored?.first ?: 0.0, stored?.second ?: false)
        }
    }

    fun save(key: String, value: Double, done: Boolean, day: LocalDate = LocalDate.now()) {
        val cv = ContentValues().apply {
            put("day_key", day.toString())
            put("habit_key", key)
            put("value", value)
            put("done", if (done) 1 else 0)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("routine_logs", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    companion object {
        private const val DB_NAME = "roozyaar_routines.db"
        private const val DB_VERSION = 1
    }
}
