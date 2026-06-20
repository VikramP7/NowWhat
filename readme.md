*<div align="right"> Vikram Procter | June 19, 2026 </div>*

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
- **Hours view** (scrollable) — one section per day. Each day has **four rows**: Night, Morning, Day, and Evening. Each row holds one **box per hour** in that band. Every box is:
  - **outlined** in the colour of the activity you *planned* for that hour, and
  - **filled** with the colour of the activity you *actually* did.

  So a box outlined amber but filled teal means "planned to gym, ended up working." Outline-only means planned but not yet done; an empty box means nothing is logged yet.
- **Entry panel** (bottom) — defaults to logging the *next* hour from the preset buttons. Tap any box above to edit that hour instead. Includes a **colour legend** mapping each colour to its activity.

## How it works (planned)

Hourly **notifications** fire on the hour (using exact alarms) prompting you to log the hour just passed and plan the hour ahead — ideally answerable straight from the notification.

## Tech stack

- **Kotlin** + **Jetpack Compose** (declarative UI)
- **Room** (SQLite) for persistence, processed via **KSP**
- **ViewModel** + **Kotlin Flow** for reactive state
- **AlarmManager** (exact alarms) for on-the-hour notifications
- Built in Android Studio; tested on a physical device over wireless ADB

## Architecture

Data flows in one direction: **Entity → DAO → Database → ViewModel → Composable**. Reads are exposed as reactive `Flow`s, so the UI updates itself whenever the data changes.

UI component tree:

- `HourTrackerScreen` — owns the top bar and splits the screen into the two sections
  - `TopBar` — title + hamburger menu
  - `HoursView` — scrollable `LazyColumn` of days
    - `DaySection` — date label + four part-of-day rows
      - `PartOfDayRow` — band label + a row of boxes
        - `HourBox` — one hour: outline = planned colour, fill = actual colour; tappable to edit
  - `EntryPanel` — bottom section for logging the next hour
    - `PresetButton` — one button per preset activity
    - `Legend` — colour → activity key

## Data model (in transition)

Currently a single free-text table. Being redesigned to:

- **Activity** — a preset: a name and a colour.
- **HourEntry** — one hour: a timestamp, a *planned* Activity, and an *actual* Activity (both optional).

## Status

Done:

- Development environment installed and project created
- Room toolchain wired up (Room 2.8.4 + KSP)
- Data layer: `HourEntry` entity, `HourEntryDao`, `AppDatabase` (singleton), `HourViewModel`
- A working v1 screen: text-field entry with a persistent, reactive list

## TODO

- [ ] Redesign the data model to the preset-based schema (an `Activity` table; planned + actual activity per hour)
- [ ] Build the new UI scaffolding bottom-up with stand-in data and `@Preview`: `HourBox` → `PartOfDayRow` → `DaySection` → `HoursView`, plus `TopBar`, `EntryPanel`, `PresetButton`, and `Legend`
- [ ] Wire the scaffolded UI to the ViewModel and Room
- [ ] Settings screen: configure the time bands (Night / Morning / Day / Evening boundaries) and manage preset activities and their colours
- [ ] Hourly notification engine: exact alarms + reschedule-on-fire + boot receiver + `POST_NOTIFICATIONS` permission + notification channel (plus inline direct-reply if feasible)
- [ ] CSV export via the system share sheet

Placeholder assumptions (all configurable later in Settings): the time bands are four 6-hour blocks (Night 00:00–06:00, Morning 06:00–12:00, Day 12:00–18:00, Evening 18:00–24:00), and the starter presets are Work, Sleep, Gym, Social, and Dating.