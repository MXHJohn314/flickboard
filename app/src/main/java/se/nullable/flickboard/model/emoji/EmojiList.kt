package se.nullable.flickboard.model.emoji

data class EmojiList(val categories: List<EmojiCategory>)
data class EmojiCategory(val name: String, val iconId: Int, val emojis: List<EmojiGroup>)
data class EmojiGroup(
    val names: List<String>,
    val baseVariant: String,
    val variants: Lazy<List<String>>?,
)
