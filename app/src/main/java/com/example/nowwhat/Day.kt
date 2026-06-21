package com.example.nowwhat

data class Day(
    val hourRows: List<List<HourSlot?>>,
    val date: String
)