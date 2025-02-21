package se.nullable.flickboard.model.credits

import kotlinx.serialization.Serializable

/**
 * Must match [se.nullable.flickboard.build.DependencyInfo]
 */
@Serializable
data class DependencyInfo(
    val name: String,
    val group: String,
    val url: String?,
    val license: String?,
)
