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
package org.isoron.uhabits

import android.app.Activity
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class BaseExceptionHandler(private val activity: Activity) : Thread.UncaughtExceptionHandler {

    private val originalHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread?, ex: Throwable?) {
        if (ex == null) return
        if (thread == null) return
        try {
            ex.printStackTrace()
            val reporter = AndroidBugReporter(activity)
            reporter.dumpBugReportToFile()

            // Write a "last_crash.txt" so the app can detect and surface it on next launch
            val sw = StringWriter()
            ex.printStackTrace(PrintWriter(sw))
            val crashMsg = buildString {
                appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Thread: ${thread.name}")
                appendLine()
                append(sw.toString())
                appendLine()
                appendLine("--- Logcat ---")
                try {
                    append(reporter.getLogcat())
                } catch (ignored: Exception) {}
            }
            val logDir = AndroidDirFinder(activity).getFilesDir("Logs")
            if (logDir != null) {
                File(logDir, "last_crash.txt").writeText(crashMsg)
            }

            try {
                val clipboard = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Crash Log", crashMsg)
                clipboard.setPrimaryClip(clip)
            } catch (ignored: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
        originalHandler?.uncaughtException(thread, ex)
    }
}
