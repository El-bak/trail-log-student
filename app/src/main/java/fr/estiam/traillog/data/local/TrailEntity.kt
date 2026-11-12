package fr.estiam.traillog.data.local

// TODO M3-LAB1 : écrire TrailEntity.kt et le raccorder aux contrats du projet.

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.estiam.traillog.model.Difficulty
import fr.estiam.traillog.model.Trail

/** Une ligne de la table "trails". */
@Entity(tableName = "trails")
data class TrailEntity(
    @PrimaryKey val id: String,          // l'ID String stable (ex. "trail_001")
    val name: String,
    val region: String,
    val distanceKm: Double,
    val elevationGainM: Int,
    val difficulty: Difficulty,          // enum : Room le stocke automatiquement en texte ("HARD")
    val notes: String,
    val isFavorite: Boolean,
    val updatedAt: Long
)

// Conversions entre le modèle de l'app (Trail) et la table Room (TrailEntity).
fun TrailEntity.toDomain() = Trail(id, name, region, distanceKm, elevationGainM, difficulty, notes, isFavorite, updatedAt)
fun Trail.toEntity() = TrailEntity(id, name, region, distanceKm, elevationGainM, difficulty, notes, isFavorite, updatedAt)
