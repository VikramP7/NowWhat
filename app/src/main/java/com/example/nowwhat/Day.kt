package com.example.nowwhat

import java.time.LocalDate

data class Day(
    val hourRows: List<List<HourSlot?>>,
    val date: String,
    val localDate: LocalDate
)