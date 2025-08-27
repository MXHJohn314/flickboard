package se.nullable.flickboard.model.zalgo

import se.nullable.flickboard.util.LastTypedData

data class ZalgoDatabase(private val substitutions: Map<String, String>) {
    fun findCombiner(previous: String?, next: String): LastTypedData.Combiner? = run {
        if (previous == null) {
            return@run null
        }
        val concated = "$previous$next"
        val substitution = substitutions[concated] ?: return@run null
        LastTypedData.Combiner(
            original = concated,
            combinedReplacement = substitution,
            baseCharLength = previous.length,
        )
    }
}