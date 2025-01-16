package se.nullable.flickboard.util

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

    companion object {
        fun parse(fileContents: String): ZalgoDatabase = ZalgoDatabase(
            substitutions = fileContents.lineSequence()
                // Comments
                .filter { it.isNotEmpty() && !it.startsWith("# ") }
                .associate {
                    val parts = it.split(" -> ")
                    Pair(parts[0], parts[1])
                },
        )
    }
}