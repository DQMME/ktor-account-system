package de.dqmme.ktoraccountsystem.dataclass.cookie

data class LoginCookie(
    val userId: Int,
    val accessToken: String,
    val refreshToken: String
)
