package org.isoron.uhabits.activities.habits.list.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.text.LineBreaker.BREAK_STRATEGY_BALANCED
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.common.views.RingView
import org.isoron.uhabits.core.models.HabitGroup
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.core.ui.screens.habits.list.ListHabitsBehavior
import org.isoron.uhabits.inject.ActivityContext
import org.isoron.uhabits.utils.currentTheme
import org.isoron.uhabits.utils.dp
import org.isoron.uhabits.utils.sres

@SuppressLint("ViewConstructor")
class HabitGroupCardView(
    @ActivityContext context: Context,
    private val behavior: ListHabitsBehavior
) : FrameLayout(context),
    ModelObservable.Listener {

    var dataOffset = 0

    var habitGroup: HabitGroup? = null
        set(newHabitGroup) {
            if (isAttachedToWindow) {
                field?.observable?.removeListener(this)
                field?.parent?.observable?.removeListener(this)
                newHabitGroup?.observable?.addListener(this)
                newHabitGroup?.parent?.observable?.addListener(this)
            }
            field = newHabitGroup
            if (newHabitGroup != null) copyAttributesFrom(newHabitGroup)
            addButtonView.habitGroup = newHabitGroup
            collapseButtonView.habitGroup = newHabitGroup
        }

    var score
        get() = scoreRing.getPercentage().toDouble()
        set(value) {
            scoreRing.setPercentage(value.toFloat())
            scoreRing.setPrecision(1.0f / 16)
        }

    val dragHandleView: android.widget.ImageView
    var addButtonView: AddButtonView
    var collapseButtonView: CollapseButtonView
    private var innerFrame: android.view.View
    private var label: TextView
    private var scoreRing: RingView
    private var groupIndicator: android.widget.ImageView

    private var currentToggleTaskId = 0

    init {
        android.view.LayoutInflater.from(context).inflate(R.layout.item_habit_group_card, this, true)

        dragHandleView = findViewById(R.id.drag_handle)
        scoreRing = findViewById(R.id.score_ring)
        groupIndicator = findViewById(R.id.group_icon)
        label = findViewById(R.id.group_label)
        addButtonView = findViewById(R.id.add_button)
        collapseButtonView = findViewById(R.id.collapse_button)

        addButtonView.habitGroup = habitGroup
        collapseButtonView.habitGroup = habitGroup

        innerFrame = getChildAt(0)
        innerFrame.setOnTouchListener { v, event ->
            v.background?.setHotspot(event.x, event.y)
            false
        }

        clipToPadding = false
        layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    override fun onModelChange() {
        Handler(Looper.getMainLooper()).post {
            habitGroup?.let { copyAttributesFrom(it) }
        }
    }

    override fun setSelected(isSelected: Boolean) {
        super.setSelected(isSelected)
        updateBackground(isSelected)
    }

    fun showDragHandle(show: Boolean) {
        dragHandleView.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        habitGroup?.observable?.addListener(this)
        habitGroup?.parent?.observable?.addListener(this)
    }

    override fun onDetachedFromWindow() {
        habitGroup?.observable?.removeListener(this)
        habitGroup?.parent?.observable?.removeListener(this)
        super.onDetachedFromWindow()
    }

    private fun copyAttributesFrom(hgr: HabitGroup) {
        // Use the original (parent) if it exists, otherwise fallback to the clone
        val source = hgr.parent ?: hgr

        fun getActiveColor(hgr: HabitGroup): Int {
            return when (hgr.isArchived) {
                true -> sres.getColor(R.attr.contrast60)
                false -> currentTheme().color(hgr.color).toInt()
            }
        }

        val c = getActiveColor(source)
        label.apply {
            text = source.name
            setTextColor(c)
        }
        groupIndicator.apply {
            setColorFilter(c)
        }
        scoreRing.apply {
            setColor(c)
        }

        collapseButtonView.setCollapsedWithoutAnimation(source.collapsed)

        if (collapseButtonView.collapsed) {
            addButtonView.visibility = GONE
        } else {
            addButtonView.visibility = VISIBLE
        }
    }

    private fun updateBackground(isSelected: Boolean) {
        val background = when (isSelected) {
            true -> R.drawable.glass_selected
            false -> R.drawable.glass_ripple
        }
        innerFrame.setBackgroundResource(background)
    }

    companion object {
        fun (() -> Unit).delay(delayInMillis: Long) {
            Handler(Looper.getMainLooper()).postDelayed(this, delayInMillis)
        }
    }
}
