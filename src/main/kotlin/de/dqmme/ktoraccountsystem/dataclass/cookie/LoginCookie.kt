package de.dqmme.ktoraccountsystem.dataclass

data class LoginCookie(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)
