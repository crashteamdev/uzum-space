package dev.crashteam.uzumspace.client.uzum.model.lk

data class AccountProductInfo(
    val category: AccountProductCategory,
    val title: AccountTitleInfo,
    val skuTitle: String
)

data class AccountProductCategory(
    val id: Long,
    val title: String
)

data class AccountTitleInfo(
    val ru: String,
    val uz: String,
)
