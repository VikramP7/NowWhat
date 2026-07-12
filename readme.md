*<div align="right"> Vikram Procter | June 19, 2026 </div>*

# What Now? - README

## Project Overview
![Image app main screen](./pics/)  
*Photo of finished project*

A personal Android app for tracking how you actually spend each hour of your day. Every hour, on the hour, NowWhat asks two questions, what did you just do and what will you do next, then builds a colour-coded picture of your days from your answers.

## The idea

Instead of free-text journaling, you log each hour by tapping a **preset activity** (for example: Work, Sleep, Gym, Social, Dating), each with its own colour. Over time you get an at-a-glance map of where your hours go, and how often your plans matched reality.

## What it looks like

The main screen, top to bottom:

- **Top bar**: the app name, and a settings icon (top right) that opens Settings.
- **Hours view** (scrollable): one section per day. Each day has **four rows**: Morning, Day, Evening, and Night. Each row holds one **box per hour** in that band. Every box is:
  - **outlined** in the colour of the activity you *planned* for that hour, and
  - **filled** with the colour of the activity you *actually* did.

  So a box outlined amber but filled teal means "planned to gym, ended up working." Outline-only means planned but not yet done; an empty box shows a light grey fill. Boxes have rounded corners and the selected hour shows a raised shadow. The top and bottom of the scrollable area fade smoothly into the background.
- **Entry panel** (pinned to bottom): shows the currently selected time slot with a clock icon. Displays preset buttons in two rows: outlined "What's planned?" buttons and filled "What's happened?" buttons. Both rows include an "Edit+" button that navigates to Settings. The "What's happened?" buttons are disabled for hours in the future. Includes a **colour legend** mapping each colour to its activity. The right edge of the button rows fades to avoid a hard scroll cutoff.

The **settings screen** is a scrollable list of setting rows, each a rounded rectangle with either a chevron (opens a sub-screen), a toggle, or a number stepper:
- **Edit Activities** → activity management screen
- **Edit Default Schedule** → weekly schedule editor
- **Day Starts At**: a number stepper choosing the hour the day begins (see "The logical day" below)
- **24-Hour Time**: a toggle switching all time displays between 12- and 24-hour format
- **Notifications** → notifications settings: master on/off switch, and a do-not-disturb window (from/until hour steppers)
- **Export/Import Data** → back up all data to a JSON file, or restore from one (via the system file picker)
- **DANGER ZONE** → destructive actions, each behind a confirmation dialog (clear logged hours, reset activities, clear default schedule)

The **activities editor** shows an editable list of activities styled as settings-style cards. Each card has a tappable colour swatch (revealing a palette to pick from), an inline text field for renaming, and a delete button, plus an "Add" button at the bottom. The list scrolls clear of the keyboard when a name field is focused.

The **default schedule editor** lets you define a recurring weekly plan. A row of seven day-chips (Mo–Su) selects which weekday you're editing; below it a grid identical to the main hours view shows that weekday's planned activities. Tap an hour box to select it, then tap a preset to set (or Clear to remove) the planned activity for that slot. The header shows the selected hour and which weekday it belongs to.

## Installing the app

NowWhat isn't on the Play Store; it installs as a standalone APK. To try it on an Android phone (Android 8.0 / API 26 or newer):

1. Download the latest `app-debug.apk` from the [Releases](../../releases) page of this repository.
2. Copy it to the phone, or download it directly on the device.
3. Open the file. Android will ask permission to install from this source: allow "Install unknown apps" for whichever app you used to open it (your browser or file manager), then confirm the install.
4. Open NowWhat. On first launch it asks for notification permission; grant it so the hourly prompts can appear.

To build the APK yourself:

1. Open the project in Android Studio and let Gradle sync.
2. Run **Build → Build App Bundle(s) / APK(s) → Build APK(s)**, or from a terminal in the project root run `./gradlew assembleDebug`.
3. The APK lands at `app/build/outputs/apk/debug/app-debug.apk`. This debug build is signed with the local debug key and installs directly, which is all that's needed for sharing a demo. A Play Store submission would instead need a signed release build (see Android Studio's **Build → Generate Signed Bundle / APK** wizard).

## How it works

### The logical day

