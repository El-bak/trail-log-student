package fr.estiam.traillog.data

import androidx.paging.PagingSource
import fr.estiam.traillog.model.Trail
import fr.estiam.traillog.model.TrailSortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** M5 Lab 1: exercise the real MemoryTrailRepository without an Android device.
 * The repository already implements the TrailRepository contract. */
/** M5 Lab 1 : tests du vrai MemoryTrailRepository, sans appareil ni base de données. */
class TrailRepositoryLocalTest {

    // Fixtures déterministes : IDs String stables, noms et distances connus.
    private val pine = Trail("pine-1", "Pine Loop", "Vosges", 3.2, 120)
    private val ridge = Trail("ridge-2", "Ridge Run", "Alpes", 7.5, 540)
    private val creek = Trail("creek-3", "Creekside", "Jura", 1.0, 30)
    private val fixtureIds = setOf("pine-1", "ridge-2", "creek-3")

    /** Un dépôt neuf par test (isolation) : il contient déjà 45 trajets de démo + nos 3 fixtures. */
    private fun repoWithFixtures(): MemoryTrailRepository = runBlocking {
        MemoryTrailRepository().also { repo -> listOf(pine, ridge, creek).forEach { repo.save(it) } }
    }

    /** Charge une page de 100 éléments (> 48) via le PagingSource, comme le ferait la liste. */
    private suspend fun loadAll(repo: TrailRepository, sort: TrailSortOrder, favoritesOnly: Boolean = false): List<Trail> {
        val result = repo.pagingSource(sort, favoritesOnly).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false)
        )
        return (result as PagingSource.LoadResult.Page).data
    }

    @Test fun saveThenGetTrailReturnsSameData() = runBlocking<Unit> {
        // Arrange
        val repo = MemoryTrailRepository()
        // Act
        repo.save(pine)
        // Assert
        assertEquals(pine, repo.getTrail("pine-1"))
    }

    @Test fun savingExistingIdUpdatesInsteadOfDuplicating() = runBlocking<Unit> {
        val repo = repoWithFixtures()
        repo.save(pine.copy(name = "Pine Loop Updated", distanceKm = 4.0))
        val stored = repo.getTrail("pine-1")
        assertEquals("Pine Loop Updated", stored?.name)
        assertEquals(4.0, stored!!.distanceKm, 0.0001)
        assertEquals(1, repo.observeTrails().first().count { it.id == "pine-1" })
    }

    @Test fun deleteRemovesOnlyThatTrailFromObservedList() = runBlocking<Unit> {
        val repo = repoWithFixtures()
        repo.delete("ridge-2")
        val ids = repo.observeTrails().first().map { it.id }
        assertFalse("ridge-2" in ids)
        assertTrue("pine-1" in ids && "creek-3" in ids)
        assertNull(repo.getTrail("ridge-2"))
    }

    @Test fun nameSortOrdersKnownTrailsAlphabetically() = runBlocking<Unit> {
        val repo = repoWithFixtures()
        val names = loadAll(repo, TrailSortOrder.NAME).filter { it.id in fixtureIds }.map { it.name }
        assertEquals(listOf("Creekside", "Pine Loop", "Ridge Run"), names)
    }

    @Test fun distanceSortOrdersKnownTrailsLongestFirst() = runBlocking<Unit> {
        val repo = repoWithFixtures()
        val names = loadAll(repo, TrailSortOrder.DISTANCE).filter { it.id in fixtureIds }.map { it.name }
        assertEquals(listOf("Ridge Run", "Pine Loop", "Creekside"), names)
    }

    @Test fun favoritesOnlyExcludesNonFavoriteFixtures() = runBlocking<Unit> {
        val repo = repoWithFixtures()                       // aucune fixture n'est favorite
        val favorites = loadAll(repo, TrailSortOrder.NAME, favoritesOnly = true)
        assertTrue(favorites.none { it.id in fixtureIds })
        assertTrue(favorites.all { it.isFavorite })
    }

    @Test fun unknownIdReturnsNull() = runBlocking<Unit> {
        val repo = repoWithFixtures()
        assertNull(repo.getTrail("does-not-exist"))
    }

    @Test fun invalidTrailIsRejected() {
        val repo = MemoryTrailRepository()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.save(Trail("bad-1", "   ", "Alpes", 2.0, 10)) }   // nom vide
        }
    }
}
