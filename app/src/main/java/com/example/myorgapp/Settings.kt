package com.example.myorgapp

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class ColorTheme {
    BLUE, PINK
}

enum class AppLanguage {
    ENGLISH, CROATIAN
}

data class Settings(
    val dayStartsHour: Int = 3,
    val dayStartsMinute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.BLUE,
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0,
    val language: AppLanguage = AppLanguage.ENGLISH
)
