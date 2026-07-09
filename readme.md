*<div align="right"> Vikram Procter | July 9, 2026 </div>*

# What Now? - README

## Project Overview
![Image app main screen](./pics/)  
*Photo of finished project*

A personal Android app for tracking how you actually spend each hour of your day. Every hour, on the hour, NowWhat asks two questions — what did you just do, and what will you do next — and builds a colour-coded picture of your days from your answers.

## The idea

Instead of free-text journaling, you log each hour by tapping a **preset activity** (for example: Work, Sleep, Gym, Social, Dating), each with its own colour. Over time you get an at-a-glance map of where your hours go — and how often your plans matched reality.

## What it looks like

The main screen, top to bottom:

- **Top bar** — the app name, and a settings icon (top right) that opens Settings.
- **Hours view** (scrollable) — one section per day. Each day has **four rows**: Morning, Day, Evening, and Night. Each row holds one **box per hour** in that band. Every box is:
  - **outlined** in the colour of the activity you *planned* for that hour, and
  - **filled** with the colour of the activity you *actually* did.

  So a box outlined amber but filled teal means "planned to gym, ended up working." Outline-only means planned but not yet done; an empty box shows a light grey fill. Boxes have rounded corners and the selected hour shows a raised shadow. The top and bottom of the scrollable area fade smoothly into the background.
- **Entry panel** (pinned to bottom) — shows the currently selected time slot with a clock icon. Displays preset buttons in two rows: outlined "What's planned?" buttons and filled "What's happened?" buttons. Both rows include an "Edit+" button that navigates to Settings. The "What's happened?" buttons are disabled for hours in the future. Includes a **colour legend** mapping each colour to its activity. The right edge of the button rows fades to avoid a hard scroll cutoff.

The **settings screen** is a scrollable list of setting rows, each a rounded rectangle with either a chevron (opens a sub-screen), a toggle, or a number stepper:
- **Edit Activities** → activity management screen
- **Edit Default Schedule** → weekly schedule editor
- **Day Starts At** — a number stepper choosing the hour the day begins (see "The logical day" below)
- **24-Hour Time** — a toggle switching all time displays between 12- and 24-hour format
- **Notifications** → notifications settings: master on/off switch, and a do-not-disturb window (from/until hour steppers)
- **Export/Import Data** → CSV import/export (placeholder)
- **DANGER ZONE** → destructive actions (placeholder)

The **activities editor** shows an editable list of activities — each row has a tappable colour swatch (revealing a palette to pick from), an inline text field for renaming, and a delete button — plus an "Add" button at the bottom.

The **default schedule editor** lets you define a recurring weekly plan. A row of seven day-chips (Mo–Su) selects which weekday you're editing; below it a grid identical to the main hours view shows that weekday's planned activities. Tap an hour box to select it, then tap a preset to set (or Clear to remove) the planned activity for that slot. The header shows the selected hour and which weekday it belongs to.

## How it works

### The logical day

A NowWhat "day" doesn't run midnight-to-midnight — it runs from a configurable **start hour** (default 6am) to the same time the next calendar day. The small hours after midnight (e.g. 1am) therefore belong to the *previous* logical day's "Night" band. This keeps a single night's late hours grouped with the day they belong to rather than split across the calendar boundary. All timestamp ↔ day/hour conversions live in one place (`DayBoundry.kt`) and pivot on this start hour, so changing "Day Starts At" re-lays-out the entire grid consistently.

### Default schedule auto-fill

When a new logical day first appears (on launch, or detected live at the day boundary via a one-minute ticker), the app seeds that day's *planned* activities from the matching weekday's schedule template. Seeding is **fill-only**: it never overwrites a planned activity you set manually. A "last seeded day" marker in DataStore ensures each day seeds only once.

### Notifications

Hourly **notifications** fire on the hour (using exact alarms) prompting you to log the hour just passed. The engine is a **self-rescheduling one-shot alarm**: an exact alarm fires into `AlarmReceiver`, which immediately schedules the next hour's alarm before doing anything else (so the chain never breaks), then posts the notification. A `BootReceiver` reschedules on device restart, since Android clears alarms on reboot.

The notification carries **inline action buttons** — tapping one logs that activity as the *actual* for the hour that just passed, straight from the notification shade, via `NotificationActionReceiver` (which writes to Room directly and dismisses the notification). Tapping the notification body instead opens the app. Suggestions are chosen contextually: first the activity you *planned* for the hour that just passed, then (as a fallback) what you *actually* did the hour before, then a default — deduplicated, and always at least one.

Two deliberate scope limits: a notification shows at most **three action buttons**, so the current design is **log-only** — planning the hour ahead from the notification would need a different mechanism (inline text reply, or tap-to-open) and is future work. Notification **format** options are also not yet built.

**Settings:** a master on/off switch and a **do-not-disturb window** (from/until hour). When notifications are off or the current hour falls inside the DND window, the alarm still reschedules but skips posting — silencing never breaks the chain. The DND check reuses `withinHourSpan` from `DayBoundry.kt`, so it correctly handles windows that cross midnight.

