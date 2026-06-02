package com.example.myorgapp

data class FeedbackEntry(
    val date: String,
    val priority: Int
)

data class CardItem(
    val id: Long = 0L,
    val name: String = "",
    val description: String = "",
    val dateCreated: String? = null,
    val dateCompleted: String? = null,
    val finished: Boolean = false,
    val taskSetTimeStart: String? = null,
    val taskSetTimeEnd: String? = null,
    val reminders: List<CardReminder> = emptyList(),
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatDaysOfWeek: List<Int>? = null,
    val repeatEndDate: String? = null,
    val repeatSkipDates: String? = null,
    val repeatCompletionCount: Int = 0,
    val repeatCustomFrequency: String = "weekly",
    val repeatDayOfMonth: Int? = null,
    val repeatMonth: Int? = null,
    val checklist: List<ChecklistItem> = emptyList(),
    val tagIds: List<String> = emptyList(),
    val priority: Int = 0,
    val snoozed: Boolean = false,
    val trashed: Boolean = false,
    val feedbackHistory: List<FeedbackEntry> = emptyList()
)
