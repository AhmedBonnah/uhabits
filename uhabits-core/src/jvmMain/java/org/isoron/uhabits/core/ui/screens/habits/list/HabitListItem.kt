package org.isoron.uhabits.core.ui.screens.habits.list

import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitGroup

sealed class HabitListItem {
    abstract val id: Long

    data class GroupItem(val group: HabitGroup) : HabitListItem() {
        override val id: Long = group.id ?: 0L
    }

    data class HabitCardItem(
        val habit: Habit,
        val checkmarks: IntArray,
        val score: Double,
        val notes: Array<String>
    ) : HabitListItem() {
        override val id: Long = habit.id ?: 0L

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as HabitCardItem

            if (habit != other.habit) return false
            if (!checkmarks.contentEquals(other.checkmarks)) return false
            if (score != other.score) return false
            if (!notes.contentEquals(other.notes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = habit.hashCode()
            result = 31 * result + checkmarks.contentHashCode()
            result = 31 * result + score.hashCode()
            result = 31 * result + notes.contentHashCode()
            return result
        }
    }
}
