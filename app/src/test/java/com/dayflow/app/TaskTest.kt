package com.dayflow.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TaskTest {
    private val day = LocalDate.of(2026, 9, 28)
    private fun habit(minute: Int = 420) = Task(title = "Stretch", section = Section.ROUTINE, minute = minute, date = day)

    @Test fun habitsRepeatFromTheirStartDate() {
        assertFalse(habit().occursOn(day.minusDays(1)))
        assertTrue(habit().occursOn(day))
        assertTrue(habit().occursOn(day.plusDays(100)))
    }
    @Test fun completionDoesNotCarryIntoTomorrow() {
        val completed = habit().toggle(day)
        assertTrue(completed.isDone(day))
        assertFalse(completed.isDone(day.plusDays(1)))
        assertFalse(completed.toggle(day).isDone(day))
    }
    @Test fun eventsAndWorkOnlyAppearOnTheirScheduledDate() {
        listOf(Section.FOCUS, Section.EVENTS).forEach {
            val task = habit().copy(section = it)
            assertTrue(task.occursOn(day))
            assertFalse(task.occursOn(day.plusDays(1)))
        }
    }
    @Test fun timelineFiltersAndSortsChronologically() {
        val early = habit(420)
        val late = habit(1200).copy(title = "Skincare")
        val event = habit(300).copy(section = Section.EVENTS)
        assertEquals(listOf(early, late), tasksForDay(listOf(late, event, early), Section.ROUTINE, day))
    }
    @Test fun separateDaysRetainTheirOwnHistory() {
        val task = habit().toggle(day).toggle(day.plusDays(1)).toggle(day)
        assertFalse(task.isDone(day))
        assertTrue(task.isDone(day.plusDays(1)))
    }
}
