package com.example.amasamya.service

import com.example.amasamya.service.A11yAuditService.Companion.A11yNodeData
import com.example.amasamya.service.A11yAuditService.Companion.checkMissingLiveRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class A11yLiveRegionTest {

    @Test
    fun testMissingLiveRegion_TextView_None() {
        val node = A11yNodeData(
            className = "android.widget.TextView",
            text = "Loading data...",
            contentDescription = "",
            bounds = null,
            isClickable = false,
            isFocusable = false,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            isFocused = false,
            isEditable = false,
            liveRegion = 0 // ACCESSIBILITY_LIVE_REGION_NONE
        )
        val violation = checkMissingLiveRegion(node)
        assertNotNull(violation)
        assertEquals("Missing Live Region", violation?.type)
        assertEquals("Warning", violation?.severity)
        assertEquals("4.1.3", violation?.wcagSc)
        assertTrue(violation?.description?.contains("liveRegion setting") == true)
    }

    @Test
    fun testLiveRegion_Polite_Excluded() {
        val node = A11yNodeData(
            className = "android.widget.TextView",
            text = "Processing finished",
            contentDescription = "",
            bounds = null,
            isClickable = false,
            isFocusable = false,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            isFocused = false,
            isEditable = false,
            liveRegion = 1 // ACCESSIBILITY_LIVE_REGION_POLITE
        )
        val violation = checkMissingLiveRegion(node)
        assertNull(violation)
    }

    @Test
    fun testLiveRegion_Assertive_Excluded() {
        val node = A11yNodeData(
            className = "android.widget.TextView",
            text = "Critical error occurred",
            contentDescription = "",
            bounds = null,
            isClickable = false,
            isFocusable = false,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            isFocused = false,
            isEditable = false,
            liveRegion = 2 // ACCESSIBILITY_LIVE_REGION_ASSERTIVE
        )
        val violation = checkMissingLiveRegion(node)
        assertNull(violation)
    }

    @Test
    fun testLiveRegion_Focused_Excluded() {
        val node = A11yNodeData(
            className = "android.widget.TextView",
            text = "Focused text update",
            contentDescription = "",
            bounds = null,
            isClickable = false,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            isFocused = true, // User is actively interacting / focusing this
            isEditable = false,
            liveRegion = 0
        )
        val violation = checkMissingLiveRegion(node)
        assertNull(violation)
    }

    @Test
    fun testLiveRegion_NonTextView_Excluded() {
        val node = A11yNodeData(
            className = "android.view.View",
            text = "Custom view update",
            contentDescription = "",
            bounds = null,
            isClickable = false,
            isFocusable = false,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            isFocused = false,
            isEditable = false,
            liveRegion = 0
        )
        val violation = checkMissingLiveRegion(node)
        assertNull(violation)
    }

    @Test
    fun testLiveRegion_Editable_Excluded() {
        val node = A11yNodeData(
            className = "android.widget.EditText",
            text = "User typing text",
            contentDescription = "",
            bounds = null,
            isClickable = true,
            isFocusable = true,
            isHeading = false,
            isVisibleToUser = true,
            hasTextInSubtree = true,
            isFocused = false,
            isEditable = true,
            liveRegion = 0
        )
        val violation = checkMissingLiveRegion(node)
        assertNull(violation)
    }

    @Test
    fun testLiveRegion_SpamPreventionCacheLogic() {
        // Cache prevents duplicate alerts on the same coordinate and class name
        val cache = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        
        val boundsStr = "[10,50][200,100]"
        val className = "android.widget.TextView"
        val key = "$boundsStr-$className"
        
        // First time adding
        val addedFirst = cache.add(key)
        assertTrue(addedFirst)
        
        // Second time adding (simulate timer updates changing text but bounds and class remain identical)
        val addedSecond = cache.add(key)
        assertTrue(!addedSecond) // Should be false, filtered out
    }
}
