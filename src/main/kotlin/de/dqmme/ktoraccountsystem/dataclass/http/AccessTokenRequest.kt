package de.dqmme.ktoraccountsystem.dataclass.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerateAccessTokenRequest(
    @SerialName("grant_type") val grantType: String,
    val code: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String,
    val state: String? = null
)