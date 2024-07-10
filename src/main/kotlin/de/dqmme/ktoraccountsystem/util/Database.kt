package de.dqmme.ktoraccountsystem.util

import de.dqmme.ktoraccountsystem.dataclass.UserDocument
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseClient
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseCode
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseToken
import de.dqmme.ktoraccountsystem.dataclass.exception.DatabaseException
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import java.util.UUID
import kotlin.random.Random

class Database<T : UserDocument>(
    private val userCollection: CoroutineCollection<T>?,
    private val refreshTokenCollection: CoroutineCollection<DatabaseToken>?,
    private val apiClientCollection: CoroutineCollection<DatabaseClient>?,
    private val apiCodeCollection: CoroutineCollection<DatabaseCode>?
) {
    //User

    suspend fun saveUser(user: T) {
        if (userCollection == null) throw DatabaseException("User Collection is not provided.")

        userCollection.save(user)
    }

    suspend fun getUserById(userId: Int): T? {
        if (userCollection == null) throw DatabaseException("User Collection is not provided.")

        return userCollection.findOne("{ _id: \"$userId\" }")
    }

    suspend fun getUserByUsername(username: String): T? {
        if (userCollection == null) throw DatabaseException("User Collection is not provided.")

        return userCollection.findOne("{ username: \"$username\" }")
    }

    suspend fun newUserId(): Int {
        val userId = Random.nextInt(11111111, 99999999)

        if (getUserById(userId) != null) return newUserId()

        return userId
    }

    //Token

    suspend fun saveToken(databaseToken: DatabaseToken) {
        if (refreshTokenCollection == null) throw DatabaseException("Refresh Token Collection is not provided.")

        refreshTokenCollection.save(databaseToken)
    }

    suspend fun findUserTokens(userId: Int): List<DatabaseToken> {
        if (refreshTokenCollection == null) throw DatabaseException("Refresh Token Collection is not provided.")

        return refreshTokenCollection.find(DatabaseToken::userId eq userId).toList()
    }

    suspend fun findClientTokens(clientId: String): List<DatabaseToken> {
        if (refreshTokenCollection == null) throw DatabaseException("Refresh Token Collection is not provided.")

        return refreshTokenCollection.find(DatabaseToken::clientId eq clientId).toList()
    }

    suspend fun deleteToken(hashedToken: String) {
        if (refreshTokenCollection == null) throw DatabaseException("Refresh Token Collection is not provided.")

        refreshTokenCollection.deleteOne(DatabaseToken::hashedRefreshToken eq hashedToken)
    }

    //Clients

    suspend fun saveOAuthClient(databaseClient: DatabaseClient) {
        if (apiClientCollection == null) throw DatabaseException("API Client Collection is not provided.")

        apiClientCollection.save(databaseClient)
    }

    suspend fun getOAuthClientById(clientId: String): DatabaseClient? {
        if (apiClientCollection == null) throw DatabaseException("API Client Collection is not provided.")

        return apiClientCollection.findOne("{ _id: \"$clientId\" }")
    }

    suspend fun newClientId(): String {
        val clientId = UUID.randomUUID().toString().replace("-", "")

        if (getOAuthClientById(clientId) != null) return newClientId()

        return clientId
    }

    //Codes

    suspend fun saveOAuthCode(databaseCode: DatabaseCode) {
        if (apiCodeCollection == null) throw DatabaseException("API Code Collection is not provided.")

        apiCodeCollection.save(databaseCode)
    }

    suspend fun findClientCodes(clientId: String): List<DatabaseCode> {
        if (apiCodeCollection == null) throw DatabaseException("API Code Collection is not provided.")

        return apiCodeCollection.find(DatabaseCode::clientId eq clientId).toList()
    }

    suspend fun deleteCode(databaseCode: DatabaseCode) {
        if (apiCodeCollection == null) throw DatabaseException("API Code Collection is not provided.")

        apiCodeCollection.deleteOne(DatabaseCode::hashedCode eq databaseCode.hashedCode)
    }
}