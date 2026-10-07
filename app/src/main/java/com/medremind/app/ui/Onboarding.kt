package com.medremind.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

private data class OnboardPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String,
    val accent: Color
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        OnboardPage(
            Icons.Rounded.Medication,
            "Welcome to MedRemind",
            "The right medicine, shown to the right person, at the right time \u2014 with help from someone you trust.",
            Color(0xFF6366F1)
        ),
        OnboardPage(
            Icons.Rounded.WbSunny,
            "Today",
            "Your day at a glance. See every dose and its time \u2014 then slide to take, snooze, or skip.",
            Color(0xFFF59E0B)
        ),
        OnboardPage(
            Icons.Rounded.Medication,
            "Your medicine cabinet",
            "Add medicines with a photo, set smart schedules (daily, weekly, as\u2011needed), and track your stock.",
            Color(0xFF10B981)
        ),
        OnboardPage(
            Icons.Rounded.Insights,
            "Progress & Health",
            "Watch your adherence over time, and log blood pressure, glucose and weight \u2014 export a report anytime.",
            Color(0xFF0EA5E9)
        ),
        OnboardPage(
            Icons.Rounded.Favorite,
            "Caregiver",
            "Connect someone you trust. They can add medicines, send reminders and see confirmations \u2014 with your approval.",
            Color(0xFFE11D48)
        ),
        OnboardPage(
            Icons.Rounded.NotificationsActive,
            "Reliable alarms",
            "Full\u2011screen reminders with the medicine photo, working even when your phone is locked.",
            Color(0xFF8B5CF6)
        )
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val item = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = CircleShape, color = item.accent.copy(alpha = 0.16f)) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.accent,
                        modifier = Modifier
                            .padding(28.dp)
                            .size(56.dp)
                    )
                }
                Spacer(Modifier.size(26.dp))
                Text(
                    item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    item.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == pager.currentPage) 10.dp else 8.dp)
                        .background(
                            color = if (index == pager.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape
                        )
                )
            }
        }
        Spacer(Modifier.size(22.dp))
        GradientPillButton(
            text = if (last) "Get started" else "Next",
            onClick = {
                if (last) onDone()
                else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(
            onClick = onDone,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Skip")
        }
    }
}

/** Shown once after the welcome slides: account is optional for the core app. */
@Composable
fun StartChoiceScreen(
    onContinueWithoutAccount: () -> Unit,
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MedGradients.hero()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.18f)
            ) {
                Icon(
                    Icons.Rounded.Medication,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(44.dp)
                )
            }
            Spacer(Modifier.size(20.dp))
            Text(
                "Welcome to MedRemind",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(10.dp))
            Text(
                "An account is only needed to help someone else (caregiver) or " +
                    "keep two phones in sync. The app is free without one.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(28.dp))
            Surface(
                onClick = onContinueWithoutAccount,
                shape = RoundedCornerShape(50),
                color = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    Text("Continue without an account", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.size(10.dp))
            Surface(
                onClick = onSignIn,
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    Text("Sign in", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.size(10.dp))
            Surface(
                onClick = onCreateAccount,
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f),
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.padding(vertical = 15.dp), contentAlignment = Alignment.Center) {
                    Text("Create account", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun AppLockScreen(pin: String?, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var showPin by remember { mutableStateOf(false) }
    val biometricAvailable = remember { BiometricLock.canAuthenticate(context) }

    fun attempt() {
        if (activity != null && biometricAvailable) {
            BiometricLock.prompt(
                activity = activity,
                onSuccess = onUnlocked,
                onFailure = { if (pin != null) showPin = true }
            )
        } else if (pin != null) {
            showPin = true
        } else {
            onUnlocked()
        }
    }

    LaunchedEffect(Unit) { attempt() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MedGradients.hero()),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.size(16.dp))
            Text(
                "MedRemind is locked",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(6.dp))
            Text(
                "Unlock to view your medicines",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.size(24.dp))
            GradientPillButton(
                text = if (biometricAvailable) "Unlock" else "Enter PIN",
                onClick = { attempt() }
            )
        }
    }

    if (showPin && pin != null) {
        PinDialog(
            title = "Enter PIN",
            expected = pin,
            onDismiss = {},
            onSuccess = { onUnlocked() }
        )
    }
}
