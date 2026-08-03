package com.example.amasamya

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Dashboard : NavKey
@Serializable data object Settings : NavKey
@Serializable data object History : NavKey
@Serializable data class ReportDetail(val sessionId: Long) : NavKey
@Serializable data class FocusPath(val sessionId: Long, val screenName: String) : NavKey
