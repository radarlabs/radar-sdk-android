package io.radar.sdk.snapshots

import java.math.BigInteger
import org.json.JSONArray
import org.json.JSONObject

/**
 * Writes serializer output in the canonical JSON form used by the serialization snapshots.
 *
 * The same rules are implemented in radar-sdk-ios, so the two platforms' snapshots can be
 * compared as text:
 * - Object keys sorted by Unicode code point, 2-space indent, `"key": value`, one array element
 *   per line, `{}` and `[]` for empty containers, trailing newline.
 * - Strings escape only `"`, `\` and control characters (`\n`, `\t`, `\r`, `\b`, `\f`, others as
 *   `\u00xx` with lowercase hex). `/` and non-ASCII are written as is.
 * - Int, Long, Short, Byte and BigInteger are written as integers.
 * - Double and Float are written with Java's `Double.toString` / `Float.toString` (a Float is not
 *   widened to Double, so `0.1f` stays `0.1`), with the exponent marker lowercased (`1.0E-7` becomes
 *   `1.0e-7`). These always contain a decimal point or an exponent, so `42.0` stays `42.0` and an
 *   int-versus-double difference shows. NaN and infinities fail.
 * - Booleans as `true`/`false`, `JSONObject.NULL` as `null`.
 * - Any other type fails, naming the type and the path, rather than guessing an encoding.
 */
internal object CanonicalJson {

    fun write(value: Any?): String {
        val sb = StringBuilder()
        writeValue(sb, value, "$", 0)
        sb.append('\n')
        return sb.toString()
    }

    private fun writeValue(sb: StringBuilder, value: Any?, path: String, depth: Int) {
        when (value) {
            JSONObject.NULL -> sb.append("null")
            is JSONObject -> writeObject(sb, value, path, depth)
            is JSONArray -> writeArray(sb, value, path, depth)
            is String -> writeString(sb, value)
            is Boolean -> sb.append(value.toString())
            is Int, is Long, is Short, is Byte, is BigInteger -> sb.append(value.toString())
            is Double -> writeFloating(sb, value.isNaN() || value.isInfinite(), value.toString(), path)
            is Float -> writeFloating(sb, value.isNaN() || value.isInfinite(), value.toString(), path)
            null -> throw IllegalArgumentException("CanonicalJson: raw null (not JSONObject.NULL) at $path")
            else -> throw IllegalArgumentException("CanonicalJson: no rule for type ${value.javaClass.name} at $path")
        }
    }

    private fun writeFloating(sb: StringBuilder, notFinite: Boolean, text: String, path: String) {
        require(!notFinite) { "CanonicalJson: non-finite number $text at $path" }
        sb.append(text.replace('E', 'e'))
    }

    private fun writeObject(sb: StringBuilder, obj: JSONObject, path: String, depth: Int) {
        val keys = obj.keys().asSequence().toList().sortedWith(CODE_POINT_ORDER)
        if (keys.isEmpty()) {
            sb.append("{}")
            return
        }
        sb.append("{\n")
        keys.forEachIndexed { index, key ->
            indent(sb, depth + 1)
            writeString(sb, key)
            sb.append(": ")
            writeValue(sb, obj.opt(key), "$path.$key", depth + 1)
            if (index < keys.size - 1) sb.append(',')
            sb.append('\n')
        }
        indent(sb, depth)
        sb.append('}')
    }

    private fun writeArray(sb: StringBuilder, arr: JSONArray, path: String, depth: Int) {
        if (arr.length() == 0) {
            sb.append("[]")
            return
        }
        sb.append("[\n")
        for (i in 0 until arr.length()) {
            indent(sb, depth + 1)
            writeValue(sb, arr.opt(i), "$path[$i]", depth + 1)
            if (i < arr.length() - 1) sb.append(',')
            sb.append('\n')
        }
        indent(sb, depth)
        sb.append(']')
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\t' -> sb.append("\\t")
                '\r' -> sb.append("\\r")
                '\b' -> sb.append("\\b")
                '\u000c' -> sb.append("\\f")
                else -> if (c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
            }
        }
        sb.append('"')
    }

    private fun indent(sb: StringBuilder, depth: Int) {
        repeat(depth) { sb.append("  ") }
    }

    /** Orders by Unicode code point (String.compareTo orders by UTF-16 code unit, which differs above U+FFFF). */
    private val CODE_POINT_ORDER = Comparator<String> { a, b ->
        val ia = a.codePoints().iterator()
        val ib = b.codePoints().iterator()
        while (ia.hasNext() && ib.hasNext()) {
            val diff = ia.nextInt().compareTo(ib.nextInt())
            if (diff != 0) return@Comparator diff
        }
        ia.hasNext().compareTo(ib.hasNext())
    }
}
