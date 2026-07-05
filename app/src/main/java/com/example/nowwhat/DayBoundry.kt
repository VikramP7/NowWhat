package com.example.nowwhat

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// A NowWhat "day" runs 6am → 5:59am next calendar day,
// so the small hours (0–5) belong to the PREVIOUS logical day.
const val DAY_START_HOUR = 6

// instant  ->  which logical day it belongs to   (the READ direction: grouping, selection)
fun logicalDateOf(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
    val zdt = Instant.ofEpochMilli(timestamp).atZone(zone)
    return if (zdt.hour < DAY_START_HOUR) zdt.toLocalDate().minusDays(1)
    else zdt.toLocalDate()
}

// logical day + hour-of-day (0–23)  ->  the real instant   (the WRITE direction: selectHour)
fun timestampOf(logicalDate: LocalDate, hourOfDay: Int, zone: ZoneId = ZoneId.systemDefault()): Long {
    val calendarDate = if (hourOfDay < DAY_START_HOUR) logicalDate.plusDays(1) else logicalDate
    return calendarDate.atTime(hourOfDay, 0).atZone(zone).toInstant().toEpochMilli()
}

// instant  ->  hour-of-day 0–23 in local time (no logical-day shift nessesary)
fun hourOfDay(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): Int =
    Instant.ofEpochMilli(timestamp).atZone(zone).hour