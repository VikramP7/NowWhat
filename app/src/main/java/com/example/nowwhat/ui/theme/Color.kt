package com.example.nowwhat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/* ------------------------------------------------------------------
 * Chrome colours — backing light/dark values
 *
 * These are plain (non-composable) vals so they can be referenced from
 * anywhere, including Theme.kt's colour-scheme definitions. The composable
 * accessors further down pick between them based on the system theme.
 * Dark values below are tasteful starting points — tune on-device.
 * ------------------------------------------------------------------ */
val BackgroundLight      = Color(0xFFF8F5F2)   // warm off-white background
val BackgroundDark       = Color(0xFF1C1712)   // warm near-black

val TextColourLight    = Color(0xFF5E4631)   // warm brown ink
val TextColourDark     = Color(0xFFEADBCB)   // warm cream

val BoxFillGreyLight   = Color(0xFFF5F5F5)   // card fill
val BoxFillGreyDark    = Color(0xFF2B2521)   // one step up from the dark background

val BoxBorderGreyLight = Color(0xFFEEEEEE)
val BoxBorderGreyDark  = Color(0xFF3B342E)

val CancelGreyLight    = Color(0xFFCCCCCC)
val CancelGreyDark     = Color(0xFF6E655C)

val DangerRedLight     = Color(0xFFB7382A)
val DangerRedDark      = Color(0xFFE5564A)   // brightened for contrast on dark

/* ------------------------------------------------------------------
 * Theme-aware accessors — the names your UI already uses
 *
 * Same identifiers as before, so every existing call site (tint = TextColour,
 * background(BoxFillGrey), containerColor = OffWhite, ...) keeps compiling
 * unchanged — they now simply resolve to the right value per theme.
 * ------------------------------------------------------------------ */
val BackgroundColour: Color
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BackgroundDark else BackgroundLight

val TextColour: Color
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) TextColourDark else TextColourLight

val BoxFillGrey: Color
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BoxFillGreyDark else BoxFillGreyLight

val BoxBorderGrey: Color
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) BoxBorderGreyDark else BoxBorderGreyLight

val CancelGrey: Color
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) CancelGreyDark else CancelGreyLight

val DangerRed: Color
    @Composable @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) DangerRedDark else DangerRedLight


/* ------------------------------------------------------------------
 * Activity palette — DATA, not chrome. Identical in both themes.
 *
 * These are stored per-activity in the database as ARGB ints and read from
 * non-composable code (the ViewModel's default seeding, the "Add" click
 * handler). They must stay plain vals: a @Composable getter can't be called
 * from those contexts, and since light == dark there's nothing to switch.
 * ------------------------------------------------------------------ */
val DeepBlue = Color(0xFF00538F)
val SkyBlue = Color(0xFF00A7E1)
val Orange = Color(0xFFE55934)
val Berry = Color(0xFFC44593)
val Crimson = Color(0xFF5C0029)
val Jungle = Color(0xFF668F80)
val Lime = Color(0xFF53DD6C)
val Purple = Color(0xFF9368B7)
// Added in V2.3
val Gold = Color(0xFFD6A51F)
val Teal = Color(0xFF00B3A4)
val Indigo = Color(0xFF4B3FCF)
val Red = Color(0xFFD1263A)
val Brown = Color(0xFF8C5A3C)
val Slate = Color(0xFF5C6B7A)
val Olive = Color(0xFF7F9A1A)
val Rose = Color(0xFFEE6C8F)

// Order matters: the default activities read this list by index (first-run seeding and
// Reset Activities), and the picker draws it in this order. Only ever append.
val LightActivityColours: List<Int> = listOf(
    DeepBlue.toArgb(),
    SkyBlue.toArgb(),
    Orange.toArgb(),
    Berry.toArgb(),
    Crimson.toArgb(),
    Jungle.toArgb(),
    Lime.toArgb(),
    Purple.toArgb(),
    Gold.toArgb(),
    Teal.toArgb(),
    Indigo.toArgb(),
    Red.toArgb(),
    Brown.toArgb(),
    Slate.toArgb(),
    Olive.toArgb(),
    Rose.toArgb()
)

// How faint a "ghost" bar is: the planned bars in SleepBarChart, the ghost bars and key in
// PairedBarChart. One value, so the app's planned treatment can't drift between charts.
const val GHOST_ALPHA = 0.28f
val UnloggedColour = Color(0xFF4A4A4A)
val OrphanedColour = Color(0xFF6E5A5A)

val InkLight = TextColourDark //Color.White
val InkDark = BackgroundDark //Color(0xFF1A1A1A)
// Picks whichever of white or near-black has more contrast on the fill.
// A plain function (not a composable getter), so it works inside draw scopes too.
fun inkOn(fill: Color): Color {
    fun contrast(a: Color, b: Color): Float {
        val hi = maxOf(a.luminance(), b.luminance())
        val lo = minOf(a.luminance(), b.luminance())
        return (hi + 0.05f) / (lo + 0.05f)
    }
    return if (contrast(fill, InkLight) >= contrast(fill, InkDark)) InkLight else InkDark
}