A NowWhat "day" doesn't run midnight-to-midnight: it runs from a configurable **start hour** (default 6am) to the same time the next calendar day. The small hours after midnight (e.g. 1am) therefore belong to the *previous* logical day's "Night" band. This keeps a single night's late hours grouped with the day they belong to rather than split across the calendar boundary. All timestamp ↔ day/hour conversions live in one place (`DayBoundry.kt`) and pivot on this start hour, so changing "Day Starts At" re-lays-out the entire grid consistently.

### Default schedule auto-fill

When a new logical day first appears, the app seeds that day's *planned* activities from the matching weekday's schedule template. Seeding is **fill-only**: it never overwrites a planned activity you set manually, and a "last seeded day" marker in DataStore ensures each day seeds only once. The seeding logic lives in one place (`Seeding.kt`) and is triggered from two spots: a one-shot seed when the ViewModel starts (covering app launch), and the hourly notification alarm (covering the day boundary while the app is open or in the background). Because the day boundary always falls on a whole hour, the hourly alarm lands exactly on it, so there is no need to poll. Changing "Day Starts At" also re-runs the seed, via a small collector on the setting.

### Notifications

Hourly **notifications** fire on the hour (using exact alarms) prompting you to log the hour just passed. The engine is a **self-rescheduling one-shot alarm**: an exact alarm fires into `AlarmReceiver`, which immediately schedules the next hour's alarm before doing anything else (so the chain never breaks), then handles seeding and the notification. A `BootReceiver` reschedules on device restart, since Android clears alarms on reboot.

The notification carries **inline action buttons**: tapping one logs that activity as the *actual* for the hour that just passed, straight from the notification shade, via `NotificationActionReceiver` (which writes to Room directly and dismisses the notification). Tapping the notification body instead opens the app. Suggestions are chosen contextually: first the activity you *planned* for the hour that just passed, then (as a fallback) what you *actually* did the hour before, then a default, deduplicated and always at least one. The notification text also names the exact hour being asked about (for example "What did you just do from 2pm - 3pm?"), formatted through `formatHourLabel` so it respects the 12/24-hour setting.

One deliberate scope limit: a notification shows at most **three action buttons**, so the design is **log-only**, and this is a settled choice, not a gap. Planning the hour ahead from the shade would need a different mechanism (inline text reply, or tap-to-open); the current log-only style is preferred and won't be extended that way.

**Settings:** a master on/off switch and a **do-not-disturb window** (from/until hour). When notifications are off or the current hour falls inside the DND window, the alarm still reschedules but skips posting, so silencing never breaks the chain. The day-change seed sits *before* those checks in `AlarmReceiver`, so it keeps running even when notifications are silenced. The DND check reuses `withinHourSpan` from `DayBoundry.kt`, so it correctly handles windows that cross midnight.

### Theming and dark mode

The app follows the system light/dark setting. Rather than routing every screen through `MaterialTheme.colorScheme`, the handful of UI "chrome" colours (background, text, card fill, borders, cancel grey, danger red) are exposed from `Color.kt` as composable accessors: each name resolves to a light or dark value based on `isSystemInDarkTheme()`, backed by an explicit light/dark value pair. Because the accessors keep the same names the UI already used, no call sites had to change. The one caveat this introduces: a composable accessor can only be read from a composable context, so any use inside a non-composable draw scope (the gradient fades in `HoursView` and `EntryPanel`) first reads the colour into a local `val`. The activity palette is deliberately *not* themed: those colours are stored per-activity in the database as ARGB ints and are read from non-composable code, so they stay plain values and look identical in both themes.

### Data backup (export / import)

The **Export/Import** screen lets you back up everything to a single JSON file and restore it later. Both directions go through Android's **Storage Access Framework**: the OS file picker hands the app a `Uri` for exactly the one file the user chose, so no broad storage permission is needed. Serialization is hand-rolled with the built-in `org.json` (no dependency): three arrays (activities, entries, schedule) plus a `version` field for future migrations. The `ContentResolver` stream I/O runs on `Dispatchers.IO`; Room's own suspend DAOs handle their own threading.

