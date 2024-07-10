package de.dqmme.ktoraccountsystem.dataclass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiClient(
    @SerialName("client_id") val clientId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("redirect_uris") val redirectUris: List<String>
)
