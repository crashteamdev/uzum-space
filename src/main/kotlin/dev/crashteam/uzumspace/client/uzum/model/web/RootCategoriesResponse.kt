package dev.crashteam.uzumspace.client.uzum.model.web

data class RootCategoriesResponse(
    val payload: List<SimpleCategory>
)

data class SimpleCategory(
    val id: Long,
    val productAmount: Long,
    val adult: Boolean,
    val eco: Boolean,
    val title: String,
    val path: List<String>?,
    val children: List<SimpleCategory>
)
