package com.example.focusguard

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DemoRepositoryTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    // Keep automated checks isolated from the user's real configuration.
    private val context = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("test_demo_$name", mode)
    }
    private val prefs get() = context.getSharedPreferences("focusguard", Context.MODE_PRIVATE)
    private val app = ManagedApp("com.example.demo", "Demo", 45)
    @Before fun reset() { prefs.edit().clear().commit() }
    @After fun cleanup() { prefs.edit().clear().commit() }

    @Test fun demoPersistsWithoutChangingDailyLimits() {
        val repo = FocusRepository(context)
        repo.saveApps(listOf(app))
        repo.startDemo(listOf(app.copy(limitMinutes = 30)))
        val reopened = FocusRepository(context)
        assertTrue(reopened.enabled)
        assertEquals(setOf(app.packageName), reopened.demoPackages())
        assertEquals(45, reopened.apps().single().limitMinutes)
        reopened.stopDemo()
        assertFalse(reopened.enabled)
        assertTrue(reopened.demoPackages().isEmpty())
        assertEquals(45, reopened.apps().single().limitMinutes)
    }
    @Test fun repeatingDemoClearsOnlyTargetRewardAndRestoresOriginalSwitch() {
        val repo = FocusRepository(context)
        repo.enabled = true
        repo.grantReward("other.app")
        val otherReward = repo.rewardUntil("other.app")
        repo.startDemo(listOf(app))
        repo.grantReward(app.packageName)
        assertTrue(repo.rewardUntil(app.packageName) > System.currentTimeMillis())
        repo.startDemo(listOf(app))
        assertEquals(0L, repo.rewardUntil(app.packageName))
        assertEquals(otherReward, repo.rewardUntil("other.app"))
        repo.stopDemo()
        assertTrue(repo.enabled)
    }
    @Test fun demoFromPreviousDayIsInactive() {
        val repo = FocusRepository(context)
        repo.startDemo(listOf(app))
        prefs.edit().putLong("demoDay", FocusRepository.dayStart() - 86_400_000).commit()
        assertTrue(repo.demoPackages().isEmpty())
    }
    @Test fun emptySelectionDoesNotEnableProtection() {
        val repo = FocusRepository(context)
        repo.startDemo(emptyList())
        assertFalse(repo.enabled)
        assertTrue(repo.apps().isEmpty())
    }
}
