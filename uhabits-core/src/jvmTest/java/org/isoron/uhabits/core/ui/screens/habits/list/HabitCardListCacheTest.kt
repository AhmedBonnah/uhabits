/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.isoron.uhabits.core.ui.screens.habits.list

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.isoron.platform.time.LocalDate
import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.commands.CreateRepetitionCommand
import org.isoron.uhabits.core.commands.DeleteHabitGroupsCommand
import org.isoron.uhabits.core.commands.DeleteHabitsCommand
import org.isoron.uhabits.core.models.Entry
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times

class HabitCardListCacheTest : BaseUnitTest() {
    private lateinit var cache: HabitCardListCache
    private lateinit var listener: HabitCardListCache.Listener
    var today = LocalDate(2015, 1, 25)

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        habitList.removeAll()
        for (i in 0..9) {
            if (i == 3) habitList.add(fixtures.createLongHabit()) else habitList.add(fixtures.createShortHabit())
        }
        habitGroupList.removeAll()
        for (i in 0..2) {
            val hgr = groupFixtures.createEmptyHabitGroup(id = 10L + i.toLong())
            habitGroupList.add(hgr)
            hgr.position = 10 + i
        }
        habitGroupList.update(emptyList())
        cache = HabitCardListCache(habitList, habitGroupList, commandRunner, taskRunner, mock())
        cache.setCheckmarkCount(10)
        cache.refreshAllHabits()
        val hgr = habitGroupList.getByPosition(2)
        val h = fixtures.createShortHabit()
        hgr.habitList.add(h)
        h.id = 13L
        h.groupId = hgr.id
        h.group = hgr
        h.groupUUID = hgr.uuid
        cache.refreshAllHabits()
        cache.onAttached()
        listener = mock()
        cache.setListener(listener)
    }

    override fun tearDown() {
        cache.onDetached()
    }

    @Test
    fun testCommandListener_delete_habit() {
        assertThat(cache.habitCount, equalTo(10))
        val h = habitList.getByPosition(0)
        commandRunner.run(
            DeleteHabitsCommand(habitList, habitGroupList, listOf(h))
        )
        assertThat(cache.habitCount, equalTo(9))
    }

    @Test
    fun testCommandListener_delete_hgr() {
        assertThat(cache.habitGroupCount, equalTo(3))
        assertThat(cache.subHabitCount, equalTo(1))
        val hgr = habitGroupList.getByPosition(0)
        commandRunner.run(
            DeleteHabitGroupsCommand(habitGroupList, listOf(hgr))
        )
        assertThat(cache.habitGroupCount, equalTo(2))
        assertThat(cache.subHabitCount, equalTo(1))

        val hgr2 = habitGroupList.getByPosition(1)
        commandRunner.run(
            DeleteHabitGroupsCommand(habitGroupList, listOf(hgr2))
        )
        assertThat(cache.habitGroupCount, equalTo(1))
        assertThat(cache.subHabitCount, equalTo(0))
    }

    @Test
    fun testCommandListener_single() {
        val h2 = habitList.getByPosition(2)
        commandRunner.run(CreateRepetitionCommand(habitList, h2, today, Entry.NO, ""))
    }

    @Test
    fun testCommandListener_single_sub_habit() {
        val hgr2 = habitGroupList.getByPosition(2)
        val h2 = hgr2.habitList.getByPosition(0)
        commandRunner.run(CreateRepetitionCommand(hgr2.habitList, h2, today, Entry.NO, ""))
    }

    @Test
    fun testGet() {
        assertThat(cache.habitCount, equalTo(10))
        val h = habitList.getByPosition(3)
        val score = h.scores[today].value
        assertThat(cache.getHabitByPosition(3), equalTo(h))
        assertThat(cache.getScore(h.id!!), equalTo(score))
        val actualCheckmarks = cache.getCheckmarks(h.id!!)

        val expectedCheckmarks = h
            .computedEntries
            .getByInterval(today.minus(9), today)
            .map { it.value }.toIntArray()
        assertThat(actualCheckmarks, equalTo(expectedCheckmarks))
    }

    @Test
    fun testGetGroup() {
        assertThat(cache.habitGroupCount, equalTo(3))
        val hgr = habitGroupList.getByPosition(2)
        val score = hgr.scores[today].value
        assertThat(cache.getHabitGroupByPosition(12), equalTo(hgr))
        assertThat(cache.getScore(hgr.id!!), equalTo(score))

        val h = hgr.habitList.getByPosition(0)
        val score2 = h.scores[today].value
        assertThat(cache.getHabitByPosition(13), equalTo(h))
        assertThat(cache.getScore(h.id!!), equalTo(score2))
        val actualCheckmarks = cache.getCheckmarks(h.id!!)

        val expectedCheckmarks = h
            .computedEntries
            .getByInterval(today.minus(9), today)
            .map { it.value }.toIntArray()
        assertThat(actualCheckmarks, equalTo(expectedCheckmarks))
    }

    @Test
    fun testRemoval() {
        removeHabitAt(0)
        removeHabitAt(3)
        removeHabitGroupAt(2)
        cache.refreshAllHabits()
        assertThat(cache.habitCount, equalTo(8))
        assertThat(cache.habitGroupCount, equalTo(2))
    }

    @Test
    fun testRefreshWithNoChanges() {
        cache.refreshAllHabits()
    }

    @Test
    fun testReorder_onCache() {
        val h2 = cache.getHabitByPosition(2)
        val h3 = cache.getHabitByPosition(3)
        val h7 = cache.getHabitByPosition(7)
        cache.reorder(2, 7)
        assertThat(cache.getHabitByPosition(2), equalTo(h3))
        assertThat(cache.getHabitByPosition(7), equalTo(h2))
        assertThat(cache.getHabitByPosition(6), equalTo(h7))
    }

    @Test
    fun testReorder_onList() {
        val h2 = habitList.getByPosition(2)
        val h3 = habitList.getByPosition(3)
        val h7 = habitList.getByPosition(7)
        assertThat(cache.getHabitByPosition(2), equalTo(h2))
        assertThat(cache.getHabitByPosition(7), equalTo(h7))
        reset(listener)
        habitList.reorder(h2, h7)
        cache.refreshAllHabits()
        assertThat(cache.getHabitByPosition(2), equalTo(h3))
        assertThat(cache.getHabitByPosition(7), equalTo(h2))
        assertThat(cache.getHabitByPosition(6), equalTo(h7))
    }

    private fun removeHabitAt(position: Int) {
        val h = habitList.getByPosition(position)
        habitList.remove(h)
    }

    private fun removeHabitGroupAt(position: Int) {
        val hgr = habitGroupList.getByPosition(position)
        habitGroupList.remove(hgr)
    }
}
