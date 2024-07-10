package de.dqmme.ktoraccountsystem.dataclass.database

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseClient(
    @SerialName("_id") val clientId: String,
    @SerialName("user_id") val userId: Int,
    @SerialName("hashed_client_secret") val hashedClientSecret: String,
    @SerialName("redirect_uris") val redirectUris: List<String>
)