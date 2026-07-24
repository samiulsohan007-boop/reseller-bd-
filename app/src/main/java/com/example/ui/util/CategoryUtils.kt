package com.example.ui.util

data class SubCategoryItem(
    val name: String,
    val iconUrl: String = ""
)

fun parseSubcategories(subcategoriesStr: String): List<SubCategoryItem> {
    if (subcategoriesStr.isBlank()) return emptyList()
    return subcategoriesStr.split(",").map { raw ->
        val trimmed = raw.trim()
        if (trimmed.contains("|")) {
            val parts = trimmed.split("|")
            SubCategoryItem(
                name = parts[0].trim(),
                iconUrl = parts.getOrNull(1)?.trim() ?: ""
            )
        } else {
            SubCategoryItem(name = trimmed, iconUrl = "")
        }
    }.filter { it.name.isNotEmpty() }
}

fun formatSubcategories(list: List<SubCategoryItem>): String {
    return list.joinToString(", ") { item ->
        val cleanName = item.name.trim().replace("|", "-")
        val cleanUrl = item.iconUrl.trim()
        if (cleanUrl.isNotBlank()) "$cleanName|$cleanUrl"
        else cleanName
    }
}
