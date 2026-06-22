package org.isoron.uhabits.activities.habits.list.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.view.View
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.habits.list.ListHabitsActivity
import org.isoron.uhabits.core.models.HabitGroup
import org.isoron.uhabits.core.models.ModelObservable
import org.isoron.uhabits.utils.getFontAwesome
import org.isoron.uhabits.utils.sp
import org.isoron.uhabits.utils.sres
import org.isoron.uhabits.utils.toMeasureSpec

class CollapseButtonView(
    context: Context,
    var habitGroup: HabitGroup?
) : View(context),
    View.OnClickListener,
    ModelObservable.Listener {

    private var drawer = Drawer()

    var collapsed = false

    fun setCollapsedWithoutAnimation(value: Boolean) {
        if (collapsed != value) {
            collapsed = value
            drawer.rotate()
            invalidate()
        }
    }

    init {
        setOnClickListener(this)
    }

    override fun onClick(v: View) {
        val group = habitGroup ?: return
        val target = group.parent ?: group
        val newState = !target.collapsed

        target.collapsed = newState
        this.collapsed = newState

        val component = (context as ListHabitsActivity).component
        component.listHabitsBehavior.onToggleHabitGroupCollapse(target)

        drawer.animateRotation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawer.draw(canvas)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = resources.getDimensionPixelSize(R.dimen.checkmarkHeight)
        val width = resources.getDimensionPixelSize(R.dimen.checkmarkWidth)
        super.onMeasure(
            width.toMeasureSpec(MeasureSpec.EXACTLY),
            height.toMeasureSpec(MeasureSpec.EXACTLY)
        )
    }

    private inner class Drawer {
        private val rect = RectF()
        private val highContrastColor = sres.getColor(R.attr.contrast100)

        private var rotationAngle = if (collapsed) 90f else 0f
        private var offset_y = if (collapsed) 0f else 0.4f
        private var offset_x = if (collapsed) -0.4f else 0f
        private var animator: android.animation.ValueAnimator? = null

        private val paint = TextPaint().apply {
            typeface = getFontAwesome()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        fun rotate() {
            rotationAngle = if (collapsed) 90f else 0f
            offset_y = if (collapsed) 0f else 0.4f
            offset_x = if (collapsed) -0.4f else 0f
        }

        fun animateRotation() {
            animator?.cancel()
            val targetRotation = if (collapsed) 90f else 0f
            val targetOffsetY = if (collapsed) 0f else 0.4f
            val targetOffsetX = if (collapsed) -0.4f else 0f

            animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 200
                addUpdateListener { animation ->
                    val fraction = animation.animatedFraction
                    rotationAngle = rotationAngle + (targetRotation - rotationAngle) * fraction
                    offset_y = offset_y + (targetOffsetY - offset_y) * fraction
                    offset_x = offset_x + (targetOffsetX - offset_x) * fraction
                    invalidate()
                }
                start()
            }
        }

        fun draw(canvas: Canvas) {
            paint.color = highContrastColor
            val id = R.string.fa_angle_down
            paint.textSize = sp(18.0f)
            paint.strokeWidth = 0f
            paint.style = Paint.Style.FILL

            val label = resources.getString(id)
            val em = paint.measureText("m")

            rect.set(0f, 0f, width.toFloat(), height.toFloat())
            rect.offset(offset_x * em, offset_y * em)

            canvas.save()
            canvas.rotate(rotationAngle, rect.centerX(), rect.centerY())
            val fontMetrics = paint.fontMetrics
            val baseline = rect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(label, rect.centerX(), baseline, paint)
            canvas.restore()
        }
    }

    override fun onModelChange() {}
}