**Import is replace, not merge:** the whole database is wiped and rebuilt from the file, inside a single Room **transaction** so a mid-restore failure rolls back rather than leaving a half-wiped database. Crucially, activity **IDs are preserved** across the round-trip: because hour entries and schedule slots reference activities *by ID*, letting Room auto-generate fresh IDs on import would silently repoint every logged hour at the wrong activity. The file is parsed *before* the transaction opens, so a malformed file fails loudly and touches nothing. A colour stored as an opaque ARGB `Int` serializes as a negative number (the alpha byte sets the sign bit); this is lossless and expected. Merge-style import is possible future work but deliberately deferred to avoid ID-collision handling.

## Tech stack

- **Kotlin** + **Jetpack Compose** (declarative UI)
- **Room** (SQLite) for record data (activities, hour entries, schedule), processed via **KSP**; schema export enabled for migration-readiness
- **Jetpack DataStore** (Preferences) for scalar settings (start hour, time format, seed marker)
- **ViewModel** + **Kotlin Flow** + **StateFlow** for reactive state
- **AlarmManager** (exact alarms) + **BroadcastReceiver**s for the self-rescheduling on-the-hour notification engine
- **Storage Access Framework** (`ActivityResultContracts.CreateDocument`/`OpenDocument`) + `ContentResolver` for user-driven file export/import; **`org.json`** for backup serialization
- **Material Design 3** with a custom, system-following light/dark colour theme and vector drawable icons
- Built in Android Studio; tested on a physical device over wireless ADB

## Architecture

Data flows in one direction: **Entity → DAO → Database → ViewModel → Composable** (with DataStore as a parallel source for settings). Reads are exposed as reactive `Flow`s and `StateFlow`s, so the UI updates itself whenever the data changes. Events flow upward via callback lambdas (state hoisting).

The **notification engine is a second entry point into the data layer**, living outside the ViewModel/Compose lifecycle: `AlarmReceiver`, `NotificationActionReceiver`, and `BootReceiver` reach Room and DataStore directly via `AppDatabase.getDatabase(context)` / `SettingsStore(context)` (reading with `runBlocking { flow.first() }`, which is acceptable for these short-lived, local-file reads). `AlarmReceiver` also drives the day-change seed each hour through the shared `seedDayFromSchedule` in `Seeding.kt`, the same function the ViewModel calls on launch. `NotificationHelper` (a stateless `object`) owns channel creation, notification building, and alarm scheduling. See the Notifications section above for the flow.

Navigation uses a simple state-based approach: an `AppScreenState` enum (`MAIN`, `SETTINGS`, `SETTINGS_ACTIVITIES`, `SETTINGS_DEFAULTSCHEDULE`, `SETTINGS_NOTIFICATIONS`, `SETTINGS_DATA`, `SETTINGS_DANGERZONE`) held in `MainActivity`, with a `when` expression swapping between screens. Sub-screens navigate back to `SETTINGS`; the settings list navigates back to `MAIN`. The Android system back button mirrors this: a single `BackHandler` uses an `AppScreenState.parent()` mapping to step up the tree, and on `MAIN` (which has no parent) it disables itself so the press falls through to the OS and the app closes normally. Every screen receives the shared `ViewModel` instance and an `onNavigate` callback.

UI component tree:

- `MainActivity`: holds screen state and ViewModel, wraps everything in `NowWhatTheme`, and owns the back-navigation `BackHandler`
  - `NowWhatScreen`: owns the Scaffold with TopBar (topBar slot) and EntryPanel (bottomBar slot)
    - `TopBar`: title + settings icon
    - `HoursView`: scrollable `LazyColumn` of days with top/bottom fade effects
      - `DaySection`: date label + four part-of-day rows (takes `hourRows` + `dateLabel`, not a whole `Day`)
        - `PartOfDayRow`: band label + a row of boxes
          - `HourBox`: one hour. Outline = planned colour, fill = actual colour; rounded corners, shadow when selected; tappable
    - `EntryPanel`: pinned bottom panel with right-edge fade effect
      - `PresetButton`: outlined (planned) or filled (actual) button per preset activity; supports an `enabled` flag
      - `ActivityLegend`: colour → activity key
  - `NowWhatSettingsScreen`: settings list of `SettingRow`s
    - `SettingRow`: rounded card row with an optional `leading` slot, either a plain `label` or a custom `content` slot, and a trailing slot (chevron / switch / stepper, or swatch + name field + delete on the activities screen)
    - `NumberStepper`: reusable −/value/+ stepper with a `format` lambda
    - `ActivitiesSettingsScreen`: self-contained (no separate child composable). Renders each activity as a `SettingRow` with the colour swatch in `leading`, an inline-rename `BasicTextField` in `content`, and a delete action trailing; keyboard-aware via `imePadding`
    - `DefaultScheduleSettingsScreen` → `WeekdayPicker` + `DaySection` + preset row
    - `NotificationsSettingsScreen` (wired: master switch + DND window)
    - `DataSettingsScreen` (wired: JSON export/import via SAF file pickers, with an import confirmation)
    - `DangerZoneSettingsScreen` (wired: destructive actions, each behind a `ConfirmDialog`)
    - `ConfirmDialog`: reusable destructive-confirmation `AlertDialog` (title, message, icon, confirm label + `onConfirm`/`onDismiss`); shared by Danger Zone and import
    - `TopBarSettings`: "Settings" title (+ optional breadcrumb path) + close/back icon

