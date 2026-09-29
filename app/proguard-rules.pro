# MedRemind release shrinker rules.
#
# Keep the home-screen widget and Glance (Compose-for-widgets) entry points,
# which the system instantiates by name / reflection.
-keep class com.medremind.app.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class androidx.glance.** { *; }

# Room generates code that references entities/DAOs directly; keep annotations.
-keepattributes *Annotation*

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
