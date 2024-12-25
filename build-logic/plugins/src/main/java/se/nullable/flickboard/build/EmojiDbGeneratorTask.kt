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

abstract class EmojiDbGeneratorTask : DefaultTask() {
    @get:InputFile
    val gemojiEmojiJson: File = project.rootDir.resolve("vendor/gemoji/db/emoji.json")

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun taskAction() {
        val outDir = outputDirectory.get().asFile
        // Delete old build results to avoid stale data
        // Disabled because it seems to cause IntelliJ to hang sometimes when the folder is visible...
//        outDir.deleteRecursively()
        val outRawResDir = outDir.resolve("raw")
        outRawResDir.mkdirs()
        val lenientJson = Json { ignoreUnknownKeys = true }
        val minJson = Json { explicitNulls = false }
        val emojis: List<EmojiInfo> =
            gemojiEmojiJson.inputStream().use(lenientJson::decodeFromStream)
        val trie = EmojiTrie()
        emojis.forEach { emoji ->
            (emoji.tags + emoji.aliases).forEach {
                val tag = it.replace("_", " ")
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
    }
}

@Serializable
data class EmojiInfo(val emoji: String, val aliases: List<String>, val tags: List<String>)

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
        node.values.add(value)
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