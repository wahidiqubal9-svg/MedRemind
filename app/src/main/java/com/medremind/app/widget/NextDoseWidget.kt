package com.medremind.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.medremind.app.alarm.ReminderScheduler
import com.medremind.app.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WidgetState(
    val medicine: String,
    val time: String,
    val empty: Boolean = false
)

class NextDoseWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = withContext(Dispatchers.IO) { loadState(context) }
        provideContent { WidgetContent(state) }
    }

    private suspend fun loadState(context: Context): WidgetState {
        val db = AppDatabase.get(context)
        val now = System.currentTimeMillis()
        var name: String? = null
        var trigger = Long.MAX_VALUE
        db.scheduleDao().getAllOnce().filter { it.enabled }.forEach { schedule ->
            val next = ReminderScheduler.nextTrigger(schedule, now) ?: return@forEach
            if (next < trigger) {
                trigger = next
                name = db.medicineDao().byId(schedule.medicineId)?.name
            }
        }
        val medicine = name ?: return WidgetState("", "", empty = true)
        val time = SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(Date(trigger))
        return WidgetState(medicine, time)
    }
}

@Composable
private fun WidgetContent(state: WidgetState) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF4F46E5)))
            .padding(14.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = "NEXT DOSE",
            style = TextStyle(
                color = ColorProvider(Color(0xFFE0E7FF)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        if (state.empty) {
            Text(
                text = "No doses scheduled",
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        } else {
            Text(
                text = state.medicine,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = state.time,
                style = TextStyle(
                    color = ColorProvider(Color(0xFFE0E7FF)),
                    fontSize = 13.sp
                )
            )
        }
    }
}

class NextDoseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextDoseWidget()
}
