# MO

A local-first personal organizer app for Android. No accounts, no cloud, no tracking.

## Features

- **Cards** — create tasks/notes with name, description, date, time, and reminders (up to 5 per card with individual alarms); check sound plays on completion
- **Checklists** — add checkbox items to any card; long-press to reorder; completion is blocked until all items are checked
- **Repeating tasks** — daily, weekly, monthly, yearly, weekdays, weekends, or custom days of week; completion counter tracks how many times you've done it
- **Tags** — predefined colored tags to organize cards; filter by tag in the active view
- **Search** — search by name, description, or checklist text
- **Duplicate from existing** — typing in the name field suggests existing cards; tap one to copy all fields
- **Calendar views** — day, week, and month views of all your cards
- **Yesterday review** — uncompleted yesterday's cards are shown in a dialog when you open the app
- **Notifications** — tap opens main screen with 2-second card highlight; "Done" button completes silently
- **Streak tracking** — consecutive day completion streak displayed on main screen
- **Statistics** — view total created, completed, completion rate, active tasks, overdue count, daily/weekly completions charts, and breakdowns by tag and priority
- **Card sort** — toggle between auto sort (time-based) and custom order with up/down arrows
- **Themes** — Blue (default) or Pink; Light/Dark/System mode independent
- **No accounts, no network, no tracking** — everything stays on your device

## Installation

1. Go to the [Releases page](https://github.com/bgjelic/MyOrgApp/releases)
2. Tap the `.apk` file to download it
3. Open the downloaded file and tap "Install" (you may need to enable "Install from unknown sources" in Settings)

## Building from source

Open the project in Android Studio and run on your device or emulator.

## Data safety

All data is stored locally on your device using SharedPreferences. No data is sent anywhere. If you uninstall the app, all data is deleted. Back up the app data through your device's built-in backup system.

## Tech stack

- Kotlin + Jetpack Compose
- Material 3
- Navigation Compose
- Gson for local persistence (SharedPreferences)
- No backend, no database, no third-party analytics

## License

MIT
