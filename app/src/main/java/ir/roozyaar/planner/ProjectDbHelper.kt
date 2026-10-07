package ir.roozyaar.planner

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ProjectDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE projects (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                category TEXT NOT NULL DEFAULT 'ساخت‌وساز',
                status TEXT NOT NULL DEFAULT 'فعال',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("""
            CREATE TABLE project_photos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id INTEGER NOT NULL,
                uri TEXT NOT NULL,
                caption TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_project_photos_project ON project_photos(project_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun allProjects(): List<ProjectItem> {
        val out = mutableListOf<ProjectItem>()
        readableDatabase.query("projects", null, null, null, null, null, "updated_at DESC").use { c ->
            while (c.moveToNext()) {
                out += ProjectItem(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    name = c.getString(c.getColumnIndexOrThrow("name")),
                    description = c.getString(c.getColumnIndexOrThrow("description")),
                    category = c.getString(c.getColumnIndexOrThrow("category")),
                    status = c.getString(c.getColumnIndexOrThrow("status")),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("created_at")),
                    updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"))
                )
            }
        }
        return out
    }

    fun insertProject(p: ProjectItem): Long {
        val cv = ContentValues().apply {
            put("name", p.name)
            put("description", p.description)
            put("category", p.category)
            put("status", p.status)
            put("created_at", p.createdAt)
            put("updated_at", p.updatedAt)
        }
        return writableDatabase.insertOrThrow("projects", null, cv)
    }

    fun updateProject(p: ProjectItem) {
        val cv = ContentValues().apply {
            put("name", p.name)
            put("description", p.description)
            put("category", p.category)
            put("status", p.status)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.update("projects", cv, "id=?", arrayOf(p.id.toString()))
    }

    fun deleteProject(id: Long) {
        writableDatabase.delete("project_photos", "project_id=?", arrayOf(id.toString()))
        writableDatabase.delete("projects", "id=?", arrayOf(id.toString()))
    }

    fun photos(projectId: Long): List<ProjectPhoto> {
        val out = mutableListOf<ProjectPhoto>()
        readableDatabase.query(
            "project_photos", null, "project_id=?", arrayOf(projectId.toString()),
            null, null, "created_at DESC"
        ).use { c ->
            while (c.moveToNext()) {
                out += ProjectPhoto(
                    id = c.getLong(c.getColumnIndexOrThrow("id")),
                    projectId = c.getLong(c.getColumnIndexOrThrow("project_id")),
                    uri = c.getString(c.getColumnIndexOrThrow("uri")),
                    caption = c.getString(c.getColumnIndexOrThrow("caption")),
                    createdAt = c.getLong(c.getColumnIndexOrThrow("created_at"))
                )
            }
        }
        return out
    }

    fun addPhoto(photo: ProjectPhoto) {
        val cv = ContentValues().apply {
            put("project_id", photo.projectId)
            put("uri", photo.uri)
            put("caption", photo.caption)
            put("created_at", photo.createdAt)
        }
        writableDatabase.insertOrThrow("project_photos", null, cv)
    }

    fun deletePhoto(id: Long) {
        writableDatabase.delete("project_photos", "id=?", arrayOf(id.toString()))
    }

    companion object {
        private const val DB_NAME = "myplanner_projects.db"
        private const val DB_VERSION = 1
    }
}
