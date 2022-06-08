package tools

import kotlinx.coroutines.*
import mu.KLoggable

/**
 * Schedule or Make a background job using Coroutines from Kotlin.
 *
 * @author A. S. Choe
 * @since 1.0.0
 */
object CoroutineManager : KLoggable {
    override val logger = logger()

    /**
     * Schedule the Job.
     * After delay time has been passed, Runnable will execute.
     *
     * @param r Runnable (Job to do)
     * @param delay Delay time
     * @return Coroutine Job
     */
    fun schedule(r: Runnable, delay: Long): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            delay(delay)
            r.run()
        }
    }

    /**
     * Schedule the Job using time stamp.
     * After given timestamp has been passed, Runnable will execute.
     *
     * @param r Runnable (Job to do)
     * @param delay Timestamp
     * @return Coroutine Job
     */
    fun scheduleAtTimestamp(r: Runnable, delay: Long): Job {
        return schedule(r, delay - System.currentTimeMillis())
    }

    /**
     * Register the repeating job to do.
     *
     * @param r Runnable (Job to do)
     * @param repeatTime How many times to repeat (0 == Infinite)
     * @param delay Delay time before repeat job
     * @return Coroutine Job
     */
    fun register(r: Runnable, repeatTime: Long, delay: Long): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            val infinite = repeatTime == 0L
            var times = repeatTime
            while(if (infinite) true else times > 0) {
                delay(delay)
                if (!infinite) times--
                try {
                    r.run()
                } catch (e: Exception) {
                    logger.error(e) { "Coroutine caused error." }
                }
            }
        }
    }
}