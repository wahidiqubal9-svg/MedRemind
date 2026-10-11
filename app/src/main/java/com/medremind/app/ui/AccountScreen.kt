package com.medremind.app.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.medremind.app.data.auth.AuthResult
import com.medremind.app.data.auth.FirebaseAuthRepository
import kotlinx.coroutines.launch

/**
 * Email/password + Google sign-in backed by Firebase Auth. Signing in is optional:
 * the app keeps working offline, and this only adds cloud backup/sync.
 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onSignedIn: () -> Unit = {},
    settings: SettingsViewModel? = null
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val repo = remember { FirebaseAuthRepository() }
    val scope = rememberCoroutineScope()
    val googleAvailable = remember {
        runCatching {
            com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) ==
                com.google.android.gms.common.ConnectionResult.SUCCESS
        }.getOrDefault(false)
    }
    var mode by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    val titles = listOf("Sign in", "Create account", "Reset password")
    val actions = listOf("Sign in", "Create account", "Send reset link")

    fun fillProfileFromAccount() {
        val name = repo.currentUser?.displayName
        settings?.let { s ->
            if (!name.isNullOrBlank() && s.profileName.isBlank()) s.updateProfileName(name)
        }
    }

    fun handle(result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                if (mode == 2) {
                    info = "Password reset link sent to ${result.email}."
                } else {
                    fillProfileFromAccount()
                    onSignedIn()
                }
            }
            is AuthResult.Error -> error = result.message
        }
    }

    fun submitEmail() {
        if (loading) return
        error = null
        info = null
        loading = true
        scope.launch {
            val result = when (mode) {
                0 -> repo.signIn(email, password)
                1 -> repo.signUp(email, password)
                else -> repo.sendPasswordReset(email)
            }
            loading = false
            handle(result)
        }
    }

    val webClientId = remember {
        context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            .takeIf { it != 0 }?.let { context.getString(it) }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        task.addOnCompleteListener { t ->
            if (t.isSuccessful) {
                val idToken = t.result?.idToken
                if (idToken.isNullOrBlank()) {
                    loading = false
                    error = "Google didn't return a sign-in token. The SHA-1 or " +
                        "Google provider may be misconfigured."
                } else {
                    scope.launch {
                        val r = repo.signInWithGoogleToken(idToken)
                        loading = false
                        handle(r)
                    }
                }
            } else {
                loading = false
                error = googleError(t.exception)
            }
        }
    }

    fun submitGoogle() {
        if (loading) return
        if (webClientId.isNullOrBlank()) {
            error = "Google sign-in isn't set up yet (missing web client id)."
            return
        }
        error = null
        info = null
        loading = true
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        // Clear any cached account first so the picker is always shown.
        client.signOut().addOnCompleteListener {
            googleLauncher.launch(client.signInIntent)
        }
    }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            ScreenHeader(titles[mode], onBack = onBack)

            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CloudDone, contentDescription = null)
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Signing in is optional. It backs up your data and syncs it " +
                            "across devices \u2014 the app still works offline.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (mode != 2 && googleAvailable) {
                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = { submitGoogle() },
                    enabled = !loading,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                    Spacer(Modifier.size(10.dp))
                    Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "or use email",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            if (mode != 2) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                    singleLine = true,
                    enabled = !loading,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))
            GradientPillButton(
                text = if (loading) "Please wait\u2026" else actions[mode],
                enabled = !loading && email.contains("@") &&
                    (mode == 2 || password.length >= 6),
                onClick = { submitEmail() },
                modifier = Modifier.fillMaxWidth()
            )

            if (loading) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }

            error?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            info?.let {
                Spacer(Modifier.height(14.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(18.dp))
            when (mode) {
                0 -> {
                    TextButton(onClick = { mode = 2; error = null }) { Text("Forgot password?") }
                    TextButton(onClick = { mode = 1; error = null }) {
                        Text("New here? Create an account")
                    }
                }
                1 -> TextButton(onClick = { mode = 0; error = null }) {
                    Text("Already have an account? Sign in")
                }
                else -> TextButton(onClick = { mode = 0; error = null }) { Text("Back to sign in") }
            }
        }
    }
}

private fun googleError(e: Exception?): String {
    val msg = e?.message ?: return "Google sign-in failed."
    return when {
        msg.contains("12501", true) || msg.contains("cancel", true) -> "Sign-in cancelled."
        msg.contains("12500", true) || msg.contains("10:", true) ||
            msg.contains("DEVELOPER_ERROR", true) ->
            "Google sign-in is misconfigured (SHA-1 or OAuth client)."
        msg.contains("12502", true) || msg.contains("network", true) ->
            "No connection. Check your internet and retry."
        else -> "Google sign-in failed: $msg"
    }
}
