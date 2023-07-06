package dev.crashteam.uzumspace.client.uzum.model.lk

data class AccountProductInfo(
    val category: AccountProductCategory,
    val title: String,
    val skuTitle: String
)

data class AccountProductCategory(
    val id: Long,
    val title: String
)
