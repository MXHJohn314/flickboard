package se.nullable.flickboard.model.emoji

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.nullable.flickboard.util.startsWithOrIsPrefixOf
import se.nullable.flickboard.util.unique

/**
 * A prefix trie for searching for emojis by name.
 */
data class EmojiTrie(private val root: Node) {
    /**
     * Must match [se.nullable.flickboard.build.EmojiTrie.Node]
     */
    @Serializable
    data class Node(
        /**
         * A prefix key that must match *before* the rest of the node ([children]/[values]) should be considered.
         * Hence, a `Node("ab", ...)` should be considered equivalent to
         * `Node(children = mapOf("a" to Node(children = mapOf("b" to Node(...)))))`
         */
        @SerialName("k") val prefix: String = "",
        /**
         * A map of (nextChar, node).
         */
        @SerialName("c") val children: Map<Char, Node> = emptyMap(),
        /**
         * Values that apply to this node.
         */
        @SerialName("v") val values: List<String> = emptyList(),
    )

    /**
     * Find the closest [Node] that starts with [query].
     * (Note: the EmojiTrie builder ([se.nullable.flickboard.build.EmojiDbGeneratorTask] also
     * includes all suffixes of emoji names, to the end effect is to find all emojis that
     * *contain* [query]).
     */
    private fun findNode(query: String): Node? {
        var node = root
        val q = query.iterator()
        while (q.hasNext()) {
            if (!node.prefix.startsWithOrIsPrefixOf(q)) {
                return null
            }
            if (!q.hasNext()) {
                break
            }
            node = node.children[q.nextChar()] ?: return null
        }
        return node
    }

    /**
     * Finds all values that contain [query].
     */
    fun matches(query: String): List<String> {
        val nodeQueue = ArrayDeque(listOf(findNode(query) ?: return emptyList()))
        val matches = mutableListOf<String>()
        while (nodeQueue.isNotEmpty()) {
            val node = nodeQueue.removeFirst()
            nodeQueue.addAll(node.children.values)
            matches.addAll(node.values)
        }
        return matches.unique()
    }
}