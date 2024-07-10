# Ktor Account System

This is a small account system for ktor which does simple account handling logic for you

* **User Authentication:** JWT-based authentication using sessions or bearer tokens.
* **Database Integration:** Use your own database and just provide the needed collections
* **OAuth2 Support:** OAuth authorization code flow to allow access to third party apps
* **Password Management:** Secure password hashing and verification using Bcrypt

## Usage

1. **Installation:**

   Add this module as a dependency to your Ktor project. (Jitpack following)

2. **Configuration:**
      
    * Create a data class that inherits the `UserDocument` interface
    * Create a `JWTData` object containing your JWT secret, issuer, and audience.
    * Initialize the `KtorAccountSystem` class with the `JWTData` object.
    * Configure your database collections (users, refresh tokens, clients, codes) by calling `initializeDatabase`.
    * Provide the application instance to the account system
    * Register cookie sessions using `registerCookie` if you want to use session-based authentication.
    * Set up authentication by calling `registerAuthentication`.  You can choose between:
        * `useSession = true`: Session-based authentication
        * `useBearer = true`: Bearer token authentication
        * Provide an optional `sessionChallenge` function to handle challenges during session authentication.
        * The bearer authentication uses the session as fallback so you can use the session authentication for frontend with a handler and the bearer authentication for api requests (it will just return 401, the bearer authentication has no other handlers)

```kotlin
//Create data class
@Serializable
data class ExampleUser(
   @SerialName("_id") override val userId: Int,
   override val username: String,
   @SerialName("hashed_password") override val hashedPassword: String
) : UserDocument

//Define jwt data 
val jwtData = JWTData("your-secret", "your-issuer", "your-audience", "your-realm")
//Create account system
val accountSystem = AccountSystem<ExampleUser>(jwtData)

accountSystem.initializeDatabase(your collections)

//Your ktor server
embeddedServer(...) {
    //Provide the application instance 
    accountSystem.application = this
   
    accountSystem.registerCookie()
    accountSystem.registerAuthentication(
        useSession = true,
        useBearer = true
    ) {
        //Is executed when Session authentication fails
    }
}
```

3. **API Endpoints:**

    * Create Ktor routes to handle:
        * User registration
        * User login
        * Token refresh
        * OAuth2 authorization and token exchange
        * Any other account-related actions
4. **Example:**
    There is a full example with very basic implementation located at `src/main/kotlin/example/FullExample.kt`

## Credits
Thanks to those people/projects for making this library possible:
- [Ktor](https://github.com/ktorio/ktor)
- [KMongo](https://github.com/Litote/kmongo)
- [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime)
- [Bcrypt - patrickfav](https://github.com/patrickfav/bcrypt)

## Todo
- Publish to jitpack (or mavencentral but ig that's not coming)
- Documentation
- Support third party OAuth
- Implement OAuth scopes properly