package com.example.myorgapp

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class Settings(
    val dayStartsHour: Int = 3,
    val dayStartsMinute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultReminderHour: Int = 9,
    val defaultReminderMinute: Int = 0
)
