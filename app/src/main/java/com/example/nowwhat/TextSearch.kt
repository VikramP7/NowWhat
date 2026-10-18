package com.example.nowwhat

private const val PREVIEW_LEAD_CHARS = 30
private const val PREVIEW_TRAILING_CHARS = 8

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

internal fun searchPreview(text: String, query: String): String{
    val ranges = matchRanges(text, query)
    if (ranges.isEmpty()) return text

    var previewStr = ""
    var lastEnd = 0
    ranges.forEach { range ->
        val previewEnd = roundToWordStart(text, minOf(range.last + 1 + PREVIEW_TRAILING_CHARS, text.length))
        if (previewEnd <= lastEnd) return@forEach          // nothing new to show
        val previewStart = if (range.first < lastEnd) lastEnd  // starts in the last segment: carry on from there
        else minOf(roundToWordStart(text, maxOf(range.first - PREVIEW_LEAD_CHARS, lastEnd)), range.first)
        if (previewStart > lastEnd) previewStr += "…"
        previewStr += text.substring(previewStart, previewEnd)
        lastEnd = previewEnd
    }
    previewStr += text.substring(lastEnd)
    return previewStr
}

private fun roundToWordStart(text: String, index: Int):Int{
    // early return if we are at the start of a word already
    if (index == 0 || index >= text.length || text[index - 1].isWhitespace()) return index

    var i = index
    // if reading non-white space, and early return didn't fire then we are in the middle of a word
    // Move till we reach the end of the word
    while (i < text.length && !text[i].isWhitespace()) i++
    // now at the white space at the end of the word we landed in the middle of move to the start of next word
    while (i < text.length && text[i].isWhitespace()) i++

    return i
}