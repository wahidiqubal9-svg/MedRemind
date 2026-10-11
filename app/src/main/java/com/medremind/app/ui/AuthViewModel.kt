package com.medremind.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.medremind.app.data.auth.AuthUser
import com.medremind.app.data.auth.FirebaseAuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Shared, app-wide view of the signed-in account. */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FirebaseAuthRepository()

    val user: StateFlow<AuthUser?> = repo.authState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.currentUser)

    val isSignedIn: Boolean get() = user.value != null

    fun signOut() {
        repo.signOut()
        // Also clear the Google client's cached account so the next Google sign-in
        // shows the account chooser again.
        runCatching {
            GoogleSignIn.getClient(
                getApplication(),
                GoogleSignInOptions.DEFAULT_SIGN_IN
            ).signOut()
        }
    }
}
