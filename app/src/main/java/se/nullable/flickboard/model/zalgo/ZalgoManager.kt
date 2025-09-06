package se.nullable.flickboard.model.zalgo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ZalgoManager(
    val defaultSubstitutions: List<ZalgoSubstitution>,
    val substitutionDao: ZalgoSubstitutionDao,
) {
    private val defaultSubstitutionMap: Map<String, String> =
        defaultSubstitutions.toZalgoMap()

    val customSubstitutions: Flow<List<ZalgoSubstitution>> =
        substitutionDao.allCustom()

    val db: Flow<ZalgoDatabase> = substitutionDao.allCustomUnordered().map {
        ZalgoDatabase(defaultSubstitutionMap + it.toZalgoMap())
    }

    companion object {
        fun parseDefaults(fileContents: String): List<ZalgoSubstitution> =
            fileContents.lineSequence()
                // Comments
                .filter { it.isNotEmpty() && !it.startsWith("# ") }
                .map {
                    val parts = it.split(" -> ")
                    ZalgoSubstitution(parts[0], parts[1], UUID.randomUUID(), isDefault = true)
                }
                .toList()
    }
}

private fun List<ZalgoSubstitution>.toZalgoMap(): Map<String, String> =
    associate { it.from to it.to }