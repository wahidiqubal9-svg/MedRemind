package com.medremind.app.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medremind.app.data.purchase.PurchasePlan
import com.medremind.app.data.purchase.PurchaseResult

/**
 * Wraps a paid action: runs it when the user is Pro, otherwise opens the paywall.
 * The paywall itself is routed by AppRoot via [EntitlementViewModel.showPaywall].
 */
@Composable
fun rememberProAction(onAllowed: () -> Unit): () -> Unit {
    val entitlement: EntitlementViewModel = viewModel()
    val isPro by entitlement.isPro.collectAsState()
    return { if (isPro) onAllowed() else entitlement.openPaywall() }
}

@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            "PRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private data class Benefit(val icon: ImageVector, val title: String, val subtitle: String)

private val benefits = listOf(
    Benefit(
        Icons.Rounded.Cloud,
        "Caregiver",
        "Help a family member manage their medicines from your phone."
    ),
    Benefit(
        Icons.Rounded.Description,
        "Adherence & health reports",
        "Export shareable CSV and PDF reports for a doctor."
    ),
    Benefit(
        Icons.Rounded.Backup,
        "Backup & restore",
        "Save a full copy of your data and restore it anytime."
    ),
    Benefit(
        Icons.Rounded.Sync,
        "Sync across devices",
        "Keep your medicines and confirmations in step (coming soon)."
    )
)

@Composable
fun PaywallScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val entitlement: EntitlementViewModel = viewModel()
    val isPro by entitlement.isPro.collectAsState()
    var planIndex by remember { mutableIntStateOf(1) }
    val plans = listOf(PurchasePlan.MONTHLY, PurchasePlan.YEARLY)

    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenHeader("MedRemind Pro", onBack = onBack)

            MedHeroCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Unlock the full MedRemind",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.size(6.dp))
                Text(
                    "Everything essential stays free, always. Pro adds the extras that keep you and your family organised.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            MedCard(modifier = Modifier.fillMaxWidth()) {
                benefits.forEachIndexed { index, benefit ->
                    if (index > 0) Spacer(Modifier.size(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(
                                benefit.icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                benefit.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                benefit.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (isPro) {
                MedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("You're on Pro", fontWeight = FontWeight.Bold)
                            Text(
                                "Thanks for supporting MedRemind.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text("Choose a plan", style = MaterialTheme.typography.titleMedium)
                MedSegmentedButtons(
                    options = plans.map { it.label },
                    selectedIndex = planIndex,
                    onSelect = { planIndex = it },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    plans[planIndex].blurb,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                GradientPillButton(
                    text = "Subscribe",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        entitlement.subscribe(plans[planIndex]) { result ->
                            val message = when (result) {
                                is PurchaseResult.Purchased -> "Welcome to Pro!"
                                is PurchaseResult.Failed -> result.message
                                PurchaseResult.NotAvailable ->
                                    "Purchases will be available at launch."
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = {
                    entitlement.restore { result ->
                        val message = when (result) {
                            is PurchaseResult.Purchased -> "Purchases restored."
                            is PurchaseResult.Failed -> result.message
                            PurchaseResult.NotAvailable ->
                                "Nothing to restore yet."
                        }
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Restore purchases") }

            Text(
                "Payment is charged through Google Play. Cancel anytime from your Play subscriptions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
