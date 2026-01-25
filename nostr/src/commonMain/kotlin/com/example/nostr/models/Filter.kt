package com.example.nostr.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Filter(
    val ids: List<String>? = null,
    val authors: List<String>? = null,
    val kinds: List<Int>? = null,
    @SerialName("#d")
    val d: List<String>? = null,
    @SerialName("#t")
    val t: List<String>? = null,
    @SerialName("#a")
    val a: List<String>? = null,
    @SerialName("#e")
    val e: List<String>? = null,
    @SerialName("#p")
    val p: List<String>? = null,
    @SerialName("#k")
    val k: List<String>? = null,
    @SerialName("#subject")
    val subject: List<String>? = null,
    val limit: Int? = null,
    val since: Long? = null,
    val until: Long? = null
)
