/*
 * Copyright (C) 2016-2026 Álinson Santos Xavier <git@axavier.org>
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

package org.isoron.uhabits.core.database.migrations

import org.isoron.uhabits.core.BaseUnitTest
import org.isoron.uhabits.core.database.Database
import org.isoron.uhabits.core.database.MigrationHelper
import org.isoron.uhabits.core.models.sqlite.SQLModelFactory
import org.isoron.uhabits.core.preferences.WidgetPreferences
import org.isoron.uhabits.core.test.HabitFixtures
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals

class Version27Test : BaseUnitTest() {

    private lateinit var db: Database

    private lateinit var helper: MigrationHelper

    private val widgetPreferences: WidgetPreferences = mock()

    override fun setUp() {
        super.setUp()
        db = openDatabaseResource("/databases/022.db")
        helper = MigrationHelper(db)
        modelFactory = SQLModelFactory(db, widgetPreferences)
        habitList = (modelFactory as SQLModelFactory).buildHabitList()
        fixtures = HabitFixtures(modelFactory, habitList)
    }

    private fun migrateTo27() = helper.migrateTo(27)

    @Test
    fun `test migrate to 27 keeps all habit groups`() {
        helper.migrateTo(26)
        db.execute("PRAGMA user_version = 26")

        // Add a mock group before migration
        db.execute("insert into habitgroups (id, name, description, color, archived, position, question, uuid, reminder_hour, reminder_min, reminder_days) values (1, 'Test Group', '', 1, 0, 1, '', 'uuid1', 0, 0, 0)")

        var cursor = db.query("select name from habitgroups")
        val namesBefore = mutableListOf<String?>()
        while (cursor.moveToNext()) {
            namesBefore.add(cursor.getString(0))
        }

        migrateTo27()

        cursor = db.query("select name, collapsed from habitgroups")
        val namesAfter = mutableListOf<String?>()
        val collapsedStates = mutableListOf<Int?>()
        while (cursor.moveToNext()) {
            namesAfter.add(cursor.getString(0))
            collapsedStates.add(cursor.getInt(1))
        }

        assertEquals(namesBefore, namesAfter)
        assertEquals(listOf<Int?>(0), collapsedStates)
    }
}