## Tech stack

- **Kotlin** + **Jetpack Compose** (declarative UI)
- **Room** (SQLite) for record data (activities, hour entries, schedule), processed via **KSP**
- **Jetpack DataStore** (Preferences) for scalar settings (start hour, time format, seed marker)
- **ViewModel** + **Kotlin Flow** + **StateFlow** for reactive state
- **AlarmManager** (exact alarms) + **BroadcastReceiver**s for the self-rescheduling on-the-hour notification engine
- **Material Design 3** with custom colour theme and vector drawable icons
- Built in Android Studio; tested on a physical device over wireless ADB

## Architecture

Data flows in one direction: **Entity → DAO → Database → ViewModel → Composable** (with DataStore as a parallel source for settings). Reads are exposed as reactive `Flow`s and `StateFlow`s, so the UI updates itself whenever the data changes. Events flow upward via callback lambdas (state hoisting).

The **notification engine is a second entry point into the data layer**, living outside the ViewModel/Compose lifecycle: `AlarmReceiver`, `NotificationActionReceiver`, and `BootReceiver` reach Room and DataStore directly via `AppDatabase.getDatabase(context)` / `SettingsStore(context)` (reading with `runBlocking { flow.first() }`, which is acceptable for these short-lived, local-file reads). `NotificationHelper` (a stateless `object`) owns channel creation, notification building, and alarm scheduling. See the Notifications section above for the flow.

Navigation uses a simple state-based approach: an `AppScreenState` enum (`MAIN`, `SETTINGS`, `SETTINGS_ACTIVITIES`, `SETTINGS_DEFAULTSCHEDULE`, `SETTINGS_NOTIFICATIONS`, `SETTINGS_DATA`, `SETTINGS_DANGERZONE`) held in `MainActivity`, with a `when` expression swapping between screens. Sub-screens navigate back to `SETTINGS`; the settings list navigates back to `MAIN`. Every screen receives the shared `ViewModel` instance and an `onNavigate` callback.

UI component tree:

- `MainActivity` — holds screen state and ViewModel, wraps everything in `NowWhatTheme`
  - `NowWhatScreen` — owns the Scaffold with TopBar (topBar slot) and EntryPanel (bottomBar slot)
    - `TopBar` — title + settings icon
    - `HoursView` — scrollable `LazyColumn` of days with top/bottom fade effects
      - `DaySection` — date label + four part-of-day rows (takes `hourRows` + `dateLabel`, not a whole `Day`)
        - `PartOfDayRow` — band label + a row of boxes
          - `HourBox` — one hour: outline = planned colour, fill = actual colour; rounded corners, shadow when selected; tappable
    - `EntryPanel` — pinned bottom panel with right-edge fade effect
      - `PresetButton` — outlined (planned) or filled (actual) button per preset activity; supports an `enabled` flag
      - `ActivityLegend` — colour → activity key
  - `NowWhatSettingsScreen` — settings list of `SettingRow`s
    - `SettingRow` — rounded row with a label and a trailing slot (chevron / switch / stepper)
    - `NumberStepper` — reusable −/value/+ stepper with a `format` lambda
    - `ActivitiesSettingsScreen` → `ActivitiesSettings`
    - `DefaultScheduleSettingsScreen` → `WeekdayPicker` + `DaySection` + preset row
    - `NotificationsSettingsScreen` (wired: master switch + DND window)
    - `DataSettingsScreen`, `DangerZoneSettingsScreen` (placeholders)
    - `TopBarSettings` — "Settings" title (+ optional breadcrumb path) + close/back icon

## Data model

Three tables:

- **Activity** (`activities`) — a preset: a name, a colour (ARGB Int), and an auto-generated ID.
- **HourEntry** (`hour_entries`) — one hour: a timestamp (epoch millis, truncated to the hour), a *planned* Activity ID, and an *actual* Activity ID (both nullable).
- **ScheduleEntry** (`schedule_entries`) — one slot of the weekly template: a `dayOfWeek` (1–7, ISO Mon–Sun), an `hourOfDay` (0–23), a *planned* Activity ID (nullable), and an auto-generated ID. A composite **unique index** on `(dayOfWeek, hourOfDay)` guarantees one row per slot; writes use `OnConflictStrategy.REPLACE`.

Scalar settings (DataStore, not Room):

- `is_24_hour` (Boolean), `day_start_hour` (Int), `last_seeded_day` (Long, epoch-day marker), `notifications_enabled` (Boolean, default true), `dnd_start_hour` (Int, default 22), `dnd_end_hour` (Int, default 7).

Supporting UI classes (not persisted):

- **HourSlot** — holds a planned and an actual `Activity?`, used to pass data to `HourBox`.
- **Day** — holds a date string, a `LocalDate`, and four lists of `HourSlot?` (one per time band), used for the main hours view. The schedule editor uses bare `List<List<HourSlot?>>` rows rather than a `Day`, since a template has no calendar date.

