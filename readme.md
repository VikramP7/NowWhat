*<div align="right"> Vikram Procter | June 21, 2026 </div>*

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
- **Entry panel** (pinned to bottom) — shows the currently selected time slot with a clock icon. Displays preset buttons in two rows: outlined "What's planned?" buttons and filled "What's happened?" buttons. Both rows include an "Edit+" button that navigates to Settings. Includes a **colour legend** mapping each colour to its activity. The right edge of the button rows fades to avoid a hard scroll cutoff.

The **settings screen** shows:
- A "Settings" title bar with a close icon
- An editable list of activities — each row has a tappable colour swatch, an inline text field for renaming, and a delete button
- Tapping a colour swatch reveals a row of colour palette swatches to pick from
- An "Add" button at the bottom to create new activities

## How it works (planned)

Hourly **notifications** fire on the hour (using exact alarms) prompting you to log the hour just passed and plan the hour ahead — ideally answerable straight from the notification.

## Tech stack

- **Kotlin** + **Jetpack Compose** (declarative UI)
- **Room** (SQLite) for persistence, processed via **KSP**
- **ViewModel** + **Kotlin Flow** + **StateFlow** for reactive state
- **AlarmManager** (exact alarms) for on-the-hour notifications (planned)
- **Material Design 3** with custom colour theme and vector drawable icons
- Built in Android Studio; tested on a physical device over wireless ADB

## Architecture

Data flows in one direction: **Entity → DAO → Database → ViewModel → Composable**. Reads are exposed as reactive `Flow`s and `StateFlow`s, so the UI updates itself whenever the data changes. Events flow upward via callback lambdas (state hoisting).

Navigation uses a simple state-based approach: an `AppScreenState` enum (`MAIN`, `SETTINGS`) held in `MainActivity`, with a `when` expression swapping between `NowWhatScreen` and `NowWhatSettingsScreen`. Both screens receive the shared `ViewModel` instance and navigation callbacks.

UI component tree:

- `MainActivity` — holds screen state and ViewModel, wraps everything in `NowWhatTheme`
  - `NowWhatScreen` — owns the Scaffold with TopBar (topBar slot) and EntryPanel (bottomBar slot)
    - `TopBar` — title + settings icon
    - `HoursView` — scrollable `LazyColumn` of days with top/bottom fade effects
      - `DaySection` — date label + four part-of-day rows
        - `PartOfDayRow` — band label + a row of boxes
          - `HourBox` — one hour: outline = planned colour, fill = actual colour; rounded corners, shadow when selected; tappable
    - `EntryPanel` — pinned bottom panel with right-edge fade effect
      - `PresetButton` — outlined (planned) or filled (actual) button per preset activity
      - `ActivityLegend` — colour → activity key
  - `NowWhatSettingsScreen` — owns its own Scaffold with TopBarSettings
    - `TopBarSettings` — "Settings" title + close icon
    - `ActivitiesSettings` — activity list with inline editing, colour picker, delete, and add

## Data model

Two tables:

- **Activity** (`activities`) — a preset: a name, a colour (ARGB Int), and an auto-generated ID.
- **HourEntry** (`hour_entries`) — one hour: a timestamp (epoch millis, truncated to the hour), a *planned* Activity ID, and an *actual* Activity ID (both nullable).

Supporting UI classes (not persisted):

- **HourSlot** — holds a planned and an actual `Activity?`, used to pass data to `HourBox`.
- **Day** — holds a date string, a `LocalDate`, and four lists of `HourSlot?` (one per time band), used to pass data to `DaySection`.

## Status

Done:

- Development environment installed and project created
- Room toolchain wired up (Room 2.8.4 + KSP)
- Data layer: `Activity` entity + `ActivityDao` (insert, update, getAll, delete), `HourEntry` entity (preset-based with planned/actual activity IDs) + `HourEntryDao` (with upsert pattern), `AppDatabase` (singleton with destructive migration fallback)
- `NowWhatViewModel` with `combine` to merge entries and activities Flows, `transformIntoDays` to group entries by date and slice into time bands, `MutableStateFlow` for selected timestamp, log functions with check-then-update-or-insert logic, and activity CRUD functions (add, update, remove)
- Full UI component tree built bottom-up with Compose: `HourBox` → `PartOfDayRow` → `DaySection` → `HoursView` → `NowWhatScreen`, plus `TopBar`, `EntryPanel`, `PresetButton`, and `ActivityLegend`
- Hour selection: tapping any HourBox sets the selected timestamp, EntryPanel updates to show the selected time slot, selected box shows elevated shadow
- Logging: tapping a preset button sets the planned or actual activity for the selected hour, with upsert to avoid duplicates
- Default activities seeded on first launch (Work, Sleep, Gym, Social, Dating)
- State-based navigation between main screen and settings screen
- Settings screen: full activity management — add new activities, rename via inline text field (saves on focus loss), change colour via tappable swatch palette, delete activities
- Visual polish: rounded corners on HourBox, shadow-based selection indicator, proper Material vector drawable icons (settings, close, clock, delete, add), custom colour theme defined in `ui/theme/Color.kt`, gradient fade effects on HoursView edges and EntryPanel button rows, filled vs outlined PresetButtons for actual vs planned, EntryPanel pinned via Scaffold bottomBar slot, consistent spacing and typography
- App runs on physical device over wireless ADB

## TODO

- [x] Redesign the data model to the preset-based schema (an `Activity` table; planned + actual activity per hour)
- [x] Build the new UI scaffolding bottom-up with stand-in data and `@Preview`: `HourBox` → `PartOfDayRow` → `DaySection` → `HoursView`, plus `TopBar`, `EntryPanel`, `PresetButton`, and `Legend`
- [x] Wire the scaffolded UI to the ViewModel and Room
- [x] Settings screen: manage preset activities (add, edit, delete) and their colours — accessible from the settings icon and the Edit button in EntryPanel
- [x] Visual polish: rounded corners on HourBox, proper Material icons, consistent spacing and typography, app theming, gradient fade effects
- [x] Auto-populate today's date in the HoursView on launch so the user doesn't have to log an entry to see the current day
- [x] Prevent logging "actual" activities for hours in the future
- [ ] Default schedule: settings to define recurring planned activities (e.g. working hours, sleep schedule) that auto-populate new days
- [ ] Danger zone in settings: clear all data / reset database
- [ ] More style decisions: Font, font size, colour scheme, night mode colours, etc
- [ ] Hourly notification engine: exact alarms + reschedule-on-fire + boot receiver + `POST_NOTIFICATIONS` permission + notification channel (plus inline direct-reply if feasible)
- [ ] CSV export/import via the system share sheet
- [ ] Settings: configure the time bands (Night / Morning / Day / Evening boundaries)

Placeholder assumptions (all configurable later in Settings): the time bands are four 6-hour blocks (Night 00:00–06:00, Morning 06:00–12:00, Day 12:00–18:00, Evening 18:00–24:00), and the starter presets are Work, Sleep, Gym, Social, and Dating.
