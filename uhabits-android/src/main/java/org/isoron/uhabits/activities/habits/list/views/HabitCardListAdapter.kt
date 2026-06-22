package org.isoron.uhabits.activities.habits.list.views

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import me.tatarka.inject.annotations.Inject
import org.isoron.uhabits.activities.habits.list.MAX_CHECKMARK_COUNT
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.HabitGroup
import org.isoron.uhabits.core.models.HabitList
import org.isoron.uhabits.core.models.HabitMatcher
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.core.ui.screens.habits.list.HabitCardListCache
import org.isoron.uhabits.core.ui.screens.habits.list.HabitListItem
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsMenuBehavior
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsSelectionMenuBehavior
import org.isoron.uhabits.core.utils.MidnightTimer
import org.isoron.uhabits.inject.ActivityScope
import java.util.LinkedList

@Inject
@ActivityScope
class HabitCardListAdapter(
    private val cache: HabitCardListCache,
    private val preferences: Preferences,
    private val midnightTimer: MidnightTimer
) : ListAdapter<HabitListItem, HabitCardViewHolder>(ItemDiffCallback()),
    HabitCardListCache.Listener,
    MidnightTimer.MidnightListener,
    ListHabitsMenuBehavior.Adapter,
    ListHabitsSelectionMenuBehavior.Adapter {

    class ItemDiffCallback : DiffUtil.ItemCallback<HabitListItem>() {
        override fun areItemsTheSame(oldItem: HabitListItem, newItem: HabitListItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HabitListItem, newItem: HabitListItem): Boolean {
            return oldItem == newItem
        }
    }

    val observable: ModelObservable = ModelObservable()
    private var listView: HabitCardListView? = null
    val selectedHabits: LinkedList<Habit> = LinkedList()
    val selectedHabitGroups: LinkedList<HabitGroup> = LinkedList()
    private var lastSortable = false

    override fun atMidnight() {
        cache.refreshAllHabits()
    }

    fun cancelRefresh() {
        cache.cancelTasks()
    }

    fun hasNoHabit(): Boolean {
        return cache.hasNoHabit()
    }

    fun hasNoHabitGroup(): Boolean {
        return cache.hasNoHabitGroup()
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun clearSelection() {
        if (selectedHabits.isEmpty() && selectedHabitGroups.isEmpty()) return
        selectedHabits.clear()
        selectedHabitGroups.clear()
        notifyDataSetChanged()
        observable.notifyListeners()
    }

    override fun getSelectedHabits(): List<Habit> {
        return ArrayList(selectedHabits)
    }

    override fun getSelectedHabitGroups(): List<HabitGroup> {
        return ArrayList(selectedHabitGroups)
    }

    @Deprecated("")
    fun getItemAt(position: Int): Habit? {
        return getHabit(position)
    }

    fun getHabit(position: Int): Habit? {
        val item = currentList.getOrNull(position)
        return (item as? HabitListItem.HabitCardItem)?.habit
    }

    fun getHabitGroup(position: Int): HabitGroup? {
        val item = currentList.getOrNull(position)
        return (item as? HabitListItem.GroupItem)?.group
    }

    override fun getItemId(position: Int): Long {
        return currentList.getOrNull(position)?.id ?: 0L
    }

    val isSelectionEmpty: Boolean
        get() = selectedHabits.isEmpty() && selectedHabitGroups.isEmpty()

    val isSortable: Boolean
        get() = cache.primaryOrder == HabitList.Order.BY_POSITION

    fun onAttached() {
        cache.onAttached()
        midnightTimer.addListener(this)
    }

    override fun onBindViewHolder(holder: HabitCardViewHolder, position: Int) {
        if (listView == null) return
        val item = getItem(position)
        if (item is HabitListItem.HabitCardItem) {
            val habit = item.habit
            val score = item.score
            val checkmarks = item.checkmarks
            val notes = item.notes
            val selected = selectedHabits.contains(habit)
            listView!!.bindCardView(holder, habit, score, checkmarks, notes, selected)
            (holder.itemView as HabitCardView).showDragHandle(isSortable)
        } else if (item is HabitListItem.GroupItem) {
            val habitGroup = item.group
            val score = cache.getScore(habitGroup.id!!)
            val selected = selectedHabitGroups.contains(habitGroup)
            listView!!.bindGroupCardView(holder, habitGroup, score, selected)
            (holder.itemView as HabitGroupCardView).showDragHandle(isSortable)
        }
    }

    override fun onViewAttachedToWindow(holder: HabitCardViewHolder) {
        listView!!.attachCardView(holder)
    }

    override fun onViewDetachedFromWindow(holder: HabitCardViewHolder) {
        listView!!.detachCardView(holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitCardViewHolder {
        if (viewType == 0) {
            val view = listView!!.createHabitCardView()
            return HabitCardViewHolder(view, null)
        } else {
            val view = listView!!.createHabitGroupCardView()
            return HabitCardViewHolder(null, view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) is HabitListItem.HabitCardItem) 0 else 1
    }

    fun onDetached() {
        cache.onDetached()
        midnightTimer.removeListener(this)
    }

    override fun onListUpdated(newList: List<HabitListItem>) {
        val currentSortable = isSortable
        if (currentSortable != lastSortable) {
            lastSortable = currentSortable
            notifyDataSetChanged()
        }
        submitList(newList) {
            observable.notifyListeners()
        }
    }

    override fun onRefreshFinished() {
        // Handled by onListUpdated diffing naturally
    }

    override fun performRemove(selected: List<Habit>) {
        for (habit in selected) cache.remove(habit.id!!)
    }

    override fun performRemoveHabitGroup(selected: List<HabitGroup>) {
        for (hgr in selected) cache.remove(hgr.id!!)
    }

    fun getTopLevelItems(): List<Any> {
        return cache.getTopLevelItems()
    }

    fun performReorder(from: Int, to: Int): Boolean {
        // With DiffUtil we can safely just reorder in the cache and wait for the natural DiffResult.
        cache.reorder(from, to)
        return true
    }

    override fun refresh() {
        cache.refreshAllHabits()
    }

    override fun setFilter(matcher: HabitMatcher) {
        cache.setFilter(matcher)
    }

    fun setListView(listView: HabitCardListView?) {
        this.listView = listView
    }

    override var primaryOrder: HabitList.Order
        get() = cache.primaryOrder
        set(value) {
            cache.primaryOrder = value
            preferences.defaultPrimaryOrder = value
        }

    override var secondaryOrder: HabitList.Order
        get() = cache.secondaryOrder
        set(value) {
            cache.secondaryOrder = value
            preferences.defaultSecondaryOrder = value
        }

    @SuppressLint("NotifyDataSetChanged")
    fun toggleSelection(position: Int) {
        val item = currentList.getOrNull(position)
        if (item is HabitListItem.HabitCardItem) {
            val h = item.habit
            val k = selectedHabits.indexOf(h)
            if (k < 0) selectedHabits.add(h) else selectedHabits.remove(h)
            notifyDataSetChanged()
        } else if (item is HabitListItem.GroupItem) {
            val hgr = item.group
            val k = selectedHabitGroups.indexOf(hgr)
            if (k < 0) selectedHabitGroups.add(hgr) else selectedHabitGroups.remove(hgr)
            notifyDataSetChanged()
        }
    }

    init {
        cache.setListener(this)
        cache.setCheckmarkCount(MAX_CHECKMARK_COUNT)
        cache.secondaryOrder = preferences.defaultSecondaryOrder
        cache.primaryOrder = preferences.defaultPrimaryOrder
        setHasStableIds(true)
    }
}
