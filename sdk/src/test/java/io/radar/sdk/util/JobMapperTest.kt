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

    private lateinit var jobMapper: JobMapper

    @Before
    fun setUp() {
        jobMapper = JobMapper()
    }

    @Test
    fun testGetJobId() {
        // Precondition
        assertEquals(JobMapper.DEFAULT_JOB_ID, jobMapper.getJobId(0))
        val n = 10
        // Initialize the size
        jobMapper.getJobId(n)

        // Increment the initial job
        jobMapper.incAndGet(JobMapper.DEFAULT_JOB_ID)

        // Shows thread-safety
        val latch = CountDownLatch(n)
        for (i in 0 until n) {
            Thread().run {
                jobMapper.incAndGet(JobMapper.DEFAULT_JOB_ID + i)
                n.dec()
            }
        }
        latch.await(10, TimeUnit.SECONDS)

        // This shows that the first Job ID has one additional count, so it is the one that will be overridden
        assertEquals(2, jobMapper.get(JobMapper.DEFAULT_JOB_ID))
        for (i in 1 until n) {
            assertEquals(1, jobMapper.get(JobMapper.DEFAULT_JOB_ID + i))
        }

        // Shows the override behavior
        assertEquals(JobMapper.DEFAULT_JOB_ID, jobMapper.getJobId(n))
    }

    @Test
    fun testAdjustSize() {
        assertEquals(0, jobMapper.size)
        jobMapper.getJobId(10)
        assertEquals(10, jobMapper.size)
        jobMapper.getJobId(5)
        assertEquals(5, jobMapper.size)
    }

    @Test
    fun testGet() {
        val n = Random.nextInt(10)
        val jobId = jobMapper.getJobId(n)
        assertEquals(0, jobMapper.get(jobId))
    }

    @Test
    fun testIncAndGet() {
        val n = Random.nextInt(10)
        val jobId = jobMapper.getJobId(n)
        assertEquals(1, jobMapper.incAndGet(jobId))
        assertEquals(2, jobMapper.incAndGet(jobId))
    }

    @Test
    fun testClear() {
        val n = Random.nextInt(10) + 2
        val jobId = jobMapper.getJobId(n)
        assertEquals(n, jobMapper.size)
        for (i in 0 until n) {
            jobMapper.incAndGet(jobId)
        }
        assertEquals(n + 1, jobMapper.incAndGet(jobId))
        jobMapper.clear(jobId)
        assertEquals(0, jobMapper.get(jobId))

    }
}