## Status

Done:

- Development environment installed and project created
- Room toolchain wired up (Room 2.8.4 + KSP); DataStore added for scalar settings
- Data layer: `Activity` + `ActivityDao`, `HourEntry` + `HourEntryDao` (upsert pattern), `ScheduleEntry` + `ScheduleDao` (unique-index + REPLACE), `AppDatabase` (v3, singleton, destructive fallback)
- `SettingsStore` — DataStore wrapper exposing settings as `Flow`s with suspend writers
- `NowWhatViewModel` with `combine` to merge flows, `transformIntoDays`/`transformScheduleIntoRows`, log functions with upsert logic, activity CRUD, schedule CRUD, settings setters, and the day-change seeding collector
- Full UI component tree built bottom-up with Compose
- Hour selection, planned/actual logging, reactive grid updates
- Default activities seeded on first launch
- State-based navigation across the seven-screen settings tree
- Activities editor: add, rename (saves on focus loss), recolour, delete
- Visual polish: rounded HourBox, shadow selection, Material vector icons, custom theme, gradient fades, filled vs outlined presets, EntryPanel pinned via bottomBar slot
- Auto-populate today's date in HoursView on launch (empty-day synthesis in `transformIntoDays`)
- Prevent logging "actual" for future hours (ViewModel guard + disabled buttons in EntryPanel)
- Configurable **start of day**: a `DAY_START_HOUR`-seeded DataStore setting drives all logical-day math, grid slicing, and labels; grid re-lays-out live when changed
- 24-hour time format toggle, applied to all time displays
- Default schedule: per-weekday template editor (day-chips + grid + presets), stored in Room with the logical-day weekday-wrap on both read and write, plus fill-only auto-fill of new days
- **Hourly notification engine**: notification channel, `POST_NOTIFICATIONS` runtime-permission flow, exact alarms via `AlarmManager`, self-rescheduling one-shot pattern (`AlarmReceiver` reschedules on fire), boot persistence (`BootReceiver` on `BOOT_COMPLETED`), tap-to-open, and **inline action buttons** that log the actual activity from the shade (`NotificationActionReceiver`) with contextual, deduplicated suggestions
- **Notifications settings wired**: master on/off switch + do-not-disturb window (from/until steppers); the receiver honours both (silencing reschedules but skips posting)
- 24-hour format now also applied to the hour steppers ("Day Starts At", DND from/until) via `formatHourLabel`; steppers gained an optional `wrap` mode (uses `wrapRange`) so time values roll 23→0 instead of clamping
- Settings-row visual polish: raised shadow + rounded shape so each row reads as its own card; disabled rows (and their steppers) grey out via `alpha` and stop responding to input
- App runs on physical device over wireless ADB

## TODO

- [x] Redesign the data model to the preset-based schema (an `Activity` table; planned + actual activity per hour)
- [x] Build the new UI scaffolding bottom-up with stand-in data and `@Preview`
- [x] Wire the scaffolded UI to the ViewModel and Room
- [x] Settings screen: manage preset activities (add, edit, delete) and their colours
- [x] Visual polish: rounded corners, Material icons, spacing, theming, gradient fades
- [x] Auto-populate today's date in the HoursView on launch
- [x] Prevent logging "actual" activities for hours in the future
- [x] Configurable start-of-day hour (drives the logical-day system)
- [x] 24-hour / 12-hour time format toggle
- [x] Default schedule: per-weekday recurring planned activities that auto-populate new days
- [x] Hourly notification engine: exact alarms + reschedule-on-fire + boot receiver + `POST_NOTIFICATIONS` permission + notification channel + inline action buttons (log actual from the shade)
- [x] Notifications settings: master switch + do-not-disturb window *(format options and planning-from-notification deferred — see below)*
- [ ] **Danger zone in settings: clear all data / reset database (screen scaffolded, not wired; `ScheduleDao.deleteAll()` already exists)** ← next priority
- [ ] CSV export/import via the system share sheet (screen scaffolded, not wired)
- [ ] Notification enhancements deferred from the engine work: **planning the next hour from the notification** (blocked by the 3-button limit — needs inline text reply or tap-to-open), and **notification format options**
- [ ] Retire the one-minute seeding ticker in favour of the hourly alarm as the day-change pulse (the engine now provides a natural hourly heartbeat)
- [ ] More style decisions: font, font size, colour scheme, night mode colours
- [ ] Android back button to navigate instead of close app
- [ ] Cleanup: unused imports (e.g. `java.sql.Timestamp` and `kotlin.time…milliseconds` in the ViewModel); replace `fallbackToDestructiveMigration()` with real migrations before real data accumulates; remove any remaining legacy v1 files

Placeholder assumptions: the four time bands are fixed 6-hour blocks anchored at the start hour (Morning = start, then +6/+12/+18), and the starter presets are Work, Sleep, Gym, Social, and Dating.
