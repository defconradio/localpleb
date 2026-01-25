package com.example.pleb2.util

fun isValidImageUrl(url: String): Boolean {
    val regex = Regex("^https?://.*\\.(jpg|jpeg|png|gif|webp|bmp|svg)", RegexOption.IGNORE_CASE)
    return url.isNotBlank() && regex.matches(url.trim())
}

