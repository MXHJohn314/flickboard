package se.nullable.flickboard.model.zalgo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ZalgoManager(
    private val defaultSubstitutions: List<ZalgoSubstitution>,
    val substitutionDao: ZalgoSubstitutionDao,
) {
    private val defaultSubstitutionMap: Map<String, String> =
        defaultSubstitutions.toZalgoMap()

    val substitutions: Flow<List<ZalgoSubstitution>> =
        substitutionDao.allCustom().map { customSubstitutions ->
            val customFroms = customSubstitutions.asSequence().map { it.from }.toSet()
            customSubstitutions + (defaultSubstitutions.filterNot { customFroms.contains(it.from) })
        }

    val db: Flow<ZalgoDatabase> = substitutionDao.allCustom().map {
        ZalgoDatabase(defaultSubstitutionMap + it.toZalgoMap())
    }

    companion object {
        fun parseDefaults(fileContents: String): List<ZalgoSubstitution> =
            fileContents.lineSequence()
                // Comments
                .filter { it.isNotEmpty() && !it.startsWith("# ") }
                .map {
                    val parts = it.split(" -> ")
                    ZalgoSubstitution(parts[0], parts[1])
                }
                .toList()
    }
}

private fun List<ZalgoSubstitution>.toZalgoMap(): Map<String, String> =
    associate { it.from to it.to }