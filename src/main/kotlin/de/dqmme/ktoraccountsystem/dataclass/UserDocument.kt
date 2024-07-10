package de.dqmme.ktoraccountsystem.dataclass

import io.ktor.server.auth.Principal

interface UserDocument : Principal {
    val userId: Int
    val username: String
    val hashedPassword: String
}