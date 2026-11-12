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

/** Implémentation Room du contrat TrailRepository existant (inchangé). */
class RoomTrailRepository(
    context: Context,
    private val dao: TrailDao
) : TrailRepository {
    private val appContext = context.applicationContext   // servira au Lab 2 (lecture du JSON)

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
        // LAB 2 : import de assets/trails.json.
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