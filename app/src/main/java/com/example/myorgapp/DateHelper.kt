package com.example.myorgapp

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateHelper {
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dateTimeFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayHeaderFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
    private val shortDateFmt = SimpleDateFormat("MMM d", Locale.getDefault())
    private val monthHeaderFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val dayNameFmt = SimpleDateFormat("EEE", Locale.getDefault())

    fun todayDate(): String = dateFmt.format(Date())
    fun todayCal(): Calendar = Calendar.getInstance()
    fun nowCal(): Calendar = Calendar.getInstance()

    fun parseDate(s: String): Calendar = Calendar.getInstance().apply {
        time = dateFmt.parse(s)!!
    }

    fun parseDateTime(s: String): Calendar = Calendar.getInstance().apply {
        time = dateTimeFmt.parse(s)!!
    }

    fun formatDate(cal: Calendar): String = dateFmt.format(cal.time)
    fun formatTime(cal: Calendar): String = timeFmt.format(cal.time)
    fun formatDateTime(cal: Calendar): String = dateTimeFmt.format(cal.time)

    fun getDatePart(s: String): String = s.substringBefore("T")
    fun getTimePart(s: String): String = s.substringAfter("T", "")

    fun getHour(cal: Calendar): Int = cal.get(Calendar.HOUR_OF_DAY)
    fun getMinute(cal: Calendar): Int = cal.get(Calendar.MINUTE)
    fun getHour(s: String): Int = getHour(parseDateTime(s))
    fun getMinute(s: String): Int = getMinute(parseDateTime(s))

    fun getDayOfMonth(s: String): Int = parseDate(s).get(Calendar.DAY_OF_MONTH)

    fun getDayOfWeekMondayBased(s: String): Int {
        val cal = parseDate(s)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return (dow - Calendar.MONDAY + 7) % 7
    }

    fun getDaysInMonth(s: String): Int {
        val cal = parseDate(s)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun getFirstOfMonth(s: String): String {
        val cal = parseDate(s)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return formatDate(cal)
    }

    fun getYearMonth(s: String): String {
        val cal = parseDate(s)
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(y, m)
    }

    fun getDayName(s: String): String {
        val cal = parseDate(s)
        return dayNameFmt.format(cal.time)
    }

    fun atTime(dateStr: String, hour: Int, minute: Int): String {
        val cal = parseDate(dateStr)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        return formatDateTime(cal)
    }

    fun addDays(s: String, days: Int): String {
        val cal = parseDate(s)
        cal.add(Calendar.DAY_OF_MONTH, days)
        return formatDate(cal)
    }

    fun addDaysCal(cal: Calendar, days: Int): Calendar {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, days)
        return c
    }

    fun addWeeks(s: String, weeks: Int): String = addDays(s, weeks * 7)

    fun addMonths(s: String, months: Int): String {
        val cal = parseDate(s)
        cal.add(Calendar.MONTH, months)
        return formatDate(cal)
    }

    fun getStartOfWeek(s: String): String {
        val offset = -getDayOfWeekMondayBased(s)
        return addDays(s, offset)
    }

    fun isSameDate(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

    fun isSameDate(a: String, b: String): Boolean = a == b

    fun daysBetween(from: Calendar, to: Calendar): Long {
        val f = Calendar.getInstance().apply {
            time = from.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val t = Calendar.getInstance().apply {
            time = to.time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (t.timeInMillis - f.timeInMillis) / (24 * 60 * 60 * 1000)
    }

    fun minutesBetween(from: Calendar, to: Calendar): Long =
        (to.timeInMillis - from.timeInMillis) / (60 * 1000)

    fun isSameYearMonth(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.MONTH) == b.get(Calendar.MONTH)

    fun formatDayHeader(s: String): String {
        val cal = parseDate(s)
        return dayHeaderFmt.format(cal.time)
    }

    fun formatDateShort(s: String): String {
        val cal = parseDate(s)
        return shortDateFmt.format(cal.time)
    }

    fun formatMonthHeader(s: String): String {
        val cal = parseDate(s)
        return monthHeaderFmt.format(cal.time)
    }

    fun formatTimeRange(start: String, end: String?): String {
        val st = timeFmt.format(parseDateTime(start).time)
        val et = end?.let {
            try { timeFmt.format(parseDateTime(it).time) } catch (_: Exception) { null }
        }
        return if (et != null) "$st-$et" else st
    }

    fun dateToMillis(s: String): Long {
        val cal = parseDate(s)
        return cal.timeInMillis
    }

    fun millisToDate(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return formatDate(cal)
    }

    fun millisToCal(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    fun isWeekend(cal: Calendar): Boolean {
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
    }

    private fun getNextDayOfWeek(fromCal: Calendar, targetDayOfWeek: Int): Calendar {
        val cal = fromCal.clone() as Calendar
        val currentDow = cal.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDayOfWeek - currentDow
        if (daysToAdd <= 0) {
            daysToAdd += 7
        }
        cal.add(Calendar.DAY_OF_MONTH, daysToAdd)
        return cal
    }

    fun computeNextDate(
        currentDate: String,
        repeatType: RepeatType,
        daysOfWeek: List<Int>?,
        skipDates: List<String>?,
        endDate: String?
    ): String? {
        if (repeatType == RepeatType.NONE) return null

        if (endDate != null && currentDate >= endDate) return null

        val cal = parseDate(currentDate)
        val nextDate: String

        when (repeatType) {
            RepeatType.DAILY -> {
                cal.add(Calendar.DAY_OF_MONTH, 1)
                nextDate = formatDate(cal)
            }
            RepeatType.WEEKDAYS -> {
                do {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                } while (isWeekend(cal))
                nextDate = formatDate(cal)
            }
            RepeatType.WEEKENDS -> {
                do {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                } while (!isWeekend(cal))
                nextDate = formatDate(cal)
            }
            RepeatType.WEEKLY -> {
                cal.add(Calendar.DAY_OF_MONTH, 7)
                nextDate = formatDate(cal)
            }
            RepeatType.MONTHLY -> {
                cal.add(Calendar.MONTH, 1)
                nextDate = formatDate(cal)
            }
            RepeatType.YEARLY -> {
                cal.add(Calendar.YEAR, 1)
                nextDate = formatDate(cal)
            }
            RepeatType.CUSTOM -> {
                if (daysOfWeek.isNullOrEmpty()) return null
                val sortedDays = daysOfWeek.sorted()
                val currentDow = cal.get(Calendar.DAY_OF_WEEK)
                var found: Int? = null
                for (day in sortedDays) {
                    if (day > currentDow) {
                        found = day
                        break
                    }
                }
                if (found != null) {
                    cal.add(Calendar.DAY_OF_MONTH, found - currentDow)
                } else {
                    cal.add(Calendar.DAY_OF_MONTH, (7 - currentDow) + sortedDays.first())
                }
                nextDate = formatDate(cal)
            }
            RepeatType.NONE -> return null
        }

        if (skipDates != null && nextDate in skipDates) {
            return computeNextDate(nextDate, repeatType, daysOfWeek, skipDates, endDate)
        }

        if (endDate != null && nextDate > endDate) return null

        return nextDate
    }

    fun isDateMatchingRepeat(
        date: String,
        repeatType: RepeatType,
        createdDate: String,
        daysOfWeek: List<Int>?,
        endDate: String?,
        skipDates: List<String>?
    ): Boolean {
        if (repeatType == RepeatType.NONE) return false

        if (date < createdDate) return false

        if (endDate != null && date > endDate) return false

        if (skipDates != null && date in skipDates) return false

        val dateCal = parseDate(date)
        val createdCal = parseDate(createdDate)

        return when (repeatType) {
            RepeatType.DAILY -> true
            RepeatType.WEEKDAYS -> {
                val dow = dateCal.get(Calendar.DAY_OF_WEEK)
                dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
            }
            RepeatType.WEEKENDS -> {
                val dow = dateCal.get(Calendar.DAY_OF_WEEK)
                dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            }
            RepeatType.WEEKLY -> {
                val diff = daysBetween(createdCal, dateCal)
                diff >= 0 && diff % 7 == 0L
            }
            RepeatType.MONTHLY -> {
                dateCal.get(Calendar.DAY_OF_MONTH) == createdCal.get(Calendar.DAY_OF_MONTH)
            }
            RepeatType.YEARLY -> {
                dateCal.get(Calendar.MONTH) == createdCal.get(Calendar.MONTH) &&
                dateCal.get(Calendar.DAY_OF_MONTH) == createdCal.get(Calendar.DAY_OF_MONTH)
            }
            RepeatType.CUSTOM -> {
                daysOfWeek?.contains(dateCal.get(Calendar.DAY_OF_WEEK)) == true
            }
            RepeatType.NONE -> false
        }
    }
}
