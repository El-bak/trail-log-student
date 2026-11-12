package fr.estiam.traillog.data.local

// TODO M3-LAB1 : écrire TrailDao.kt et le raccorder aux contrats du projet.

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrailDao {
    // Pagination : le tri est fait par SQL (ORDER BY), donc sur tout le catalogue.
    @Query("SELECT * FROM trails ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun pagingByName(): PagingSource<Int, TrailEntity>

    @Query("SELECT * FROM trails ORDER BY distanceKm DESC, name COLLATE NOCASE ASC, id ASC")
    fun pagingByDistance(): PagingSource<Int, TrailEntity>

    @Query("SELECT * FROM trails WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun pagingFavoriteByName(): PagingSource<Int, TrailEntity>

    @Query("SELECT * FROM trails WHERE isFavorite = 1 ORDER BY distanceKm DESC, name COLLATE NOCASE ASC, id ASC")
    fun pagingFavoriteByDistance(): PagingSource<Int, TrailEntity>

    @Query("SELECT * FROM trails ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<TrailEntity>>

    @Query("SELECT * FROM trails WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): TrailEntity?

    @Query("SELECT COUNT(*) FROM trails")
    suspend fun count(): Int

    // Ajout OU modification : REPLACE écrase la ligne qui a le même id.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(trail: TrailEntity)

    @Query("DELETE FROM trails WHERE id = :id")
    suspend fun deleteById(id: String)
}