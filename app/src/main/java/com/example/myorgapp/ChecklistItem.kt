package com.example.myorgapp

data class ChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean = false
)
