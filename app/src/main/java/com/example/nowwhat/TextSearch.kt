package com.example.nowwhat

internal fun matchRanges(text: String, query: String): List<IntRange>{
    if (query.isEmpty()) return emptyList()

    var startIndex = 0
    val ranges = mutableListOf<IntRange>()
    do {
        startIndex = text.indexOf(query, startIndex, ignoreCase = true)
        if (startIndex != -1){
            ranges.add(startIndex until startIndex+query.length)
            startIndex += query.length
        }
    }while(startIndex != -1)

    return ranges
}