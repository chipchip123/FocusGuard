package com.example.focusguard

import org.junit.Assert.*
import org.junit.Test

class FocusPolicyTest {
    @Test fun `demo locks unused app but honors reward and protection switch`() {
        assertTrue(FocusPolicy.blocked(true, 0, 30, 0, 1000, demo = true))
        assertFalse(FocusPolicy.blocked(true, 0, 30, 2000, 1999, demo = true))
        assertTrue(FocusPolicy.blocked(true, 0, 30, 2000, 2000, demo = true))
        assertFalse(FocusPolicy.blocked(false, 0, 30, 0, 1000, demo = true))
        assertFalse(FocusPolicy.blocked(true, 0, 30, 0, 1000, demo = false))
    }
    @Test fun `locks exactly at limit and after reward expires`() {
        assertFalse(FocusPolicy.blocked(true, 299_999, 5, 0, 1000))
        assertTrue(FocusPolicy.blocked(true, 300_000, 5, 0, 1000))
        assertFalse(FocusPolicy.blocked(true, 300_000, 5, 2000, 1999))
        assertTrue(FocusPolicy.blocked(true, 300_000, 5, 2000, 2000))
    }
    @Test fun `disabled protection never locks`() {
        assertFalse(FocusPolicy.blocked(false, Long.MAX_VALUE, 5, 0, 1000))
    }
    @Test fun `reward is capped at next midnight`() {
        assertEquals(301_000L, FocusPolicy.rewardExpiry(1000, 1_000_000))
        assertEquals(2000L, FocusPolicy.rewardExpiry(1000, 2000))
    }
    @Test fun `shake requires falling below reset threshold`() {
        val c = ShakeCounter()
        assertTrue(c.sample(1.4f, 0))
        assertFalse(c.sample(1.4f, 1000))
        assertFalse(c.sample(1.1f, 1100))
        assertFalse(c.sample(1.4f, 1200))
        assertFalse(c.sample(1f, 1250))
        assertTrue(c.sample(1.4f, 1300))
    }
    @Test fun `shake rejects bounces within 250 milliseconds`() {
        val c = ShakeCounter()
        assertTrue(c.sample(1.4f, 0))
        c.sample(1f, 50)
        assertFalse(c.sample(1.4f, 249))
        assertTrue(c.sample(1.4f, 250))
    }
}
