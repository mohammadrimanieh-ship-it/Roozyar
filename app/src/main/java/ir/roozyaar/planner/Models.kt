package ir.roozyaar.planner

enum class TaskCategory(val label: String, val emoji: String) {
    STOCK("بورس", "📈"),
    PROJECTS("ساخت‌وساز و پروژه‌ها", "🏗️"),
    HOME("خانه و خرید", "🏠"),
    SPOUSE("همسر", "❤️"),
    PEOPLE("افراد", "👥"),
    PERSONAL("شخصی", "👤"),
    LATER("بعداً / ایده‌ها", "💡")
}

enum class TaskPriority(val label: String) {
    NORMAL("عادی"),
    HIGH("مهم"),
    URGENT("فوری")
}

enum class TaskStatus(val label: String) {
    ACTIVE("فعال"),
    WAITING("منتظر دیگران"),
    DONE("انجام‌شده")
}

data class TaskItem(
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val category: TaskCategory = TaskCategory.PERSONAL,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val status: TaskStatus = TaskStatus.ACTIVE,
    val contextName: String = "",
    val dueAt: Long? = null,
    val reminderAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class TaskDraft(
    val id: Long? = null,
    val title: String,
    val notes: String = "",
    val category: TaskCategory? = null,
    val priority: TaskPriority? = null,
    val waiting: Boolean = false,
    val contextName: String = "",
    val dueAt: Long? = null,
    val reminderEnabled: Boolean = true
)
