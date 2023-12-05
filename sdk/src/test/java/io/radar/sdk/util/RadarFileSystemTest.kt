package io.radar.sdk.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk=[Build.VERSION_CODES.P])
class RadarFileSystemTest {
    companion object {
        private val context: Context = ApplicationProvider.getApplicationContext()
        private val radarFileSystem: RadarFileSystem = RadarFileSystem(context)
        private val testPath:String = "Test.txt"
    }

    @Test
    fun testWriteToFile() {
        radarFileSystem.writeData(testPath, "testing text1")
        radarFileSystem.writeData(testPath, "testing text2")
        assertEquals(radarFileSystem.readFileAtPath(testPath), "testing text2")
        radarFileSystem.deleteFileAtPath(testPath)
        assertEquals(radarFileSystem.readFileAtPath(testPath), "")
    }

}