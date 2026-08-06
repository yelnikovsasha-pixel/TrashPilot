package com.trashpilot.app.core.trashdna

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TrashDnaSessionEntity::class, TrashDnaStateEntity::class],
    version = 3,
    exportSchema = false
)
abstract class TrashDnaDatabase : RoomDatabase() {
    abstract fun trashDnaDao(): TrashDnaDao

    companion object {
        @Volatile private var instance: TrashDnaDatabase? = null

        fun get(context: Context): TrashDnaDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrashDnaDatabase::class.java,
                    "trash-dna.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trash_dna_sessions ADD COLUMN scannedFileCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trash_dna_sessions ADD COLUMN scanDurationMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trash_dna_sessions ADD COLUMN privacyAppsChecked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE trash_dna_sessions ADD COLUMN privacySensitiveAppCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf(
                    "usedStorageBytes", "imageBytes", "videoBytes", "audioBytes",
                    "documentBytes", "downloadBytes", "messengerMediaBytes", "screenshotBytes",
                    "largeFileBytes", "largeVideoBytes", "hiddenFileBytes"
                ).forEach { column ->
                    db.execSQL("ALTER TABLE trash_dna_sessions ADD COLUMN $column INTEGER NOT NULL DEFAULT 0")
                }
                db.execSQL("ALTER TABLE trash_dna_sessions ADD COLUMN messengerSourceName TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS trash_dna_state (`key` TEXT NOT NULL, " +
                        "resetAtMillis INTEGER NOT NULL, PRIMARY KEY(`key`))"
                )
            }
        }
    }
}
