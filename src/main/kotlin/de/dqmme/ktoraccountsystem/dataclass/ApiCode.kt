package de.dqmme.ktoraccountsystem.dataclass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiCode(
    val code: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("user_id") val userId: Int,
    val state: String? = null,
    val scope: List<String>
)
