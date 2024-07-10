package de.dqmme.ktoraccountsystem.dataclass

import io.ktor.server.auth.Principal
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatabaseToken(
    @SerialName("_id") val hashedRefreshToken: String,
    @SerialName("user_id") val userId: String,
    @SerialName("expires_at") val expiresAt: Instant
) : Principal
