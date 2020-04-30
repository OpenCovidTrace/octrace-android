package org.opencovidtrace.octrace.di


import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.opencovidtrace.octrace.db.Database

object DatabaseProvider : IndependentProvider<Database>() {

    override fun initInstance(): Database {
        val context by ContextProvider()
        return Room.databaseBuilder(context, Database::class.java, "Octrace.db")
            .addMigrations(provide3To4Migration)
            .fallbackToDestructiveMigration()
            .build()
    }

    private val provide3To4Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE log_table ADD COLUMN tag TEXT")
        }
    }

}