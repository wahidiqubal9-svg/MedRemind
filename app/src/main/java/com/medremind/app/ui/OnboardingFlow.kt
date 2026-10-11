package com.medremind.app.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.medremind.app.R

/**
 * Guided first-run onboarding: welcome → who you manage for → add first medicine
 * → permission intro → notification → exact alarm → test reminder → done.
 */
@Composable
fun OnboardingFlow(
    settings: SettingsViewModel,
    vm: MedicineViewModel,
    isPro: Boolean,
    onOpenCaregiverPaywall: () -> Unit,
    onFinished: () -> Unit
) {
    when (settings.onboardStep) {
        0 -> WelcomeStep(onNext = { settings.goToOnboardStep(1) })
        1 -> ManagingForStep(
            selected = settings.managingFor,
            isPro = isPro,
            onSelect = settings::selectManagingFor,
            onOpenCaregiverPaywall = onOpenCaregiverPaywall,
            onBack = { settings.goToOnboardStep(0) },
            onNext = { settings.goToOnboardStep(2) }
        )
        2 -> AddFirstMedicineStep(
            vm = vm,
            onDone = { settings.goToOnboardStep(3) }
        )
        3 -> PermissionIntroStep(
            onBack = { settings.goToOnboardStep(2) },
            onNext = { settings.goToOnboardStep(4) }
        )
        4 -> NotificationPermissionStep(
            onBack = { settings.goToOnboardStep(3) },
            onNext = { settings.goToOnboardStep(5) }
        )
        5 -> ExactAlarmPermissionStep(
            onBack = { settings.goToOnboardStep(4) },
            onNext = { settings.goToOnboardStep(6) }
        )
        6 -> OverlayPermissionStep(
            onBack = { settings.goToOnboardStep(5) },
            onNext = { settings.goToOnboardStep(7) }
        )
        7 -> TestReminderStep(
            vm = vm,
            onBack = { settings.goToOnboardStep(6) },
            onNext = { settings.goToOnboardStep(8) }
        )
        else -> SetupCompleteStep(
            vm = vm,
            onGoToDashboard = onFinished,
            onAddAnother = onFinished
        )
    }
}

private const val TOTAL_STEPS = 9

