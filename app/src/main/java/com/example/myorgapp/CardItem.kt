package com.example.myorgapp

data class CardItem(
    val id: Long = 0L,
    val name: String = "",
    val description: String = "",
    val dateCreated: String? = null,
    val dateCompleted: String? = null,
    val finished: Boolean = false,
    val taskSetTimeStart: String? = null,
    val taskSetTimeEnd: String? = null,
    val reminderMinutesBefore: Int? = null,
    val reminderCustomTime: String? = null,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatDaysOfWeek: List<Int>? = null,
    val repeatEndDate: String? = null,
    val repeatSkipDates: String? = null
)
