package dev.crashteam.uzumspace.client.uzum.model.web

data class UzumGQLQuery<T>(
    val operationName: String,
    val query: String,
    val variables: T
)

data class CategoryGQLQueryVariables(
    val queryInput: CategoryGQLQueryInput
)

data class CategoryGQLQueryInput(
    val categoryId: String,
    val pagination: CategoryGQLQueryInputPagination,
    val showAdultContent: String,
    val filters: List<Any> = emptyList(),
    val sort: String,
)

data class CategoryGQLQueryInputPagination(
    val offset: Long,
    val limit: Int
)
