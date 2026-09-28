package com.dayflow.app

import java.time.LocalDate
import java.util.UUID

enum class Section(val label: String, val subtitle: String) {
    ROUTINE("Daily routine", "Small steps. A better everyday."),
    FOCUS("Work & study", "Make space for what matters."),
    EVENTS("Appointments", "Be there for life's plans.")
}

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val section: Section,
    val minute: Int,
    val duration: Int = 15,
    val date: LocalDate = LocalDate.now(),
    val category: String = "Personal",
    val notes: String = "",
    val location: String = "",
    val completedDates: Set<LocalDate> = emptySet()
) {
    fun occursOn(day: LocalDate) = if (section == Section.ROUTINE) day >= date else day == date
    fun isDone(day: LocalDate) = day in completedDates
    fun toggle(day: LocalDate) = copy(completedDates = if (isDone(day)) completedDates - day else completedDates + day)
}

fun tasksForDay(tasks: List<Task>, section: Section, day: LocalDate): List<Task> =
    tasks.filter { it.section == section && it.occursOn(day) }
        .sortedWith(compareBy<Task> { it.minute }.thenBy { it.title.lowercase() })

fun sampleTasks(day: LocalDate) = listOf(
    Task(title = "Wake up & stretch", section = Section.ROUTINE, minute = 420, duration = 10, date = day, category = "Wellbeing", notes = "A little movement to start the day."),
    Task(title = "Brush teeth", section = Section.ROUTINE, minute = 435, duration = 3, date = day, category = "Hygiene"),
    Task(title = "Shower & skincare", section = Section.ROUTINE, minute = 440, duration = 20, date = day, category = "Hygiene"),
    Task(title = "Evening wind-down", section = Section.ROUTINE, minute = 1290, duration = 20, date = day, category = "Wellbeing"),
    Task(title = "Plan a focused study session", section = Section.FOCUS, minute = 540, duration = 60, date = day, category = "Study"),
    Task(title = "Tidy your space", section = Section.FOCUS, minute = 1080, duration = 20, date = day, category = "Chores")
)
