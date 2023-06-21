package io.radar.sdk.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(JUnit4::class)
class JobMapperTest {
    @Test
    fun testGetJobId() {
        // Precondition
        assertEquals(JobMapper.DEFAULT_JOB_ID, JobMapper.getJobId(0))
        val n = 10
        // Initialize the size
        JobMapper.getJobId(n)

        // Increment the initial job
        JobMapper.incAndGet(JobMapper.DEFAULT_JOB_ID)

        // Shows thread-safety
        val latch = CountDownLatch(n)
        for (i in 0 until n) {
            Thread().run {
                JobMapper.incAndGet(JobMapper.DEFAULT_JOB_ID + i)
                n.dec()
            }
        }
        latch.await(10, TimeUnit.SECONDS)

        // This shows that the first Job ID has one additional count, so it is the one that will be overridden
        assertEquals(2, JobMapper.get(JobMapper.DEFAULT_JOB_ID))
        for (i in 1 until n) {
            assertEquals(1, JobMapper.get(JobMapper.DEFAULT_JOB_ID + i))
        }

        // Shows the override behavior
        assertEquals(JobMapper.DEFAULT_JOB_ID, JobMapper.getJobId(n))
    }

    @Test
    fun testAdjustSize() {
        JobMapper.adjustSize(0)
        assertEquals(0, JobMapper.size)
        for (i in 0..10) {
            JobMapper.incAndGet(JobMapper.getJobId(i))
        }
        assertEquals(10, JobMapper.size)
        JobMapper.getJobId(5)
        assertEquals(5, JobMapper.size)
    }

    @Test
    fun testGet() {
        val n = Random.nextInt(10)
        val jobId = JobMapper.getJobId(n)
        assertEquals(0, JobMapper.get(jobId))
    }

    @Test
    fun testIncAndGet() {
        val n = Random.nextInt(10)
        val jobId = JobMapper.getJobId(n)
        assertEquals(1, JobMapper.incAndGet(jobId))
        assertEquals(2, JobMapper.incAndGet(jobId))
    }

    @Test
    fun testClear() {
        val n = Random.nextInt(10) + 2
        val jobId = JobMapper.getJobId(n)
        for (i in 0 until n) {
            JobMapper.incAndGet(jobId)
        }
        assertEquals(n + 1, JobMapper.incAndGet(jobId))
        JobMapper.clear(jobId)
        assertEquals(0, JobMapper.get(jobId))

    }
}
