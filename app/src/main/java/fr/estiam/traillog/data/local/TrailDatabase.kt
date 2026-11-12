package fr.estiam.traillog.data.local

// TODO M3-LAB1 : écrire TrailDatabase.kt et le raccorder aux contrats du projet.

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TrailEntity::class], version = 1, exportSchema = false)
abstract class TrailDatabase : RoomDatabase() {
    abstract fun trailDao(): TrailDao

    companion object {
        @Volatile
        private var instance: TrailDatabase? = null

        // Une seule instance de la base pour toute l'application.
        fun get(context: Context): TrailDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TrailDatabase::class.java,
                    "traillog.db"
                ).build().also { instance = it }
            }
    }
}