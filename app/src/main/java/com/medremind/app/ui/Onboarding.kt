package com.medremind.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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

private data class IntroSlide(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val message: String
)

/**
 * Premium first-run intro: a few branded slides before the account choice.
 * Shown over the brand gradient so the very first impression feels polished.
 */
@Composable
fun IntroCarousel(onDone: () -> Unit) {
    val slides = listOf(
        IntroSlide(
            Icons.Rounded.NotificationsActive,
            "Your medicines, on time",
            "Full\u2011screen reminders with the medicine photo \u2014 working even when " +
                "your phone is locked."
        ),
        IntroSlide(
            Icons.Rounded.Insights,
            "Health & progress at a glance",
            "See your adherence over time, and log blood pressure, glucose and weight."
        ),
        IntroSlide(
            Icons.Rounded.Favorite,
            "Help from someone you trust",
            "Connect a caregiver so they can remind you and see confirmations \u2014 " +
                "with your approval."
        )
    )
    val pager = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLast = pager.currentPage == slides.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MedGradients.hero())
    ) {
        TextButton(
            onClick = onDone,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Text("Skip", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                val slide = slides[page]
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(34.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.size(150.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = slide.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(68.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(34.dp))
                    Text(
                        slide.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        slide.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(slides.size) { index ->
                    val selected = index == pager.currentPage
                    val width by animateDpAsState(
                        targetValue = if (selected) 22.dp else 8.dp,
                        animationSpec = tween(260),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(width)
                            .height(8.dp)
                            .background(
                                color = if (selected) Color.White
                                else Color.White.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Surface(
                onClick = {
                    if (isLast) onDone()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                shape = RoundedCornerShape(50),
                color = Color.White,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isLast) "Get started" else "Next",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
