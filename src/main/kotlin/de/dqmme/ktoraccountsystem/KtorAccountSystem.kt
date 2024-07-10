package de.dqmme.ktoraccountsystem

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import de.dqmme.ktoraccountsystem.dataclass.ApiClient
import de.dqmme.ktoraccountsystem.dataclass.ApiCode
import de.dqmme.ktoraccountsystem.dataclass.JWTData
import de.dqmme.ktoraccountsystem.dataclass.TokenPair
import de.dqmme.ktoraccountsystem.dataclass.UserDocument
import de.dqmme.ktoraccountsystem.dataclass.cookie.LoginCookie
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseClient
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseCode
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseToken
import de.dqmme.ktoraccountsystem.util.Bcrypt
import de.dqmme.ktoraccountsystem.util.Database
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.session
import io.ktor.server.request.header
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.maxAge
import io.ktor.server.sessions.sessions
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.litote.kmongo.coroutine.CoroutineCollection
import java.security.SecureRandom
import java.util.Date
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.toJavaDuration
import java.time.Instant as JInstant

class KtorAccountSystem<T : UserDocument>(
    private val jwtData: JWTData,
    private val accessTokenExpiration: Duration = 1.hours,
    private val refreshTokenExpiration: Duration = 30.days
) {
    lateinit var application: Application

    lateinit var database: Database<T>

    private val jwtVerifier: JWTVerifier = JWT
        .require(Algorithm.HMAC256(jwtData.secret))
        .withAudience(jwtData.audience)
        .withIssuer(jwtData.issuer)
        .build()

    fun initializeDatabase(
        userCollection: CoroutineCollection<T>? = null,
        refreshTokenCollection: CoroutineCollection<DatabaseToken>? = null,
        apiClientCollection: CoroutineCollection<DatabaseClient>? = null,
        apiCodeCollection: CoroutineCollection<DatabaseCode>? = null
    ) {
        database = Database(userCollection, refreshTokenCollection, apiClientCollection, apiCodeCollection)
    }

    fun registerCookie(maxAge: Duration = refreshTokenExpiration) {
        application.install(Sessions) {
            cookie<LoginCookie>("as-login") {
                cookie.maxAge = maxAge
            }
        }
    }

    fun registerAuthentication(
        useSession: Boolean = false,
        useBearer: Boolean = false,
        sessionChallenge: (suspend (ApplicationCall) -> Unit?)? = null
    ) {
        application.install(Authentication) {
            if (useSession) {
                session<LoginCookie>("ac-session") {
                    validate {
                        getUserByToken(it.accessToken)
                    }

                    if (sessionChallenge != null) {
                        challenge {
                            sessionChallenge(call)
                        }
                    }
                }
            }

            if (useBearer) {
                bearer("ac-bearer") {
                    realm = "Account Token"

                    authHeader {
                        val accessTokenHeader = it.request.header(HttpHeaders.Authorization)
                        val accessTokenCookie = it.sessions.get<LoginCookie>()?.accessToken

                        if (accessTokenHeader != null) return@authHeader parseAuthorizationHeader(accessTokenHeader)

                        if (accessTokenCookie != null) return@authHeader parseAuthorizationHeader("Bearer $accessTokenCookie")

                        return@authHeader null
                    }

                    authenticate {
                        getUserByToken(it.token)
                    }
                }
            }
        }
    }

    suspend fun createTokenPair(
        user: T,
        accessTokenExpires: JInstant = Date().toInstant().plus(accessTokenExpiration.toJavaDuration()),
        refreshTokenExpires: Instant = Clock.System.now() + refreshTokenExpiration,
        clientId: String? = null,
        scope: List<String>? = null
    ): TokenPair {
        val accessToken = if (scope != null) {
            JWT.create()
                .withAudience(jwtData.audience)
                .withIssuer(jwtData.issuer)
                .withIssuedAt(Date())
                .withClaim("user-id", user.userId)
                .withClaim("scope", scope.joinToString(" "))
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(accessTokenExpires)
                .sign(Algorithm.HMAC256(jwtData.secret))
        } else {
            JWT.create()
                .withAudience(jwtData.audience)
                .withIssuer(jwtData.issuer)
                .withIssuedAt(Date())
                .withClaim("user-id", user.userId)
                .withJWTId(UUID.randomUUID().toString())
                .withExpiresAt(accessTokenExpires)
                .sign(Algorithm.HMAC256(jwtData.secret))
        }

        val refreshToken = newSecret()

        if (clientId != null) {
            if (database.findClientTokens(clientId)
                    .firstOrNull { Bcrypt.verifyHash(refreshToken, it.hashedRefreshToken) } != null
            ) return createTokenPair(user, accessTokenExpires, refreshTokenExpires, clientId, scope)
        }

        if (database.findUserTokens(user.userId)
                .firstOrNull { Bcrypt.verifyHash(refreshToken, it.hashedRefreshToken) } != null
        ) return createTokenPair(user, accessTokenExpires, refreshTokenExpires, clientId, scope)

        database.saveToken(
            DatabaseToken(
                user.userId,
                Bcrypt.createHash(refreshToken),
                refreshTokenExpires,
                clientId,
                scope
            )
        )

        return TokenPair(accessToken, refreshToken)
    }

    suspend fun refreshToken(
        userId: Int,
        refreshToken: String,
        expiresAt: JInstant = Date().toInstant().plus(accessTokenExpiration.toJavaDuration())
    ): TokenPair? {
        val user = database.getUserById(userId) ?: return null

        val token = database.findUserTokens(userId)
            .firstOrNull { Bcrypt.verifyHash(refreshToken, it.hashedRefreshToken) } ?: return null

        database.deleteToken(token.hashedRefreshToken)

        if (token.expiresAt <= Clock.System.now()) return null

        return createTokenPair(user, expiresAt)
    }

    suspend fun refreshToken(
        clientId: String,
        refreshToken: String,
        expiresAt: JInstant = Date().toInstant().plus(accessTokenExpiration.toJavaDuration())
    ): TokenPair? {
        val token = database.findClientTokens(clientId)
            .firstOrNull { Bcrypt.verifyHash(refreshToken, it.hashedRefreshToken) } ?: return null

        val user = database.getUserById(token.userId) ?: return null

        database.deleteToken(token.hashedRefreshToken)

        if (token.expiresAt <= Clock.System.now()) return null

        return createTokenPair(user, expiresAt)
    }

    suspend fun verifyLogin(username: String, password: String): T? {
        val user = database.getUserByUsername(username) ?: return null

        if (!Bcrypt.verifyHash(password, user.hashedPassword)) return null

        return user
    }

    private suspend fun getUserByToken(token: String): T? {
        try {
            val userId = jwtVerifier.verify(token).getClaim("user-id").asInt()

            return database.getUserById(userId)
        } catch (_: Exception) {
            return null
        }
    }

    suspend fun createOAuthClient(user: T): ApiClient {
        val client = ApiClient(
            database.newClientId(),
            user.userId,
            newSecret(),
            emptyList()
        )

        database.saveOAuthClient(
            DatabaseClient(
                client.clientId,
                user.userId,
                Bcrypt.createHash(client.clientSecret),
                client.redirectUris
            )
        )

        return client
    }

    suspend fun newClientCode(
        user: T,
        clientId: String,
        state: String? = null,
        scope: List<String> = emptyList()
    ): ApiCode {
        val code = ApiCode(
            newSecret(),
            clientId,
            user.userId,
            state,
            scope
        )

        database.saveOAuthCode(
            DatabaseCode(
                Bcrypt.createHash(code.code),
                code.clientId,
                user.userId,
                state,
                code.scope
            )
        )

        return code
    }

    suspend fun verifyCode(clientId: String, clientSecret: String, code: String, state: String? = null): DatabaseCode? {
        val databaseCode =
            database.findClientCodes(clientId).firstOrNull { Bcrypt.verifyHash(code, it.hashedCode) } ?: return null

        val client = database.getOAuthClientById(clientId) ?: return null

        if (Bcrypt.verifyHash(client.hashedClientSecret, clientSecret)) return null

        if (databaseCode.state != state) return null

        return databaseCode
    }

    private fun newSecret(): String {
        val charPool = ('a'..'z') + ('A'..'Z')
        return (1..64)
            .map { SecureRandom().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}