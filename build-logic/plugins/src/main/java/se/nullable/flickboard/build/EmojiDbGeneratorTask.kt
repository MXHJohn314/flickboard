package se.nullable.flickboard.build

import com.android.build.api.variant.AndroidComponentsExtension
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.extensions.stdlib.capitalized
import java.io.File

// NOTE: When editing codegen, DISABLE LIVE EDIT OR INTELLIJ WILL HANG ON BUILD

abstract class EmojiDbGeneratorTask : DefaultTask() {
    @get:InputFile
    val emojibaseEmojisJson: File =
        project.rootDir.resolve("vendor/emojibase/packages/data/en/data.raw.json")

    @get:InputFile
    val emojibaseShortcodesJson: File =
        project.rootDir.resolve("vendor/emojibase/packages/data/en/shortcodes/emojibase.raw.json")

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun taskAction() {
        val outDir = outputDirectory.get().asFile
        // Delete old build results to avoid stale data
        outDir.deleteRecursively()
        val outRawResDir = outDir.resolve("raw")
        outRawResDir.mkdirs()
        val lenientJson = Json { ignoreUnknownKeys = true }
        val minJson = Json { explicitNulls = false }

        val emojis: List<EmojiInfo> =
            emojibaseEmojisJson.inputStream().use(lenientJson::decodeFromStream)
        val emojisWithSkins = emojis.flatMap { emoji -> listOf(emoji) + emoji.skins }
        val emojisByHexcode = emojisWithSkins.associateBy { it.hexcode }
        val tagsByEmoji = emojisWithSkins.associateWith { it.tags }

        val shortcodes: Map<String, ListOrSingle<String>> =
            emojibaseShortcodesJson.inputStream().use(lenientJson::decodeFromStream)
        val shortcodesByEmoji = shortcodes.map { (hexcode, shortcodes) ->
            val emoji = emojisByHexcode[hexcode] ?: throw Exception("emoji $hexcode not found")
            Pair(emoji, shortcodes.values)
        }

        val trie = EmojiTrie()
        val finalShortcodes = mutableMapOf<String, MutableList<String>>()
        (tagsByEmoji.toList() + shortcodesByEmoji.toList()).forEach { (emoji, shortcodes) ->
            shortcodes.forEach {
                val tag = it.replace("_", " ")
                finalShortcodes.getOrPut(tag, ::mutableListOf) += emoji.emoji
                // Include all substrings, for example: "illy" matches "silly"
                tag.indices.forEach { startIndex ->
                    trie.insert(
                        tag.substring(startIndex),
                        emoji.emoji,
                    )
                }
            }
        }
        trie.root.compress()
        outRawResDir.resolve("emoji_trie.json").outputStream()
            .use { minJson.encodeToStream(trie.root, it) }
        outRawResDir.resolve("emoji_db.json").outputStream()
            .use { minJson.encodeToStream(EmojiDb(emojis), it) }

        // For debugging: the final list of shortcodes used by emoji_trie.json
        if (false) {
            outRawResDir.resolve("emoji_shortcodes.json").outputStream()
                .use { minJson.encodeToStream(finalShortcodes, it) }
        }
    }
}

@Serializable
data class EmojiInfo(
    val emoji: String,
    val hexcode: String,
    val label: String,
    val tags: List<String> = emptyList(),
    val skins: List<EmojiInfo> = emptyList(),
)

/**
 * Must match [se.nullable.flickboard.model.emoji.EmojiDb]
 */
@Serializable
data class EmojiDb(val entries: Map<String, Metadata>) {
    @Serializable
    data class Metadata(
        @SerialName("n") val name: String,
        val skinTones: Boolean = false
    )

    constructor(emojis: List<EmojiInfo>) : this(
        entries = emojis.associate { emoji ->
            emoji.emoji to Metadata(
                name = emoji.label,
                // FIXME: use the specific skin data instead
                skinTones = emoji.skins.isNotEmpty(),
            )
        },
    )
}

class EmojiTrie {
    val root: Node = Node()

    /**
     * Mutable counterpart to [se.nullable.flickboard.model.emoji.EmojiTrie.Node], see it for details
     * Semantics and serialization must match it
     */
    @Serializable
    class Node(
        @SerialName("k") var prefix: String? = null,
        @SerialName("c") var children: MutableMap<Char, Node> = mutableMapOf(),
        @SerialName("v") var values: MutableList<String> = mutableListOf(),
    ) {
        /**
         * Compresses nodes that only contain a single child into itself to reduce the amount of nesting
         * for unique substrings.
         */
        fun compress() {
            children.values.forEach { node -> node.compress() }
            if (values.isNotEmpty()) {
                return
            }
            children.asIterable().singleOrNull()?.let { singleChild ->
                children = singleChild.value.children
                values = singleChild.value.values
                prefix = singleChild.key + (singleChild.value.prefix ?: "")
            }
        }
    }

    fun insert(path: String, value: String) {
        val node = path.fold(root) { node, chr ->
            node.children.getOrPut(chr) { Node() }
        }
        if (!node.values.contains(value)) {
            node.values.add(value)
        }
    }
}

abstract class EmojiDbGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
        androidComponents.onVariants { variant ->
            val task = project.tasks.register(
                "compile${variant.name.capitalized()}EmojiDb",
                EmojiDbGeneratorTask::class.java,
            )
            variant.sources.res?.addGeneratedSourceDirectory(
                task,
                EmojiDbGeneratorTask::outputDirectory,
            )
        }
    }
}