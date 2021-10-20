package io.radar.sdk.matchers

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import java.util.*

/**
 * Used to provide matchers for some range of object
 */
sealed class RangeMatcher<T>(val from: T, val to: T, val inclusive: Boolean) : BaseMatcher<T>() {

    companion object {
        fun isBetween(from: Date, to: Date, inclusive: Boolean = true): RangeMatcher<Date> {
            return DateRangeMatcher(from, to, inclusive)
        }
    }

    /**
     * Matcher for checking if a given [Date] is within the bounds of other dates
     */
    private class DateRangeMatcher(
        from: Date,
        to: Date,
        inclusive: Boolean
    ) : RangeMatcher<Date>(from, to, inclusive) {
        override fun matches(item: Any?): Boolean {
            return when (item) {
                null -> {
                    false
                }
                is Date -> {
                    if (inclusive) {
                        item.time >= from.time && item.time <= to.time
                    } else {
                        item.time > from.time && item.time < to.time
                    }
                }
                is Long -> {
                    if (inclusive) {
                        item >= from.time && item <= to.time
                    } else {
                        item > from.time && item < to.time
                    }
                }
                else -> {
                    false
                }
            }
        }
    }

    override fun describeTo(description: Description?) {
        description?.appendText("is ${if (inclusive) "inclusively" else "exclusively"} between $from and $to")
    }
}