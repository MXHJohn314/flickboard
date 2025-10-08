package se.nullable.flickboard.build

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlin.reflect.KClass

@JvmInline
@Serializable
value class ListOrSingle<T>(
    @Serializable(with = ListOrSingleSerializer::class)
    val values: List<T>,
)

// We can't use ::class literals for applied types (List<T>),
// and the super call requires the generic to be specified.
@Suppress("UNCHECKED_CAST")
class ListOrSingleSerializer<T>(
    private val elementSerializer: KSerializer<T>,
) : JsonContentPolymorphicSerializer<List<T>>(
    baseClass = List::class as KClass<List<T>>,
) {
    val listSerializer = ListSerializer(elementSerializer)
    val singleElementListSerializer = object : KSerializer<List<T>> {
        override val descriptor: SerialDescriptor = elementSerializer.descriptor

        override fun serialize(
            encoder: Encoder,
            value: List<T>,
        ) {
            listSerializer.serialize(encoder, value)
        }

        override fun deserialize(decoder: Decoder): List<T> {
            return listOf(elementSerializer.deserialize(decoder))
        }
    }

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<List<T>> = when {
        element is JsonArray -> listSerializer
        else -> singleElementListSerializer
    }
}