## Data model

Three tables:

- **Activity** (`activities`): a preset with a name, a colour (ARGB Int), and an auto-generated ID.
- **HourEntry** (`hour_entries`): one hour, holding a timestamp (epoch millis, truncated to the hour), a *planned* Activity ID, and an *actual* Activity ID (both nullable).
- **ScheduleEntry** (`schedule_entries`): one slot of the weekly template, holding a `dayOfWeek` (1–7, ISO Mon–Sun), an `hourOfDay` (0–23), a *planned* Activity ID (nullable), and an auto-generated ID. A composite **unique index** on `(dayOfWeek, hourOfDay)` guarantees one row per slot; writes use `OnConflictStrategy.REPLACE`.

The database is at **version 3**. Schema export is enabled (`exportSchema = true` plus the KSP `room.schemaLocation` argument), and the v3 baseline (`schemas/…/AppDatabase/3.json`) is committed to version control. This means a future additive change (adding a column or table) can be handled with a bumped version and an `@AutoMigration(from = 3, to = 4)`, letting Room generate the migration SQL by diffing against the committed baseline. The builder still keeps `fallbackToDestructiveMigration(true)` as a backstop; no schema change has been needed yet, so no real migration has had to be written.

Scalar settings (DataStore, not Room):

- `is_24_hour` (Boolean), `day_start_hour` (Int), `last_seeded_day` (Long, epoch-day marker), `notifications_enabled` (Boolean, default true), `dnd_start_hour` (Int, default 22), `dnd_end_hour` (Int, default 7).

Supporting UI classes (not persisted):

- **HourSlot**: holds a planned and an actual `Activity?`, used to pass data to `HourBox`.
- **Day**: holds a date string, a `LocalDate`, and four lists of `HourSlot?` (one per time band), used for the main hours view. The schedule editor uses bare `List<List<HourSlot?>>` rows rather than a `Day`, since a template has no calendar date.

## Status

Done:

