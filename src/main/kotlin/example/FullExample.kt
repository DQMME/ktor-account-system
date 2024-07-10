package example

import de.dqmme.ktoraccountsystem.KtorAccountSystem
import de.dqmme.ktoraccountsystem.dataclass.JWTData
import de.dqmme.ktoraccountsystem.dataclass.UserDocument
import de.dqmme.ktoraccountsystem.dataclass.cookie.LoginCookie
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseClient
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseCode
import de.dqmme.ktoraccountsystem.dataclass.database.DatabaseToken
import de.dqmme.ktoraccountsystem.dataclass.http.AccessTokenRequest
import de.dqmme.ktoraccountsystem.util.Bcrypt
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

@Serializable
data class ExampleUser(
    @SerialName("_id") override val userId: Int,
    override val username: String,
    @SerialName("hashed_password") override val hashedPassword: String
) : UserDocument

fun main() {
    //Create a new KtorAccountSystem instance with given JWT data (store this in a config!)
    val accountSystem = KtorAccountSystem<ExampleUser>(
        JWTData("your-secret", "http://localhost:8080", "http://localhost:8080/hello", "Access")
    )

    //Connect to MongoDB and get all collections
    val mongoClient = KMongo.createClient().coroutine
    val database = mongoClient.getDatabase("account-system-test")
    val userCollection = database.getCollection<ExampleUser>("users")
    val tokenCollection = database.getCollection<DatabaseToken>("refresh-tokens")
    val clientCollection = database.getCollection<DatabaseClient>("api-clients")
    val codeCollection = database.getCollection<DatabaseCode>("api-codes")

    //Pass the collections to the account system
    accountSystem.initializeDatabase(userCollection, tokenCollection, clientCollection, codeCollection)

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        //Pass the application instance
        accountSystem.application = this

        //Register and configure the ktor plugins
        accountSystem.registerCookie()
        accountSystem.registerAuthentication(useSession = true, useBearer = true)

        routing {
            get("/login") {
                //Very simple login logic, don't do it like this
                val username = call.parameters["username"]
                val password = call.parameters["password"]

                if (username == null || password == null) return@get

                //Verify the credentials
                val user = accountSystem.verifyLogin(username, password) ?: return@get
                //Generate access and refresh tokens
                val tokenPair = accountSystem.createTokenPair(user)

                //Store the info in a session cookie
                call.sessions.set(LoginCookie(user.userId, tokenPair.accessToken, tokenPair.refreshToken))
                //Redirect user to the protected route
                call.respondRedirect("/hello")
            }

            get("/register") {
                //Very simple register logic, don't do it like this
                val username = call.parameters["username"]
                val password = call.parameters["password"]

                if (username == null || password == null) return@get

                //Check if username is already used
                if (accountSystem.database.getUserByUsername(username) != null) return@get

                //Create user object
                val user = ExampleUser(
                    accountSystem.database.newUserId(),
                    username,
                    Bcrypt.createHash(password)
                )

                //Store user in the database
                accountSystem.database.saveUser(user)
                //Redirect user to the login page
                call.respondRedirect("/login")
            }

            //This endpoint would need to be called in the background from the frontend everytime the access token expires
            get("/refresh") {
                val session = call.sessions.get<LoginCookie>() ?: return@get

                val tokenPair = accountSystem.refreshToken(session.userId, session.refreshToken) ?: return@get

                call.sessions.set(LoginCookie(session.userId, tokenPair.accessToken, tokenPair.refreshToken))
                call.respond(HttpStatusCode.OK, "OK")
            }

            route("/api") {
                authenticate("ac-bearer") {
                    post("/create-client") {
                        val user = call.principal<ExampleUser>()!!

                        //Create a client
                        val client = accountSystem.createOAuthClient(user)

                        //Returns all information including the client secret (no option to retrieve it after)
                        call.respond(client)
                    }
                }

                authenticate("ac-sessions") {
                    //Does not prompt the user to do anything, just for showcase, implement your own logic
                    get("/authorize") {
                        //Get all the required info
                        val clientId = call.parameters["client_id"]
                        val responseType = call.parameters["response_type"]?.lowercase()
                        val redirectUri = call.parameters["redirect_uri"]?.lowercase()
                        val state = call.parameters["state"]
                        val scope = call.parameters["scope"]
                        val scopeList = scope?.split(" ") ?: emptyList()

                        if (clientId == null || responseType == null || redirectUri == null || responseType != "code") return@get

                        //Retrieve client from database
                        val client = accountSystem.database.getOAuthClientById(clientId) ?: return@get

                        //Check if the provided redirect uri is correct
                        if (!client.redirectUris.contains(redirectUri)) return@get

                        //Generate a new code with given state (state can be null)
                        val code = accountSystem.newClientCode(call.principal()!!, clientId, state, scopeList)

                        //Redirect the user
                        call.respondRedirect("$redirectUri?code=$code&state=$state" + if (scope != null) "&scope=$scope" else "")
                    }
                }

                post("/token") {
                    val body = call.receive<AccessTokenRequest>()

                    when (body.grantType) {
                        //Check if the token is generated or refreshed
                        "authorization_code" -> {
                            if (body.code == null || body.clientSecret == null) return@post

                            //Verify the given code
                            val databaseCode = accountSystem
                                .verifyCode(body.clientId, body.clientSecret, body.code, body.state) ?: return@post

                            //Delete the code as it's used now
                            accountSystem.database.deleteCode(databaseCode)

                            //Retrieve the user from database
                            val user = accountSystem.database.getUserById(databaseCode.userId) ?: return@post

                            //Generate refresh and access tokens with scope and client id
                            val tokenPair = accountSystem.createTokenPair(
                                user = user,
                                clientId = databaseCode.clientId,
                                scope = databaseCode.scope,
                            )

                            call.respond(tokenPair)
                        }

                        "refresh_token" -> {
                            if (body.refreshToken == null) return@post

                            //Generate a new token pair
                            val tokenPair = accountSystem.refreshToken(body.clientId, body.refreshToken) ?: return@post

                            call.respond(tokenPair)
                        }
                    }
                }
            }

            //Example route to authenticate
            authenticate("ac-sessions") {
                get("/hello") {
                    val user = call.principal<ExampleUser>()!!

                    call.respondText(user.username)
                }
            }
        }
    }.start(wait = true)
}
