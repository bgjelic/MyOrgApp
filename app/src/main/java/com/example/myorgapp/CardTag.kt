package com.example.myorgapp

import androidx.compose.ui.graphics.Color

data class CardTag(
    val id: String,
    val name: String,
    val colorIndex: Int
)

val tagPalette: List<Color> = listOf(
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFFFDD835),
    Color(0xFF43A047),
    Color(0xFF1E88E5),
    Color(0xFF8E24AA),
    Color(0xFF00ACC1),
    Color(0xFF6D4C41),
)

val tagPaletteNames: List<String> = listOf(
    "Red", "Orange", "Yellow", "Green", "Blue", "Purple", "Cyan", "Brown"
)
