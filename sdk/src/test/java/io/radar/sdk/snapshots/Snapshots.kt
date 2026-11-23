package io.radar.sdk.snapshots

import java.io.File
import org.json.JSONObject
import org.junit.Assert.fail

/**
 * Compares serializer output to checked-in snapshots under
 * `sdk/src/test/resources/serialized/`.
 *
 * To update snapshots, run the tests with the record flag:
 *
 *     ./gradlew :sdk:testDebugUnitTest --tests 'io.radar.sdk.snapshots.*' -Pradar.recordSnapshots=true
 *
 * In record mode, snapshots are written instead of compared, and the tests pass. Review the diff
 * in git: a changed snapshot means the cross-platform wrappers (Flutter, React Native, Capacitor)
 * may need a matching change. CI never sets the flag.
 */
internal object Snapshots {

    /** Set by the Gradle test task from `-Pradar.recordSnapshots=true`. */
    private val record: Boolean
        get() = System.getProperty("radar.recordSnapshots")?.toBoolean() == true

    /** `sdk/src/test/resources`, set by the Gradle test task. Falls back to the module directory. */
    private val resourcesDir: File
        get() = File(System.getProperty("radar.testResourcesDir") ?: "src/test/resources")

    /** Reads an input fixture from the test classpath, such as `geocode.json` or `full/Address.json`. */
    fun fixture(path: String): JSONObject {
        val stream = Snapshots::class.java.getResourceAsStream("/$path")
            ?: throw AssertionError("Missing fixture $path")
        return JSONObject(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    /**
     * Canonicalizes [value] and compares it to `serialized/<name>.json`, where [name] is
     * `<fixture>/<path>`, such as `geocode/addresses.0`.
     */
    fun assertMatches(name: String, value: Any) {
        val actual = CanonicalJson.write(value)
        val file = File(resourcesDir, "serialized/$name.json")
        if (record) {
            file.parentFile?.mkdirs()
            file.writeText(actual, Charsets.UTF_8)
            println("Recorded snapshot ${file.path}")
            return
        }
        if (!file.exists()) {
            fail(
                "Missing snapshot ${file.path}. Record it with -Pradar.recordSnapshots=true.\n" +
                    "Actual output:\n$actual"
            )
        }
        val expected = file.readText(Charsets.UTF_8)
        if (expected != actual) {
            fail(
                "Serialized output for $name doesn't match its snapshot (${file.path}).\n" +
                    "If the change is intended, re-record with -Pradar.recordSnapshots=true, and " +
                    "check whether the cross-platform wrappers need a matching change.\n" +
                    "--- snapshot\n+++ actual\n" + lineDiff(expected, actual)
            )
        }
    }

    /** A minimal LCS line diff: unchanged lines prefixed with two spaces, others with `- ` or `+ `. */
    internal fun lineDiff(expected: String, actual: String): String {
        val a = expected.removeSuffix("\n").split('\n')
        val b = actual.removeSuffix("\n").split('\n')
        val lcs = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) {
            for (j in b.indices.reversed()) {
                lcs[i][j] = if (a[i] == b[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
            }
        }
        val out = StringBuilder()
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            if (i < a.size && j < b.size && a[i] == b[j]) {
                out.append("  ").append(a[i]).append('\n')
                i++
                j++
            } else if (i < a.size && (j == b.size || lcs[i + 1][j] >= lcs[i][j + 1])) {
                out.append("- ").append(a[i]).append('\n')
                i++
            } else {
                out.append("+ ").append(b[j]).append('\n')
                j++
            }
        }
        return out.toString()
    }
}
