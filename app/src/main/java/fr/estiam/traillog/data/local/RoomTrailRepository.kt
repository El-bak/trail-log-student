package fr.estiam.traillog.data.local

// TODO M3-LAB1 : écrire RoomTrailRepository.kt et le raccorder aux contrats du projet.

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import fr.estiam.traillog.data.TrailRepository
import fr.estiam.traillog.model.Trail
import fr.estiam.traillog.model.TrailSortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import fr.estiam.traillog.model.Difficulty
import org.json.JSONArray

/** Implémentation Room du contrat TrailRepository existant (inchangé). */
class RoomTrailRepository(
    context: Context,
    private val dao: TrailDao
) : TrailRepository {
    private val appContext = context.applicationContext   // servira au Lab 2 (lecture du JSON)

    private val db: TrailDatabase by lazy { TrailDatabase.get(appContext) }

    override fun observeTrails(): Flow<List<Trail>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun pagingSource(sort: TrailSortOrder, favoritesOnly: Boolean): PagingSource<Int, Trail> =
        EntityPagingSource(
            when {
                favoritesOnly && sort == TrailSortOrder.NAME -> dao.pagingFavoriteByName()
                favoritesOnly && sort == TrailSortOrder.DISTANCE -> dao.pagingFavoriteByDistance()
                sort == TrailSortOrder.DISTANCE -> dao.pagingByDistance()
                else -> dao.pagingByName()
            }
        )

    override suspend fun getTrail(id: String): Trail? = dao.byId(id)?.toDomain()

    override suspend fun save(trail: Trail) {
        require(trail.isValid()) { "Invalid trail" }   // refuse nom vide, distance négative…
        dao.upsert(trail.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun seedIfNeeded() {
        val markerKey = "initial_trails"

        // Cas habituel : l'import a déjà été fait, on sort tout de suite.
        if (dao.wasSeeded(markerKey)) return

        // Lire et valider AVANT la transaction (pas de lecture de fichier pendant qu'on verrouille la base).
        val initial = readAndValidateAssets()

        // Tout ou rien : les 30 sentiers ET le marqueur, ou aucun des deux.
        db.withTransaction {
            if (!dao.wasSeeded(markerKey)) {
                dao.insertAll(initial.map { it.toEntity() })
                dao.putSeedMarker(SeedMarker(markerKey))
            }
        }
    }

    private fun readAndValidateAssets(): List<Trail> {
        val json = appContext.assets.open("trails.json")
            .bufferedReader(Charsets.UTF_8)
            .use { reader -> reader.readText() }        // UTF-8 + flux fermé automatiquement
        val array = JSONArray(json)                       // le fichier commence par [ : c'est un tableau
        val seenIds = mutableSetOf<String>()

        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            val id = obj.getString("id").trim()
            val rawDifficulty = obj.getString("difficulty")

            // Difficulté inconnue → erreur claire, pas de valueOf() qui plante sans message.
            val difficulty = Difficulty.entries.firstOrNull { it.name == rawDifficulty }
                ?: error("Unknown difficulty '$rawDifficulty' at JSON index $index")

            val trail = Trail(
                id = id,
                name = obj.getString("name").trim(),
                region = obj.getString("region"),
                distanceKm = obj.getDouble("distanceKm"),
                elevationGainM = obj.getInt("elevationGainM"),
                difficulty = difficulty,
                notes = obj.optString("notes", ""),
                isFavorite = obj.optBoolean("isFavorite", false),
                updatedAt = 0L
            )

            require(trail.isValid()) { "Invalid trail at JSON index $index: $id" }   // nom/id vides, valeurs négatives
            require(seenIds.add(id)) { "Duplicate trail ID in JSON: $id" }          // id en double
            trail
        }
    }
}

/** Transforme la PagingSource<Int, TrailEntity> de Room en PagingSource<Int, Trail>. */
private class EntityPagingSource(
    private val delegate: PagingSource<Int, TrailEntity>
) : PagingSource<Int, Trail>() {
    init {
        // Quand Room invalide sa source (table modifiée), on invalide aussi celle-ci.
        delegate.registerInvalidatedCallback { invalidate() }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Trail> =
        when (val result = delegate.load(params)) {
            is LoadResult.Page -> LoadResult.Page(
                data = result.data.map { it.toDomain() },
                prevKey = result.prevKey,
                nextKey = result.nextKey,
                itemsBefore = result.itemsBefore,
                itemsAfter = result.itemsAfter
            )
            is LoadResult.Error -> LoadResult.Error(result.throwable)
            is LoadResult.Invalid -> LoadResult.Invalid()
        }

    override fun getRefreshKey(state: PagingState<Int, Trail>): Int? = state.anchorPosition
}