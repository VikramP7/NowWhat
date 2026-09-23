package com.example.nowwhat

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.floor
import kotlin.math.roundToInt

// A NowWhat "day" runs dayStartHour → dayStartHour-1min next calendar day,
// so typically the small hours belong to the PREVIOUS logical day.

const val HOUR_MS = 3_600_000L

// instant  ->  which logical day it belongs to (the READ direction: grouping, selection)
fun logicalDateOf(timestamp: Long, dayStartHour: Int, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
    val zdt = Instant.ofEpochMilli(timestamp).atZone(zone)
    return if (zdt.hour < dayStartHour) zdt.toLocalDate().minusDays(1)
    else zdt.toLocalDate()
}

// logical day + hour-of-day (0–23)  ->  the real instant (the WRITE direction: selectHour)
fun timestampOf(logicalDate: LocalDate, hourOfDay: Int, dayStartHour: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val calendarDate = if (hourOfDay < dayStartHour) logicalDate.plusDays(1) else logicalDate
    return calendarDate.atTime(hourOfDay, 0).atZone(zone).toInstant().toEpochMilli()
}

fun partOfLogicalDay(dayOfWeek: Int, hourOfDay: Int, selectedDay:Int, dayStartHour: Int): Boolean{
    return if (hourOfDay >= dayStartHour){
        dayOfWeek == selectedDay
    }else{
        dayOfWeek == ((selectedDay%7)+1)
    }
}

// which real weekday a slot belongs to, given the logical day it's displayed under
fun scheduleWeekdayFor(hourOfDay: Int, selectedDay: Int, dayStartHour: Int): Int =
    if (hourOfDay >= dayStartHour) selectedDay else (selectedDay % 7) + 1

// instant  ->  hour-of-day 0–23 in local time (no logical-day shift nessesary)
fun hourOfDay(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(timestamp).atZone(zone).hour

fun dayOfWeek(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(timestamp).atZone(zone).dayOfWeek.value

val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE · MMM d")
fun formatHourLabel(hour: Int, is24Hour: Boolean): String {
    return formatClockLabel(hour*60, is24Hour)
}

fun formatClockLabel(minuteOfDay: Int, is24Hour: Boolean): String{
    val hour24Wrap = wrapRange(minuteOfDay/60)
    val minute60Wrap = wrapRange(minuteOfDay, 59)
    return if (is24Hour) {
        "%02d:%02d".format(hour24Wrap, minute60Wrap)
    } else {
        val period = if (hour24Wrap < 12) "am" else "pm"
        val h12 = when (hour24Wrap % 12) {
            0 -> 12          // 0 and 12 both map to 12 on a 12h clock
            else -> hour24Wrap % 12
        }
        val m12 = if(minute60Wrap == 0) "" else ":%02d".format(minute60Wrap)
        "$h12$m12$period"
    }
}

fun formatDurationLabel(hours:Float, roundTo:Int = 5): String{
    val totalMinutes = ((hours*60)/roundTo).roundToInt()*roundTo
    val truncHours:Int = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${truncHours}h ${minutes}m"
}

fun withinHourSpan(timestamp: Long, startHour: Int, endHour:Int): Boolean{
    val curHour = hourOfDay(timestamp)
    return if(startHour > endHour){
        // crosses midnight
        (curHour>=startHour) || (curHour<endHour)
    } else{
        // doesn't cross midnight
        (curHour>=startHour) && (curHour<endHour)
    }
}

fun truncateToHour(timestamp: Long): Long = (timestamp / HOUR_MS) * HOUR_MS

fun wrapRange(hours: Int, top:Int = 23): Int {
    val modTop = top+1
    return ((hours%modTop)+modTop)%modTop
}