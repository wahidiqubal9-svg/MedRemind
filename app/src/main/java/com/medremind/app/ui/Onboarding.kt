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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Medication
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
    val message: String
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        OnboardPage(
            Icons.Rounded.Medication,
            "Never miss a dose",
            "Exact alarms and full-screen reminders for every medicine, even when the phone is locked."
        ),
        OnboardPage(
            Icons.Rounded.CheckCircle,
            "See your progress",
            "Track daily doses and adherence over 7, 30 or 90 days, and share a report with your doctor."
        ),
        OnboardPage(
            Icons.Rounded.Lock,
            "Private by design",
            "Everything stays on your device. Back up, restore or export whenever you like."
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
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
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
