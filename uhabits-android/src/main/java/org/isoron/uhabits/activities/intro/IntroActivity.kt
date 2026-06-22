package org.isoron.uhabits.activities.intro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import org.isoron.uhabits.R

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnSkip: Button
    private lateinit var btnNext: Button
    private lateinit var indicatorContainer: LinearLayout

    private val slides = listOf(
        IntroSlide(
            titleRes = R.string.intro_title_1,
            descRes = R.string.intro_description_1,
            iconRes = R.drawable.intro_icon_1
        ),
        IntroSlide(
            titleRes = R.string.intro_title_2,
            descRes = R.string.intro_description_2,
            iconRes = R.drawable.intro_icon_2
        ),
        IntroSlide(
            titleRes = R.string.intro_title_4,
            descRes = R.string.intro_description_4,
            iconRes = R.drawable.intro_icon_4
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        viewPager = findViewById(R.id.viewPager)
        btnSkip = findViewById(R.id.btnSkip)
        btnNext = findViewById(R.id.btnNext)
        indicatorContainer = findViewById(R.id.indicatorContainer)

        setupViewPager()
        setupIndicators()
        setupButtons()
    }

    private fun setupViewPager() {
        viewPager.adapter = IntroPagerAdapter(slides)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                if (position == slides.size - 1) {
                    btnNext.text = getString(R.string.done_label)
                } else {
                    btnNext.text = getString(R.string.intro_next)
                }
            }
        })
    }

    private fun setupIndicators() {
        val indicators = arrayOfNulls<ImageView>(slides.size)
        val margin = org.isoron.uhabits.utils.InterfaceUtils.dpToPixels(this, 4f).toInt()
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(margin, 0, margin, 0)
        }

        for (i in indicators.indices) {
            indicators[i] = ImageView(applicationContext).apply {
                setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.indicator_inactive))
                this.layoutParams = layoutParams
            }
            indicatorContainer.addView(indicators[i])
        }
    }

    private fun updateIndicators(position: Int) {
        val childCount = indicatorContainer.childCount
        for (i in 0 until childCount) {
            val imageView = indicatorContainer.getChildAt(i) as ImageView
            if (i == position) {
                imageView.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.indicator_active))
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.indicator_inactive))
            }
        }
    }

    private fun setupButtons() {
        btnSkip.setOnClickListener {
            finish()
        }
        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < slides.size) {
                viewPager.currentItem += 1
            } else {
                finish()
            }
        }
    }

    private data class IntroSlide(
        val titleRes: Int,
        val descRes: Int,
        val iconRes: Int
    )

    private inner class IntroPagerAdapter(private val slides: List<IntroSlide>) :
        RecyclerView.Adapter<IntroPagerAdapter.IntroViewHolder>() {

        inner class IntroViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val slideIcon: ImageView = view.findViewById(R.id.slideIcon)
            val slideTitle: TextView = view.findViewById(R.id.slideTitle)
            val slideDescription: TextView = view.findViewById(R.id.slideDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IntroViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_intro_slide, parent, false)
            return IntroViewHolder(view)
        }

        override fun onBindViewHolder(holder: IntroViewHolder, position: Int) {
            val slide = slides[position]
            holder.slideTitle.setText(slide.titleRes)
            holder.slideDescription.setText(slide.descRes)
            holder.slideIcon.setImageResource(slide.iconRes)
        }

        override fun getItemCount(): Int = slides.size
    }
}
