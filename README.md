# MO

A local-first personal organizer app for Android. No accounts, no cloud, no tracking.

## Features

- **Cards** — create tasks/notes with name, description, date, time, and reminders
- **Checklists** — add checkbox items to any card; completion is blocked until all items are checked
- **Repeating tasks** — daily, weekly, monthly, yearly, weekdays, weekends, or custom days of week; completion counter tracks how many times you've done it
- **Tags** — predefined colored tags to organize cards; filter by tag in the active view
- **Search** — search by name, description, or checklist text
- **Duplicate from existing** — typing in the name field suggests existing cards; tap one to copy all fields
- **Calendar views** — day, week, and month views of all your cards
- **Yesterday review** — uncompleted yesterday's cards are shown in a dialog when you open the app
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

## Troubleshooting

**App shows "MO keeps stopping" or ANR dialog on first launch** — This can happen on slow devices or emulators. The app composes its UI on the first frame which may take a few seconds. A future update will add a splash screen to prevent this. For now, tapping "Wait" should work after the initial load.

## Tech stack

- Kotlin + Jetpack Compose
- Material 3
- Navigation Compose
- Gson for local persistence (SharedPreferences)
- No backend, no database, no third-party analytics

## License

MIT
