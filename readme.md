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

- **Top bar** — the app name, and a hamburger menu (top right) that opens Settings.
- **Hours view** (scrollable) — one section per day. Each day has **four rows**: Morning, Day, Evening, and Night. Each row holds one **box per hour** in that band. Every box is:
  - **outlined** in the colour of the activity you *planned* for that hour, and
  - **filled** with the colour of the activity you *actually* did.

  So a box outlined amber but filled teal means "planned to gym, ended up working." Outline-only means planned but not yet done; an empty box means nothing is logged yet.
- **Entry panel** (bottom) — shows the currently selected time slot. Displays preset buttons in two rows: "What's planned?" and "What's happened?". Tap any box in the hours view to select that hour for editing. Includes a **colour legend** mapping each colour to its activity.

## How it works (planned)

Hourly **notifications** fire on the hour (using exact alarms) prompting you to log the hour just passed and plan the hour ahead — ideally answerable straight from the notification.

## Tech stack

- **Kotlin** + **Jetpack Compose** (declarative UI)
- **Room** (SQLite) for persistence, processed via **KSP**
- **ViewModel** + **Kotlin Flow** + **StateFlow** for reactive state
- **AlarmManager** (exact alarms) for on-the-hour notifications (planned)
- Built in Android Studio; tested on a physical device over wireless ADB

## Architecture

Data flows in one direction: **Entity → DAO → Database → ViewModel → Composable**. Reads are exposed as reactive `Flow`s and `StateFlow`s, so the UI updates itself whenever the data changes. Events flow upward via callback lambdas (state hoisting).

UI component tree:

- `NowWhatScreen` — owns the Scaffold, TopBar, and splits the screen into two sections
  - `TopBar` — title + hamburger menu (placeholder icon)
  - `HoursView` — scrollable `LazyColumn` of days
    - `DaySection` — date label + four part-of-day rows
      - `PartOfDayRow` — band label + a row of boxes
        - `HourBox` — one hour: outline = planned colour, fill = actual colour; tappable to select
  - `EntryPanel` — bottom section showing selected time and logging controls
    - `PresetButton` — one button per preset activity (outlined, coloured)
    - `ActivityLegend` — colour → activity key

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
- Data layer: `Activity` entity + `ActivityDao`, `HourEntry` entity (preset-based with planned/actual activity IDs) + `HourEntryDao` (with upsert pattern), `AppDatabase` (singleton with destructive migration fallback)
- `NowWhatViewModel` with `combine` to merge entries and activities Flows, `transformIntoDays` to group entries by date and slice into time bands, `MutableStateFlow` for selected timestamp, and log functions with check-then-update-or-insert logic
- Full UI component tree built bottom-up with Compose: `HourBox` → `PartOfDayRow` → `DaySection` → `HoursView` → `NowWhatScreen`, plus `TopBar`, `EntryPanel`, `PresetButton`, and `ActivityLegend`
- Hour selection: tapping any HourBox sets the selected timestamp, EntryPanel updates to show the selected time slot
- Logging: tapping a preset button sets the planned or actual activity for the selected hour, with upsert to avoid duplicates
- Default activities seeded on first launch (Work, Sleep, Gym, Social, Dating)
- App runs on physical device over wireless ADB

## TODO

- [x] Redesign the data model to the preset-based schema (an `Activity` table; planned + actual activity per hour)
- [x] Build the new UI scaffolding bottom-up with stand-in data and `@Preview`: `HourBox` → `PartOfDayRow` → `DaySection` → `HoursView`, plus `TopBar`, `EntryPanel`, `PresetButton`, and `Legend`
- [x] Wire the scaffolded UI to the ViewModel and Room
- [ ] Settings screen: configure the time bands (Night / Morning / Day / Evening boundaries) and manage preset activities (add, edit, delete) and their colours — accessible from the hamburger menu and the Edit button in EntryPanel
- [ ] Visual polish: rounded corners on HourBox, gradient/glow borders, proper Material icons (clock, hamburger), consistent spacing and typography, app theming
- [ ] Hourly notification engine: exact alarms + reschedule-on-fire + boot receiver + `POST_NOTIFICATIONS` permission + notification channel (plus inline direct-reply if feasible)
- [ ] CSV export via the system share sheet

Placeholder assumptions (all configurable later in Settings): the time bands are four 6-hour blocks (Night 00:00–06:00, Morning 06:00–12:00, Day 12:00–18:00, Evening 18:00–24:00), and the starter presets are Work, Sleep, Gym, Social, and Dating.
