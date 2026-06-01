package com.example.myorgapp

data class CardReminder(
    val id: String = java.util.UUID.randomUUID().toString(),
    val minutesBefore: Int? = null,
    val customTime: String? = null
)
