package com.medremind.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Medicine::class,
        Schedule::class,
        DoseEvent::class,
        Metric::class,
        Patient::class,
        CaregiverLink::class,
        PairingRequest::class,
        CaregiverActivity::class
    ],
    version = 11,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseEventDao(): DoseEventDao
    abstract fun metricDao(): MetricDao
    abstract fun caregiverDao(): CaregiverDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN refillThreshold INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Profile scoping (0 = device owner) keeps existing data intact.
                db.execSQL("ALTER TABLE medicines ADD COLUMN profileId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE metrics ADD COLUMN profileId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE dose_events ADD COLUMN source TEXT NOT NULL DEFAULT 'SCHEDULED'")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `patients` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`relation` TEXT NOT NULL, " +
                        "`isSelf` INTEGER NOT NULL, " +
                        "`avatarPath` TEXT, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caregiver_links` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`patientProfileId` INTEGER NOT NULL, " +
                        "`caregiverName` TEXT NOT NULL, " +
                        "`direction` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`permissions` INTEGER NOT NULL, " +
                        "`pairingId` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pairing_requests` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`token` TEXT NOT NULL, " +
                        "`code` TEXT NOT NULL, " +
                        "`direction` TEXT NOT NULL, " +
                        "`peerName` TEXT NOT NULL, " +
                        "`expiresAt` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `caregiver_activity` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`linkId` INTEGER NOT NULL, " +
                        "`patientProfileId` INTEGER NOT NULL, " +
                        "`actor` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`medicineName` TEXT NOT NULL, " +
                        "`message` TEXT NOT NULL, " +
                        "`at` INTEGER NOT NULL)"
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE metrics ADD COLUMN context TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN cycleOnDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedules ADD COLUMN cycleOffDays INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE schedules ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedules ADD COLUMN selectedDates TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN batchNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN expiryDate INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN category TEXT NOT NULL DEFAULT 'PRESCRIPTION'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN form TEXT NOT NULL DEFAULT 'tablet'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN prescriber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN rxNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN refillsLeft INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN packSize INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN autoRefillDate INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE medicines ADD COLUMN intakeInstruction TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `metrics` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`value` REAL NOT NULL, " +
                        "`value2` REAL NOT NULL, " +
                        "`recordedAt` INTEGER NOT NULL)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medremind.db"
                )
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11
                    )
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                    .also { instance = it }
            }
    }
}
