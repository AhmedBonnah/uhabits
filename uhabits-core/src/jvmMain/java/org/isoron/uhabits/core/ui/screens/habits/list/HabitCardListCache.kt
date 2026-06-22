package org.isoron.uhabits.core.ui.screens.habits.list

import me.tatarka.inject.annotations.Inject
import org.isoron.platform.time.getToday
import org.isoron.uhabits.core.AppScope
import org.isoron.uhabits.core.commands.Command
import org.isoron.uhabits.core.commands.CommandRunner
import org.isoron.uhabits.core.io.Logging
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitGroup
import org.isoron.uhabits.core.models.HabitGroupList
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.tasks.Task
import org.isoron.uhabits.core.tasks.TaskRunner

@AppScope
@Inject
class HabitCardListCache(
    private val habits: HabitList,
    private val habitGroups: HabitGroupList,
    private val commandRunner: CommandRunner,
    private val taskRunner: TaskRunner,
    logging: Logging
) : CommandRunner.Listener, ModelObservable.Listener {

    private val logger = logging.getLogger("HabitCardListCache")

    interface Listener {
        fun onListUpdated(newList: List<HabitListItem>)
        fun onRefreshFinished()
    }

    private var listener: Listener? = null
    private var checkmarkCount = 7
    private var isAttached = false
    private var filter: HabitMatcher? = null

    @Volatile
    private var currentList: List<HabitListItem> = emptyList()

    var primaryOrder: HabitList.Order = HabitList.Order.BY_POSITION
        set(value) {
            field = value
            refreshAllHabits()
        }

    var secondaryOrder: HabitList.Order = HabitList.Order.BY_SCORE_DESC
        set(value) {
            field = value
            refreshAllHabits()
        }

    val itemCount: Int get() = currentList.size
    val habitCount: Int get() = currentList.count { it is HabitListItem.HabitCardItem && it.habit.groupId == null }
    val habitGroupCount: Int get() = currentList.count { it is HabitListItem.GroupItem }
    val subHabitCount: Int get() = currentList.count { it is HabitListItem.HabitCardItem && it.habit.groupId != null }

    fun hasNoHabit() = habitCount == 0
    fun hasNoHabitGroup() = habitGroupCount == 0

    fun setListener(l: Listener?) { listener = l }
    fun setCheckmarkCount(count: Int) { checkmarkCount = count }

    fun onAttached() {
        if (isAttached) return
        isAttached = true
        habits.observable.addListener(this)
        habitGroups.observable.addListener(this)
        commandRunner.addListener(this)
        refreshAllHabits()
    }

    fun onDetached() {
        isAttached = false
        habits.observable.removeListener(this)
        habitGroups.observable.removeListener(this)
        commandRunner.removeListener(this)
        cancelTasks()
    }

    override fun onModelChange() {
        refreshAllHabits()
    }

    override fun onCommandFinished(command: Command) {
        refreshAllHabits()
    }

    fun setFilter(matcher: HabitMatcher) {
        filter = matcher
        refreshAllHabits()
    }

    private var currentTask: Task? = null

    @Synchronized
    fun refreshAllHabits() {
        if (!isAttached) return

        val task = object : Task {
            private var isCancelled = false
            private var newList: List<HabitListItem> = emptyList()

            override fun cancel() {
                isCancelled = true
            }

            override fun doInBackground() {
                newList = buildList()
            }

            override fun onPostExecute() {
                if (isCancelled) return
                currentList = newList
                listener?.onListUpdated(newList)
                listener?.onRefreshFinished()
            }
        }

        currentTask?.cancel()
        currentTask = task
        taskRunner.execute(task)
    }

    fun cancelTasks() {
        currentTask?.cancel()
        currentTask = null
    }

    private fun buildList(): List<HabitListItem> {
        val filteredHabits = filter?.let { habits.getFiltered(it) } ?: habits
        val filteredGroups = filter?.let { habitGroups.getFiltered(it) } ?: habitGroups

        val result = mutableListOf<HabitListItem>()
        val today = getToday()
        val dateFrom = today.minus(checkmarkCount - 1)

        val topLevelItems = mutableListOf<Any>()
        topLevelItems.addAll(filteredHabits.filter { it.uuid != null && it.id != null })
        topLevelItems.addAll(filteredGroups.filter { it.uuid != null && it.id != null })

        val habitIndices = filteredHabits.withIndex().associate { it.value.id to it.index }
        val groupIndices = filteredGroups.withIndex().associate { it.value.id to it.index }

        topLevelItems.sortWith(
            Comparator { o1, o2 ->
                if (primaryOrder == HabitList.Order.BY_POSITION) {
                    val p1 = if (o1 is Habit) o1.position else (o1 as HabitGroup).position
                    val p2 = if (o2 is Habit) o2.position else (o2 as HabitGroup).position
                    if (p1 != p2) return@Comparator p1.compareTo(p2)
                }
                if (o1.javaClass != o2.javaClass) {
                    if (o1 is HabitGroup) -1 else 1
                } else {
                    if (o1 is Habit && o2 is Habit) {
                        val idx1 = habitIndices[(o1 as Habit).id] ?: 0
                        val idx2 = habitIndices[(o2 as Habit).id] ?: 0
                        idx1.compareTo(idx2)
                    } else if (o1 is HabitGroup && o2 is HabitGroup) {
                        val idx1 = groupIndices[(o1 as HabitGroup).id] ?: 0
                        val idx2 = groupIndices[(o2 as HabitGroup).id] ?: 0
                        idx1.compareTo(idx2)
                    } else {
                        0
                    }
                }
            }
        )

        for (item in topLevelItems) {
            if (item is Habit) {
                result.add(buildHabitCardItem(item, today, dateFrom))
            } else if (item is HabitGroup) {
                result.add(HabitListItem.GroupItem(item))
                if (!item.collapsed) {
                    for (h in item.habitList) {
                        if (h.uuid != null && h.id != null) {
                            result.add(buildHabitCardItem(h, today, dateFrom))
                        }
                    }
                }
            }
        }
        return result
    }

    private fun buildHabitCardItem(habit: Habit, today: org.isoron.platform.time.LocalDate, dateFrom: org.isoron.platform.time.LocalDate): HabitListItem.HabitCardItem {
        val score = habit.scores[today].value
        val checkmarks = IntArray(checkmarkCount) { -1 }
        val notes = Array(checkmarkCount) { "" }

        val entries = habit.computedEntries.getByInterval(dateFrom, today)
        for ((i, entry) in entries.withIndex()) {
            if (i < checkmarkCount) {
                checkmarks[i] = entry.value
                notes[i] = if (entry.notes.isNotEmpty()) "note" else ""
            }
        }

        return HabitListItem.HabitCardItem(habit, checkmarks, score, notes)
    }

    fun getHabitByPosition(position: Int): Habit? {
        val item = currentList.getOrNull(position)
        return (item as? HabitListItem.HabitCardItem)?.habit
    }

    fun getHabitGroupByPosition(position: Int): HabitGroup? {
        val item = currentList.getOrNull(position)
        return (item as? HabitListItem.GroupItem)?.group
    }

    fun getIdByPosition(position: Int): Long? {
        return currentList.getOrNull(position)?.id
    }

    fun getScore(id: Long): Double {
        val item = currentList.find { it.id == id }
        return when (item) {
            is HabitListItem.HabitCardItem -> item.score
            is HabitListItem.GroupItem -> item.group.scores[getToday()].value
            else -> 0.0
        }
    }

    fun getCheckmarks(id: Long): IntArray {
        val item = currentList.find { it.id == id }
        return if (item is HabitListItem.HabitCardItem) item.checkmarks else IntArray(checkmarkCount) { -1 }
    }

    fun getNotes(id: Long): Array<String> {
        val item = currentList.find { it.id == id }
        if (item is HabitListItem.HabitCardItem) {
            return item.notes
        }
        return Array(checkmarkCount) { "" }
    }

    fun getTopLevelItems(): List<Any> {
        return currentList.mapNotNull {
            when (it) {
                is HabitListItem.GroupItem -> it.group
                is HabitListItem.HabitCardItem -> if (it.habit.groupId == null) it.habit else null
            }
        }
    }

    fun getSubHabitCountForGroup(group: HabitGroup): Int {
        return group.habitList.size()
    }

    fun remove(id: Long) {
        val mut = currentList.toMutableList()
        val idx = mut.indexOfFirst { it.id == id }
        if (idx >= 0) {
            mut.removeAt(idx)
            currentList = mut
            listener?.onListUpdated(mut)
        }
    }

    fun reorder(from: Int, to: Int) {
        val mut = currentList.toMutableList()
        if (from in mut.indices && to in mut.indices) {
            val item = mut.removeAt(from)
            mut.add(to, item)
            currentList = mut
            listener?.onListUpdated(mut)
        }
    }
}
