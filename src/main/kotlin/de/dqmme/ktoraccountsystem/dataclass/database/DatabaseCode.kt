package de.dqmme.ktoraccountsystem.dataclass.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseCode(
    @SerialName("_id") val hashedCode: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("user_id") val userId: Int,
    val state: String? = null,
    val scope: List<String>
)
