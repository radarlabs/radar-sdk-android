package io.radar.sdk.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk=[Build.VERSION_CODES.P])
class RadarFileStorageTest {
    companion object {
        private val context: Context = ApplicationProvider.getApplicationContext()
        private val radarFileStorage: RadarFileStorage = RadarFileStorage(context)
        private val testPath:String = "Test.txt"
    }

    @Test
    fun testWriteToFile() {
        radarFileStorage.writeData("test1", testPath, "testing text1")
        radarFileStorage.writeData("test1", testPath, "testing text2")
        assertEquals(radarFileStorage.readFileAtPath("test1", testPath), "testing text2")
        radarFileStorage.deleteFileAtPath("test1", testPath)
        assertEquals(radarFileStorage.readFileAtPath("test1", testPath), "")

        radarFileStorage.writeData( filename = testPath, content = "testing text1")
        radarFileStorage.writeData( filename =  testPath, content = "testing text3")
        assertEquals(radarFileStorage.readFileAtPath( filePath =  testPath), "testing text3")
        radarFileStorage.deleteFileAtPath(filePath =  testPath)
        assertEquals(radarFileStorage.readFileAtPath(filePath =  testPath), "")


    }

    @Test
    fun testAllFilesInDirectory(){
        radarFileStorage.writeData("testDir", "578", "testing text1")
        radarFileStorage.writeData("testDir", "456", "testing text2")
        val comparator = Comparator<File> { file1, file2 ->
            val number1 = file1.name.toLongOrNull() ?: 0L
            val number2 = file2.name.toLongOrNull() ?: 0L
            number1.compareTo(number2)
        }
        val files = radarFileStorage.allFilesInDirectory("testDir", comparator)
        assertNotNull(files)
        assertEquals(files?.size, 2)
        assertEquals(files?.get(0)?.name ?: "" , "456")

    }

}