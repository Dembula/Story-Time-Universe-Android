package com.storytime.universe.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Decodes an Int that the API may send as a JSON number, a floating point value, or a String.
 * Mirrors the iOS `decodeFlexibleInt` helper so a single odd row never blanks the UI.
 */
object FlexibleIntSerializer : KSerializer<Int?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int? {
        val json = decoder as? JsonDecoder ?: return runCatching { decoder.decodeInt() }.getOrNull()
        val element = json.decodeJsonElement()
        if (element is JsonNull) return null
        val prim = element as? JsonPrimitive ?: return null
        return prim.intOrNull
            ?: prim.doubleOrNull?.toInt()
            ?: prim.contentOrNull?.toDoubleOrNull()?.toInt()
    }

    override fun serialize(encoder: Encoder, value: Int?) {
        if (value == null) encoder.encodeNull() else encoder.encodeInt(value)
    }
}

/**
 * Decodes a String that the API may send as a plain String or an array of Strings (e.g. tags),
 * joining arrays with ", ". Mirrors the iOS `decodeFlexibleString` helper.
 */
object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val json = decoder as? JsonDecoder ?: return runCatching { decoder.decodeString() }.getOrNull()
        val element = json.decodeJsonElement()
        if (element is JsonNull) return null
        return when (element) {
            is JsonPrimitive -> element.contentOrNull
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .joinToString(", ")
                .ifEmpty { null }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull() else encoder.encodeString(value)
    }
}
