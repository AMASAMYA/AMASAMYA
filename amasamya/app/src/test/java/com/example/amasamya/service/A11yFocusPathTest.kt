package com.example.amasamya.service

import com.example.amasamya.db.FocusPathNode
import com.example.amasamya.utils.ReportExporter
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class A11yFocusPathTest {

    @Test
    fun testGenerateFocusPathSvg_SingleNode() {
        val node = FocusPathNode(
            id = 1L,
            sessionId = 123L,
            screenName = "MainActivity",
            className = "android.widget.Button",
            bounds = "[50,100][250,150]",
            text = "Click Me",
            contentDescription = "",
            focusOrder = 1
        )
        
        val svg = ReportExporter.generateFocusPathSvg(listOf(node))
        assertNotNull(svg)
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("viewBox=\"0 50 300 150\""))
        assertTrue(svg.contains("<rect x=\"50\" y=\"100\" width=\"200\" height=\"50\""))
        assertTrue(svg.contains("android.widget.Button"))
        assertTrue(svg.contains("Click Me"))
        assertTrue(svg.contains("circle cx=\"65\" cy=\"115\""))
    }

    @Test
    fun testGenerateFocusPathSvg_MultipleNodes() {
        val node1 = FocusPathNode(
            id = 1L,
            sessionId = 123L,
            screenName = "MainActivity",
            className = "android.widget.Button",
            bounds = "[50,100][250,150]",
            text = "First",
            contentDescription = "",
            focusOrder = 1
        )
        val node2 = FocusPathNode(
            id = 2L,
            sessionId = 123L,
            screenName = "MainActivity",
            className = "android.widget.TextView",
            bounds = "[100,200][400,250]",
            text = "Second",
            contentDescription = "",
            focusOrder = 2
        )

        val svg = ReportExporter.generateFocusPathSvg(listOf(node1, node2))
        assertNotNull(svg)
        
        // Check rectangles are drawn
        assertTrue(svg.contains("<rect x=\"50\" y=\"100\" width=\"200\" height=\"50\""))
        assertTrue(svg.contains("<rect x=\"100\" y=\"200\" width=\"300\" height=\"50\""))
        
        // Check connecting flow line is drawn between centers:
        // cxA = 50 + 200/2 = 150
        // cyA = 100 + 50/2 = 125
        // cxB = 100 + 300/2 = 250
        // cyB = 200 + 50/2 = 225
        assertTrue(svg.contains("x1=\"150\" y1=\"125\" x2=\"250\" y2=\"225\""))
    }
    
    @Test
    fun testGenerateFocusPathSvg_EmptyNodesFallback() {
        val svg = ReportExporter.generateFocusPathSvg(emptyList())
        assertNotNull(svg)
        assertTrue(svg.contains("viewBox=\"0 0 1080 2400\""))
    }
}
