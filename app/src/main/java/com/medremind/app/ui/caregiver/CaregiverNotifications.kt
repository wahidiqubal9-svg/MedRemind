package com.medremind.app.ui.caregiver

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.ui.MedCard
import com.medremind.app.ui.MedClickableCard
import com.medremind.app.ui.MedEmptyState
import com.medremind.app.ui.ScreenHeader
import com.medremind.app.ui.SquareIconButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bell button with an unread badge, shown in the main tab headers. */
@Composable
fun NotificationBell(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val vm: CaregiverViewModel = viewModel()
    val unread by vm.unreadCount.collectAsState()
    Box(modifier = modifier) {
        SquareIconButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = "Notifications",
            onClick = onClick
        )
        if (unread > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (unread > 9) "9+" else unread.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenPatient: (Long) -> Unit
) {
    BackHandler { onBack() }
    val vm: CaregiverViewModel = viewModel()
    val items by vm.notifications.collectAsState()

    LaunchedEffect(Unit) { vm.markNotificationsSeen() }

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScreenHeader("Notifications", onBack = onBack)

            Text(
                "Caregiver activity, missed doses and low stock \u2014 on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (items.isEmpty()) {
                MedEmptyState(
                    icon = Icons.Rounded.Notifications,
                    title = "All caught up",
                    message = "Caregiver reminders, confirmations and alerts will show up here.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp)
                )
            }

            items.forEach { item ->
                val content: @Composable () -> Unit = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val tint = when (item.type) {
                            NotificationType.MISSED -> Color(0xFFD53862)
                            NotificationType.LOW_STOCK -> Color(0xFFB45309)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val icon = when (item.type) {
                            NotificationType.MISSED -> Icons.Rounded.ErrorOutline
                            NotificationType.LOW_STOCK -> Icons.Rounded.Inventory2
                            else -> Icons.Rounded.Favorite
                        }
                        Surface(
                            shape = CircleShape,
                            color = tint.copy(alpha = 0.14f),
                            contentColor = tint
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(9.dp)
                                    .size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            SimpleDateFormat("h:mm a", Locale.getDefault())
                                .format(Date(item.at)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (item.patientProfileId > 0L) {
                    MedClickableCard(
                        onClick = { onOpenPatient(item.patientProfileId) },
                        modifier = Modifier.fillMaxWidth()
                    ) { content() }
                } else {
                    MedCard(modifier = Modifier.fillMaxWidth()) { content() }
                }
            }
        }
    }
}
