package org.opencovidtrace.octrace.di


import androidx.room.Room
import org.opencovidtrace.octrace.db.Database

object DatabaseProvider : IndependentProvider<Database>() {

    override fun initInstance(): Database {
        val context by ContextProvider()
        return Room.databaseBuilder(context, Database::class.java, "Octrace.db")
            .fallbackToDestructiveMigration()
            .build()
    }

}