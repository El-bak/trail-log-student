package fr.estiam.traillog.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Preuve durable que l'import initial a déjà été fait. */
@Entity(tableName = "seed_markers")
data class SeedMarker(
    @PrimaryKey val key: String
)