- Development environment installed and project created
- Room toolchain wired up (Room 2.8.4 + KSP); DataStore added for scalar settings
- Data layer: `Activity` + `ActivityDao`, `HourEntry` + `HourEntryDao` (upsert pattern), `ScheduleEntry` + `ScheduleDao` (unique-index + REPLACE), `AppDatabase` (v3, singleton)
- `SettingsStore`: DataStore wrapper exposing settings as `Flow`s with suspend writers
- `NowWhatViewModel` with `combine` to merge flows, `transformIntoDays`/`transformScheduleIntoRows`, log functions with upsert logic, activity CRUD, schedule CRUD, and settings setters
- Full UI component tree built bottom-up with Compose
- Hour selection, planned/actual logging, reactive grid updates
- Default activities seeded on first launch
- State-based navigation across the seven-screen settings tree, plus Android back-button navigation
- Activities editor: add, rename (saves on focus loss), recolour, delete, unified onto `SettingRow`
- Visual polish: rounded HourBox, shadow selection, Material vector icons, custom theme, gradient fades, filled vs outlined presets, EntryPanel pinned via bottomBar slot
- Auto-populate today's date in HoursView on launch (empty-day synthesis in `transformIntoDays`)
- Prevent logging "actual" for future hours (ViewModel guard + disabled buttons in EntryPanel)
- Configurable **start of day**: a `DAY_START_HOUR`-seeded DataStore setting drives all logical-day math, grid slicing, and labels; grid re-lays-out live when changed
- 24-hour time format toggle, applied to all time displays
- Default schedule: per-weekday template editor (day-chips + grid + presets), stored in Room with the logical-day weekday-wrap on both read and write, plus fill-only auto-fill of new days
- **Hourly notification engine**: notification channel, `POST_NOTIFICATIONS` runtime-permission flow, exact alarms via `AlarmManager`, self-rescheduling one-shot pattern (`AlarmReceiver` reschedules on fire), boot persistence (`BootReceiver` on `BOOT_COMPLETED`), tap-to-open, and **inline action buttons** that log the actual activity from the shade (`NotificationActionReceiver`) with contextual, deduplicated suggestions
- **Notification text names the hour being logged** (via `formatHourLabel`, honouring the 12/24-hour setting)
- **Notifications settings wired**: master on/off switch + do-not-disturb window (from/until steppers); the receiver honours both (silencing reschedules but skips posting)
- 24-hour format now also applied to the hour steppers ("Day Starts At", DND from/until) via `formatHourLabel`; steppers gained an optional `wrap` mode (uses `wrapRange`) so time values roll 23→0 instead of clamping
- Settings-row visual polish: raised shadow + rounded shape so each row reads as its own card; disabled rows (and their steppers) grey out via `alpha` and stop responding to input; `SettingRow` extended with optional `leading` and `content` slots
- **Danger Zone wired**: clear all logged hours (resets the seed marker + re-seeds today from the schedule), reset activities to defaults (trim-and-rename, IDs preserved so existing entries keep resolving), clear default schedule, each behind a reusable `ConfirmDialog` with destructive-red styling (`DangerRed`)
- **JSON backup export/import**: single-file backup via the Storage Access Framework, `org.json` serialization with a `version` field, replace-on-import inside a Room transaction with activity-ID preservation, plus an import confirmation dialog. Round-trip verified on device (export → wipe via Danger Zone → import → identical)
- **Seeding ticker retired**: the day-change seed is factored into `Seeding.kt` and driven by a one-shot launch seed plus the hourly alarm pulse, both guarded by the `last_seeded_day` marker; the one-minute polling loop is gone
- **System-following dark mode**: chrome colours are composable accessors that switch on `isSystemInDarkTheme()`, backed by light/dark value pairs; activity palette stays theme-independent
- **Room migration-readiness**: schema export enabled and the v3 baseline committed
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
- [x] Notifications settings: master switch + do-not-disturb window *(format options and planning-from-notification deferred; see below)*
- [x] Danger zone in settings: clear logged hours / reset activities / clear default schedule, each behind a confirmation dialog
- [x] Data export/import: single-file **JSON** backup via the system file picker (replace-on-import, IDs preserved, transactional). *Note: implemented as JSON via the Storage Access Framework rather than CSV via the share sheet, for lossless escaping and one-file simplicity.*
- [x] **Show the hour being logged in the notification**: the title/text now names the hour the prompt refers to. *(Planning-the-next-hour-from-the-notification is explicitly **not** wanted; the current log-only style is preferred. Notification format options likewise deferred as low priority.)*
- [x] Retire the one-minute seeding ticker in favour of the hourly alarm as the day-change pulse
- [x] System-following dark mode colours *(font and font-size choices intentionally left at Material defaults; not pursued)*
- [x] Unify the activity settings screen onto `SettingRow`
- [x] Android back button to navigate instead of close the app
- [x] Cleanup: unused imports in touched files removed; legacy `ActivitiesSettings.kt` deleted after inlining
- [x] Enable Room schema export and commit the v3 baseline (migration-readiness)

Deferred (out of scope for this build, but noted for anyone picking it up):

- [ ] Write a real `@AutoMigration` the next time an entity changes, and drop `fallbackToDestructiveMigration` once migrations cover every version path
- [ ] Merge-style import (currently replace-only); would need activity-ID-collision handling
- [ ] Fully configurable time bands (currently fixed 6-hour blocks anchored at the start hour)

Placeholder assumptions: the four time bands are fixed 6-hour blocks anchored at the start hour (Morning = start, then +6/+12/+18), and the starter presets are Work, Sleep, Gym, Social, and Dating.
