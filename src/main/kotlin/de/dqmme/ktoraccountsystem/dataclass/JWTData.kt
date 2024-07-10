package de.dqmme.ktoraccountsystem.dataclass

data class JWTData(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)
