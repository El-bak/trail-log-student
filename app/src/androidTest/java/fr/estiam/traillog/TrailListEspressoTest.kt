package fr.estiam.traillog

import android.os.SystemClock
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.estiam.traillog.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** M5 Lab 2 : parcours utilisateur réels sur le catalogue TrailLog. */
@RunWith(AndroidJUnit4::class)
class TrailListEspressoTest {
    @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // Nom unique à chaque exécution : jamais de faux positif avec un trajet d'un essai précédent.
    // "AA " + distance élevée : le trajet est en tête de liste en tri NAME comme en tri DISTANCE.
    private val trailName = "AA Espresso ${System.currentTimeMillis()}"
    private val trailRegion = "Espresso Region"

    @Test fun catalogueIsDisplayed() {
        onView(withId(R.id.trailList)).check(matches(isDisplayed()))
    }

    @Test fun addButtonOpensEditForm() {
        onView(withId(R.id.addButton)).perform(click())
        onView(withId(R.id.nameInput)).check(matches(isDisplayed()))
        onView(withId(R.id.saveButton)).check(matches(isDisplayed()))
    }

    @Test fun savedTrailAppearsInCatalogue() {
        // Arrange + Act : saisie d'un trajet identifiable via l'interface.
        onView(withId(R.id.addButton)).perform(click())
        onView(withId(R.id.nameInput)).perform(replaceText(trailName))
        onView(withId(R.id.regionInput)).perform(replaceText(trailRegion))
        onView(withId(R.id.distanceInput)).perform(replaceText("99.9"))
        onView(withId(R.id.elevationInput)).perform(replaceText("321"), closeSoftKeyboard())
        onView(withId(R.id.saveButton)).perform(click())

        // Assert : retour au catalogue, puis recherche de LA ligne créée, par contenu.
        onView(withId(R.id.trailList)).check(matches(isDisplayed()))
        onView(withId(R.id.trailList)).perform(waitForItem(hasDescendant(withText(trailName))))
        onView(withId(R.id.trailList))
            .perform(RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(hasDescendant(withText(trailName))))
        onView(withText(trailName)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.region), hasSibling(withText(trailName))))
            .check(matches(withText(trailRegion)))
    }

    /** Nettoyage : ne supprime QUE les trajets créés par ce test (nom unique). */
    @After fun deleteTestTrails() = runBlocking<Unit> {
        val repo = ApplicationProvider.getApplicationContext<TrailLogApplication>().trails
        repo.observeTrails().first()
            .filter { it.name.startsWith("AA Espresso ") }
            .forEach { repo.delete(it.id) }
    }

    /**
     * Synchronisation déterministe : Room et Paging travaillent sur des threads d'arrière-plan
     * qu'Espresso ne surveille pas. On fait tourner la boucle principale par petits pas
     * (sans Thread.sleep) jusqu'à ce que la ligne existe, avec un délai maximal.
     */
    private fun waitForItem(itemMatcher: Matcher<View>, timeoutMs: Long = 5_000): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(RecyclerView::class.java)
        override fun getDescription() = "attendre une ligne du RecyclerView correspondant à $itemMatcher"
        override fun perform(uiController: UiController, view: View) {
            val recycler = view as RecyclerView
            val deadline = SystemClock.uptimeMillis() + timeoutMs
            while (SystemClock.uptimeMillis() < deadline) {
                if (hasMatchingItem(recycler, itemMatcher)) return
                uiController.loopMainThreadForAtLeast(50)
            }
            throw AssertionError("Délai dépassé : aucune ligne ne correspond à $itemMatcher")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun hasMatchingItem(recycler: RecyclerView, itemMatcher: Matcher<View>): Boolean {
        val adapter = recycler.adapter as? RecyclerView.Adapter<RecyclerView.ViewHolder> ?: return false
        for (position in 0 until adapter.itemCount) {
            val holder = adapter.createViewHolder(recycler, adapter.getItemViewType(position))
            adapter.bindViewHolder(holder, position)
            if (itemMatcher.matches(holder.itemView)) return true
        }
        return false
    }
}