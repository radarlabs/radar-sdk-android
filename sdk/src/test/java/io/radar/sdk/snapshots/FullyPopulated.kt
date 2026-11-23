package io.radar.sdk.snapshots

import java.util.IdentityHashMap
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.fail

/**
 * Fails unless every property of [instance] has a value, so a full fixture really sets every
 * field and a field added to `toJson()` shows up in the full snapshot.
 *
 * Walks Kotlin `memberProperties` (including internal and private ones), recursing into
 * `io.radar.sdk` model classes and into list and array elements. A property fails when it is:
 * - null,
 * - an empty string (`optString` turns a missing string into `""`, so null alone can't catch a
 *   field missing from the fixture),
 * - an empty array, collection, `JSONArray` or `JSONObject`.
 *
 * [allowedNull] lists property paths that may be unset, such as `RadarAddress.resultId`. Paths
 * are `<ClassSimpleName>.<property>`. Each entry should carry a comment at the call site saying why.
 *
 * Pilot scope: written just well enough for RadarAddress (and RadarCoordinate, RadarTimeZone).
 * Number fields that default to 0 when missing (`optInt`, `optDouble`) can't be told apart from a
 * real 0 and aren't checked.
 */
internal fun assertFullyPopulated(instance: Any, allowedNull: Set<String> = emptySet()) {
    val problems = mutableListOf<String>()
    val seen = IdentityHashMap<Any, Unit>()
    checkModel(instance, instance.javaClass.simpleName, allowedNull, problems, seen)
    if (problems.isNotEmpty()) {
        fail("${instance.javaClass.simpleName} isn't fully populated:\n" + problems.joinToString("\n") { "  $it" })
    }
}

private fun isRadarModel(value: Any): Boolean = value.javaClass.name.startsWith("io.radar.sdk.") && !value.javaClass.isEnum

private fun checkModel(
    model: Any,
    path: String,
    allowedNull: Set<String>,
    problems: MutableList<String>,
    seen: IdentityHashMap<Any, Unit>
) {
    if (seen.put(model, Unit) != null) return
    val className = model.javaClass.simpleName
    for (property in model::class.memberProperties) {
        property.isAccessible = true
        val key = "$className.${property.name}"
        val value = property.getter.call(model)
        checkValue(value, "$path.${property.name}", key in allowedNull, allowedNull, problems, seen)
    }
}

private fun checkValue(
    value: Any?,
    path: String,
    allowed: Boolean,
    allowedNull: Set<String>,
    problems: MutableList<String>,
    seen: IdentityHashMap<Any, Unit>
) {
    val empty = when (value) {
        null -> "null"
        is String -> if (value.isEmpty()) "empty string" else null
        is Array<*> -> if (value.isEmpty()) "empty array" else null
        is Collection<*> -> if (value.isEmpty()) "empty list" else null
        is JSONArray -> if (value.length() == 0) "empty JSONArray" else null
        is JSONObject -> if (value.length() == 0) "empty JSONObject" else null
        else -> null
    }
    if (empty != null) {
        if (!allowed) problems.add("$path is $empty")
        return
    }
    when (value) {
        is Array<*> -> value.forEachIndexed { i, element ->
            checkValue(element, "$path[$i]", false, allowedNull, problems, seen)
        }

        is Collection<*> -> value.forEachIndexed { i, element ->
            checkValue(element, "$path[$i]", false, allowedNull, problems, seen)
        }

        else -> if (value != null && isRadarModel(value)) {
            checkModel(value, path, allowedNull, problems, seen)
        }
    }
}
