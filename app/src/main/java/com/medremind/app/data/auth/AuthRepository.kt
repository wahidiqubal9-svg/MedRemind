package com.medremind.app.data.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Outcome of an auth action, with a human-readable error message. */
sealed interface AuthResult {
    data class Success(val email: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

/**
 * Email/password authentication. Firebase is initialised from google-services.json;
 * if that config is missing the calls return a clear [AuthResult.Error] instead of
 * crashing.
 */
interface AuthRepository {
    val currentEmail: String?
    val isSignedIn: Boolean
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun sendPasswordReset(email: String): AuthResult
    fun signOut()
}

class FirebaseAuthRepository : AuthRepository {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull()

    override val currentEmail: String? get() = auth?.currentUser?.email
    override val isSignedIn: Boolean get() = auth?.currentUser != null

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

    override fun signOut() {
        runCatching { auth?.signOut() }
    }

    /** Best-effort: keep a lightweight user document in Firestore. */
    private fun ensureProfile(user: FirebaseUser) {
        val db = firestore ?: return
        runCatching {
            val doc = db.collection("users").document(user.uid)
            doc.set(
                mapOf(
                    "email" to (user.email ?: ""),
                    "role" to "caregiver",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }
    }
}

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
    else -> message!!
}
