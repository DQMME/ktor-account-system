package de.dqmme.ktoraccountsystem.dataclass.database

import io.ktor.server.auth.Principal
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseToken(
    @SerialName("user_id") val userId: Int,
    @SerialName("_id") val hashedRefreshToken: String,
    @SerialName("expires_at") val expiresAt: Instant,
    @SerialName("client_id") val clientId: String? = null,
    val scope: List<String>? = null
) : Principal
