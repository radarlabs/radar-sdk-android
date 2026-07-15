package io.radar.sdk.matchers

import java.lang.UnsupportedOperationException
import java.util.*
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * Used to provide matchers for some range of object
 */
sealed class RangeMatcher<T>(val from: T, val to: T, val inclusive: Boolean) : BaseMatcher<T>() {

    companion object {
        fun isBetween(from: Date, to: Date, inclusive: Boolean = true): RangeMatcher<Date> = DateRangeMatcher(from, to, inclusive)
        fun isBetween(from: Number, to: Number, inclusive: Boolean = true): RangeMatcher<Number> = NumberMatcher(from, to, inclusive)
        fun isGreaterThan(from: Number): RangeMatcher<Number> = NumberMatcher(from)
        fun isLessThan(to: Number): RangeMatcher<Number> = NumberMatcher(to = to)
    }

    private class NumberMatcher(
        from: Number = Double.MIN_VALUE,
        to: Number = Double.MAX_VALUE,
        inclusive: Boolean = false
    ) : RangeMatcher<Number>(from, to, inclusive) {
        override fun matches(item: Any?): Boolean = when (item) {
            null -> {
                false
            }

            is Number -> {
                if (inclusive) {
                    item >= from && item <= to
                } else {
                    item > from && item < to
                }
            }

            else -> {
                false
            }
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
        override fun matches(item: Any?): Boolean = when (item) {
            null -> {
                false
            }

            is Date -> {
                if (inclusive) {
                    item in from..to
                } else {
                    item > from && item < to
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

    override fun describeTo(description: Description?) {
        description?.appendText("is ${if (inclusive) "inclusively" else "exclusively"} between $from and $to")
    }

    operator fun Number.compareTo(number: Number): Int = when (number) {
        is Double -> this.toDouble().compareTo(number.toDouble())
        is Float -> this.toFloat().compareTo(number.toFloat())
        is Long -> this.toLong().compareTo(number.toLong())
        is Int -> this.toInt().compareTo(number.toInt())
        is Short -> this.toShort().compareTo(number.toShort())
        is Byte -> this.toByte().compareTo(number.toByte())
        else -> throw UnsupportedOperationException("Could not compare $this to $number")
    }
}
