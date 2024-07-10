package de.dqmme.ktoraccountsystem.dataclass.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenRequest(
    @SerialName("grant_type") val grantType: String,
    @SerialName("client_id") val clientId: String,
    val code: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("client_secret") val clientSecret: String? = null,
    val state: String? = null
)