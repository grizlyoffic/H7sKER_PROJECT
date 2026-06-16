package com.nexbytes.h7skertool.utils

import android.util.Log

object ProtoModifier {
    private const val TAG = "ProtoModifier"

    fun encodeVarint(value: Long): ByteArray {
        val result = mutableListOf<Byte>()
        var v = value
        while (v > 127) { result.add(((v and 0x7F) or 0x80).toByte()); v = v ushr 7 }
        result.add((v and 0x7F).toByte())
        return result.toByteArray()
    }

    fun readVarint(data: ByteArray, offset: Int): Pair<Long, Int> {
        var value = 0L; var shift = 0; var i = offset
        while (i < data.size) {
            val b = data[i].toLong() and 0xFF; i++
            value = value or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0L) break
            shift += 7
        }
        return Pair(value, i)
    }

    fun modifyProtoBytes(data: ByteArray, modFields: Map<Int, String>): ByteArray {
        if (data.isEmpty() || modFields.isEmpty()) return data
        return try {
            val output = mutableListOf<Byte>(); var i = 0
            while (i < data.size) {
                val tagResult = readVarint(data, i); val tag = tagResult.first.toInt(); val afterTag = tagResult.second
                val fieldNumber = tag ushr 3; val wireType = tag and 0x07
                val tagBytes = encodeVarint(tag.toLong()); val modValue = modFields[fieldNumber]
                when (wireType) {
                    0 -> { val (origValue, afterVal) = readVarint(data, afterTag)
                        val newVal = if (modValue != null) modValue.trim().toLongOrNull() ?: origValue else origValue
                        output.addAll(tagBytes.toList()); output.addAll(encodeVarint(newVal).toList()); i = afterVal }
                    1 -> { if (afterTag + 8 <= data.size) {
                        output.addAll(tagBytes.toList())
                        if (modValue != null) { val nv = modValue.trim().toLongOrNull(); val bytes = ByteArray(8)
                            if (nv != null) for (b in 0..7) bytes[b] = (nv ushr (b * 8)).toByte() else data.copyInto(bytes, 0, afterTag, afterTag + 8)
                            output.addAll(bytes.toList()) } else output.addAll(data.slice(afterTag until afterTag + 8))
                        i = afterTag + 8 } else { output.addAll(data.slice(i until data.size)); break } }
                    2 -> { val (length, afterLen) = readVarint(data, afterTag); val len = length.toInt(); val dataEnd = afterLen + len
                        if (dataEnd <= data.size) {
                            output.addAll(tagBytes.toList())
                            if (modValue != null) { val nb = modValue.toByteArray(Charsets.UTF_8); output.addAll(encodeVarint(nb.size.toLong()).toList()); output.addAll(nb.toList()) }
                            else { output.addAll(encodeVarint(length).toList()); output.addAll(data.slice(afterLen until dataEnd)) }
                            i = dataEnd } else { output.addAll(data.slice(i until data.size)); break } }
                    5 -> { if (afterTag + 4 <= data.size) { output.addAll(tagBytes.toList()); output.addAll(data.slice(afterTag until afterTag + 4)); i = afterTag + 4 }
                        else { output.addAll(data.slice(i until data.size)); break } }
                    else -> { output.addAll(data.slice(i until data.size)); break }
                }
            }
            output.toByteArray()
        } catch (e: Exception) { Log.e(TAG, "Proto modification failed: ${e.message}"); data }
    }

    fun parseModFields(modJson: String): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        try {
            val gson = com.google.gson.Gson()
            @Suppress("UNCHECKED_CAST")
            val map = gson.fromJson(modJson, Map::class.java) as? Map<String, Any> ?: return result
            for ((k, v) in map) { val fn = k.trim().toIntOrNull() ?: continue; result[fn] = v.toString() }
        } catch (e: Exception) { Log.e(TAG, "parseModFields error: ${e.message}") }
        return result
    }
}
