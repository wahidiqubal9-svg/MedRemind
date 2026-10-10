package com.medremind.app.data.auth

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/** The signed-in user, or null. */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

/** Outcome of an auth action, with a human-readable error message. */
sealed interface AuthResult {
    data class Success(val email: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

/**
 * Email/password + Google authentication. Firebase is initialised from
 * google-services.json; if that config is missing the calls return a clear
 * [AuthResult.Error] instead of crashing.
 */
interface AuthRepository {
    val currentUser: AuthUser?
    fun authState(): Flow<AuthUser?>
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun sendPasswordReset(email: String): AuthResult
    suspend fun signInWithGoogle(): AuthResult
    fun signOut()
}

class FirebaseAuthRepository(private val context: Context) : AuthRepository {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    override val currentUser: AuthUser? get() = auth?.currentUser?.toAuthUser()

    override fun authState(): Flow<AuthUser?> = callbackFlow {
        val a = auth
        if (a == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        a.addAuthStateListener(listener)
        awaitClose { a.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): AuthResult {
        val a = auth ?: return AuthResult.Error(NOT_CONFIGURED)
        return runCatching { a.signInWithEmailAndPassword(email.trim(), password).await() }
            .fold(
                onSuccess = { AuthResult.Success(it.user?.email ?: email.trim()) },
                onFailure = { AuthResult.Error(it.friendly()) }
            )
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        val a = auth ?: return AuthResult.Error(NOT_CONFIGURED)
        return runCatching { a.createUserWithEmailAndPassword(email.trim(), password).await() }
            .fold(
                onSuccess = { result ->
                    result.user?.let { ensureProfile(it) }
                    AuthResult.Success(result.user?.email ?: email.trim())
                },
                onFailure = { AuthResult.Error(it.friendly()) }
            )
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        val a = auth ?: return AuthResult.Error(NOT_CONFIGURED)
        return runCatching { a.sendPasswordResetEmail(email.trim()).await() }
            .fold(
                onSuccess = { AuthResult.Success(email.trim()) },
                onFailure = { AuthResult.Error(it.friendly()) }
            )
    }

    /**
     * Shows the device's Google account picker (Credential Manager) and signs in
     * to Firebase with the chosen account. Needs the Google provider enabled in
     * Firebase and a web client id in google-services.json.
     */
    override suspend fun signInWithGoogle(): AuthResult {
        val a = auth ?: return AuthResult.Error(NOT_CONFIGURED)
        val serverClientId = context.resources.getIdentifier(
            "default_web_client_id", "string", context.packageName
        ).takeIf { it != 0 }?.let { context.getString(it) }
        if (serverClientId.isNullOrBlank()) {
            return AuthResult.Error(
                "Google sign-in isn't set up yet. Enable Google in Firebase and " +
                    "re-download google-services.json."
            )
        }
        return try {
            val credentialManager = androidx.credentials.CredentialManager.create(context)
            val option = com.google.android.libraries.identity.googleid.GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = androidx.credentials.GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            val googleId = com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
                .createFrom(credential.data)
            val firebaseCred = GoogleAuthProvider.getCredential(googleId.idToken, null)
            val result = a.signInWithCredential(firebaseCred).await()
            result.user?.let { ensureProfile(it) }
            AuthResult.Success(result.user?.email ?: googleId.id)
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            AuthResult.Error("Sign-in cancelled.")
        } catch (e: androidx.credentials.exceptions.NoCredentialException) {
            AuthResult.Error("No Google account found on this device.")
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            AuthResult.Error(e.friendly())
        } catch (e: Exception) {
            AuthResult.Error(e.friendly())
        }
    }

    override fun signOut() {
        runCatching { auth?.signOut() }
    }

    /** Best-effort: keep a lightweight user document in Firestore. */
    private fun ensureProfile(user: FirebaseUser) {
        val db = firestore ?: return
        runCatching {
            db.collection("users").document(user.uid).set(
                mapOf(
                    "email" to (user.email ?: ""),
                    "name" to (user.displayName ?: ""),
                    "role" to "caregiver",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString()
)

private const val NOT_CONFIGURED =
    "Cloud isn't configured. Add google-services.json and rebuild."

/** Firebase Task -> coroutine. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) cont.resume(task.result)
        else cont.resumeWithException(task.exception ?: RuntimeException("Firebase error"))
    }
}

private fun Throwable.friendly(): String = when {
    message == null -> "Something went wrong. Please try again."
    message!!.contains("password is invalid", true) -> "Incorrect email or password."
    message!!.contains("no user record", true) -> "No account found for that email."
    message!!.contains("email address is already in use", true) ->
        "That email is already registered. Try signing in."
    message!!.contains("network", true) -> "No connection. Check your internet and retry."
    message!!.contains("badly formatted", true) -> "Please enter a valid email address."
    message!!.contains("at least 6 characters", true) ->
        "Password must be at least 6 characters."
    message!!.contains("CONFIGURATION_NOT_FOUND", true) ->
        "Sign-in method not enabled. In Firebase: Authentication \u2192 Sign-in " +
            "method \u2192 enable Email/Password."
    else -> message!!
}
