package ir.roozyaar.planner

data class ProjectItem(
    val id: Long = 0L,
    val name: String,
    val description: String = "",
    val category: String = "ساخت‌وساز",
    val status: String = "فعال",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProjectPhoto(
    val id: Long = 0L,
    val projectId: Long,
    val uri: String,
    val caption: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
