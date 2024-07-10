package de.dqmme.ktoraccountsystem.util

import at.favre.lib.crypto.bcrypt.BCrypt

object Bcrypt {
    fun createHash(toHash: String): String {
        return BCrypt.withDefaults().hashToString(12, toHash.toCharArray())
    }

    fun verifyHash(toVerify: String, hashed: String): Boolean {
        return BCrypt.verifyer().verify(toVerify.toCharArray(), hashed).verified
    }
}