@Composable
private fun OnboardingScaffold(
    stepIndex: Int,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                } else {
                    Spacer(Modifier.width(44.dp))
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(TOTAL_STEPS) { i ->
                        Box(
                            modifier = Modifier
                                .size(width = if (i == stepIndex) 20.dp else 7.dp, height = 7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (i <= stepIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(44.dp))
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun StepArt(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(112.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun Bullet(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(8.dp).size(16.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---------------------------------------------------------------- Step 1: welcome

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    OnboardingScaffold(stepIndex = 0) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            StepArt(Icons.Rounded.Medication)
            Spacer(Modifier.height(24.dp))
            StepTitle(
                "Welcome to MedRemind",
                "Never miss a medicine. Smart reminders, health tracking and " +
                    "caregiver support \u2014 all in one place."
            )
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Bullet(Icons.Rounded.NotificationsActive, "Reliable medicine reminders")
                Bullet(Icons.Rounded.Info, "Track your health & progress")
                Bullet(Icons.Rounded.Favorite, "Optional caregiver support")
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
            GradientPillButton(
                text = "Get started",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ------------------------------------------------------- Step 2: who are you managing

@Composable
private fun ManagingForStep(
    selected: String,
    isPro: Boolean,
    onSelect: (String) -> Unit,
    onOpenCaregiverPaywall: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    OnboardingScaffold(stepIndex = 1, onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(8.dp))
            StepTitle(
                "Who are you managing medicines for?",
                "You can add other people later for caregiving support."
            )
            Spacer(Modifier.height(24.dp))
            UseCaseCard(
                painterRes = R.drawable.il_avatar_me,
                title = "Myself",
                subtitle = "My own medicines and reminders",
                selected = selected == "me",
                pro = false,
                onClick = { onSelect("me") }
            )
            Spacer(Modifier.height(14.dp))
            UseCaseCard(
                painterRes = R.drawable.il_avatar_family,
                title = "Someone else",
                subtitle = "A family member or someone I care for",
                selected = selected == "other",
                pro = true,
                // Tapping this opens the caregiver paywall straight away.
                onClick = { if (isPro) onSelect("other") else onOpenCaregiverPaywall() }
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Caregiving is a Pro feature \u2014 you (the caregiver) subscribe, and " +
                    "the person you care for pays nothing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            GradientPillButton(
                text = "Continue",
                onClick = { if (selected == "other" && !isPro) onOpenCaregiverPaywall() else onNext() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UseCaseCard(
    painterRes: Int,
    title: String,
    subtitle: String,
    selected: Boolean,
    pro: Boolean,
    onClick: () -> Unit
) {
    val border = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, border, RoundedCornerShape(22.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(painterRes),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (pro) {
                        Spacer(Modifier.width(8.dp))
                        ProBadge()
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ------------------------------------------------------- Step 3: add first medicine

@Composable
private fun AddFirstMedicineStep(vm: MedicineViewModel, onDone: () -> Unit) {
    AddEditMedicineScreen(
        initial = null,
        vm = vm,
        onCancel = onDone,
        onDone = onDone
    )
}

// ------------------------------------------------------- Step 4: permission intro

@Composable
private fun PermissionIntroStep(onBack: () -> Unit, onNext: () -> Unit) {
    OnboardingScaffold(stepIndex = 3, onBack = onBack) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(112.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                StepTitle(
                    "Make your reminders reliable",
                    "To alert you on time, MedRemind needs a few permissions. You " +
                        "can change these later in Settings."
                )
            }
            Spacer(Modifier.height(24.dp))
            MedCard(modifier = Modifier.fillMaxWidth()) {
                PermissionInfoRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",
                    subtitle = "Shows the reminder when it's time to take medicine."
                )
                Spacer(Modifier.height(16.dp))
                PermissionInfoRow(
                    icon = Icons.Rounded.Info,
                    title = "Exact alarms",
                    subtitle = "Fires the reminder at the exact scheduled time."
                )
                Spacer(Modifier.height(16.dp))
                PermissionInfoRow(
                    icon = Icons.Rounded.Warning,
                    title = "Full-screen alerts",
                    subtitle = "Shows the medicine photo over the lock screen."
                )
            }
            Spacer(Modifier.weight(1f))
            GradientPillButton(
                text = "Continue",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ------------------------------------------------------- Step 5: notification

@Composable
private fun NotificationPermissionStep(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onNext() }

    OnboardingScaffold(stepIndex = 4, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            StepArt(Icons.Rounded.Notifications)
            Spacer(Modifier.height(24.dp))
            StepTitle(
                "Allow MedRemind to send you notifications?",
                "You'll get a reminder exactly when it's time for a medicine."
            )
            Spacer(Modifier.weight(1f))
            GradientPillButton(
                text = "Ok",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onNext()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNext) { Text("Not now") }
        }
    }
}

// ------------------------------------------------------- Step 6: exact alarms

@Composable
private fun ExactAlarmPermissionStep(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    OnboardingScaffold(stepIndex = 5, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            StepArt(Icons.Rounded.Info)
            Spacer(Modifier.height(24.dp))
            StepTitle(
                "Allow exact alarms",
                "This lets MedRemind alert you at the exact time you scheduled your medicine."
            )
            Spacer(Modifier.weight(1f))
            GradientPillButton(
                text = "Open settings",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + context.packageName)
                                )
                            )
                        }
                    }
                    onNext()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNext) { Text("Skip for now") }
        }
    }
}

// ----------------------------------------------- Step 7: display over other apps

@Composable
private fun OverlayPermissionStep(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    OnboardingScaffold(stepIndex = 6, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            StepArt(Icons.Rounded.Warning)
            Spacer(Modifier.height(24.dp))
            StepTitle(
                "Allow full-screen reminders",
                "Turn on \u201cDisplay over other apps\u201d so the reminder can pop up " +
                    "as a full screen even while you're using your phone."
            )
            Spacer(Modifier.weight(1f))
            GradientPillButton(
                text = "Open settings",
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + context.packageName)
                            )
                        )
                    }
                    onNext()
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onNext) { Text("Skip for now") }
        }
    }
}

// ------------------------------------------------------- Step 8: test reminder

@Composable
private fun TestReminderStep(vm: MedicineViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    var sent by remember { mutableStateOf(false) }
    OnboardingScaffold(stepIndex = 7, onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            StepArt(Icons.Rounded.NotificationsActive)
            Spacer(Modifier.height(24.dp))
            StepTitle(
                "Let's send a test reminder",
                if (sent) "Your reminder will appear in about 10 seconds. Lock the " +
                    "screen to see the full-screen alarm."
                else "Make sure everything is working. It only takes a few seconds."
            )
            Spacer(Modifier.weight(1f))
            if (sent) {
                GradientPillButton(
                    text = "Continue",
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                GradientPillButton(
                    text = "Send test reminder",
                    onClick = {
                        vm.triggerTestAlarm()
                        sent = true
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onNext) { Text("Skip") }
            }
        }
    }
}

// ------------------------------------------------------- Step 8: done

@Composable
private fun SetupCompleteStep(
    vm: MedicineViewModel,
    onGoToDashboard: () -> Unit,
    onAddAnother: () -> Unit
) {
    val context = LocalContext.current
    val first = vm.medicines.value.firstOrNull()
    OnboardingScaffold(stepIndex = 8) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(26.dp).size(52.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            StepTitle(
                "You're all set!",
                "Your reminders are ready. Here's a quick summary."
            )
            Spacer(Modifier.height(22.dp))
            if (first != null) {
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MedIconSquare(
                            label = first.name,
                            seed = first.id,
                            photoPath = first.photoPath
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                first.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                listOf(first.strength, "0 of 1 doses today")
                                    .filter { it.isNotBlank() }.joinToString(" \u00b7 "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No medicine added yet. You can add one from the dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            GradientPillButton(
                text = "Go to my dashboard",
                onClick = onGoToDashboard,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAddAnother) { Text("Add another medicine") }
        }
    }
}
