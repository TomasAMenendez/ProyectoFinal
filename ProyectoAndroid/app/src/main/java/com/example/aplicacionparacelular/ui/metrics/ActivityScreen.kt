package com.example.aplicacionparacelular.ui.metrics

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.databinding.FragmentMetricsBinding

object ActivityScreen {
    fun render(binding: FragmentMetricsBinding, presentation: ActivityPresentation) {
        binding.metricsScroll.scrollTo(0, 0)
        val ready = presentation.phase == ActivityPhase.READY
        binding.groupContent.visibility = if (ready) View.VISIBLE else View.GONE
        binding.progressActivity.visibility = if (presentation.phase == ActivityPhase.LOADING) View.VISIBLE else View.GONE
        binding.btnRetry.visibility = if (
            presentation.phase == ActivityPhase.FAILED || presentation.phase == ActivityPhase.OFFLINE
        ) View.VISIBLE else View.GONE
        val status = when {
            presentation.phase == ActivityPhase.LOADING -> R.string.activity_loading
            presentation.phase == ActivityPhase.FAILED -> R.string.activity_error
            presentation.phase == ActivityPhase.OFFLINE -> R.string.activity_offline
            ready && presentation.interactions == 0 -> R.string.activity_empty
            else -> 0
        }
        binding.txtStatus.visibility = if (status == 0) View.GONE else View.VISIBLE
        if (status != 0) binding.txtStatus.setText(status)
        binding.txtPeriodRange.text = ActivityCopy.rangeLabel(presentation.requestedDays)
        if (!ready) return

        val context = binding.root.context
        bindCount(binding.txtInteractions, presentation.interactions, numberSize = 40f, absentSize = 20f)
        binding.txtInteractionsCaption.visibility = if (presentation.interactions == null) View.GONE else View.VISIBLE
        binding.txtGames.text = ActivityCopy.counterLine(presentation.games, context.getString(R.string.activity_games))
        binding.txtRoutines.text = ActivityCopy.counterLine(
            presentation.routines,
            context.getString(R.string.activity_routines),
        )
        binding.txtCrises.text = ActivityCopy.counterLine(presentation.crises, context.getString(R.string.activity_crises))
        ActivityCharts.days(binding.dayScroll, binding.dayBars, binding.txtDaysAbsent, presentation.days)
        ActivityCharts.areas(binding.areaBars, binding.txtAreasAbsent, presentation.areas)
        binding.txtNewWords.text = ActivityCopy.wordsLine(presentation.newWords)
        if (presentation.newWords == null) {
            binding.txtNewWords.setBackgroundResource(0)
            binding.txtWordsPeriod.visibility = View.GONE
        } else {
            binding.txtNewWords.setBackgroundResource(R.drawable.teo_word_highlight)
            binding.txtNewWords.setPadding(
                (8 * context.resources.displayMetrics.density).toInt(),
                (2 * context.resources.displayMetrics.density).toInt(),
                (8 * context.resources.displayMetrics.density).toInt(),
                (2 * context.resources.displayMetrics.density).toInt(),
            )
            binding.txtWordsPeriod.visibility = View.VISIBLE
            binding.txtWordsPeriod.text = ActivityCopy.periodLine(presentation.requestedDays)
        }
        binding.txtVocabTotal.text = ActivityCopy.totalLine(presentation.vocabularyTotal)
    }

    private fun bindCount(view: TextView, value: Int?, numberSize: Float, absentSize: Float) {
        view.text = ActivityCopy.countText(value)
        view.textSize = if (value == null) absentSize else numberSize
        view.maxLines = 2
    }
}

object ActivityCharts {
    fun days(
        scroll: HorizontalScrollView,
        container: LinearLayout,
        absent: TextView,
        days: List<ActivityDayBar>?,
    ) {
        if (days.isNullOrEmpty()) {
            scroll.visibility = View.GONE
            container.visibility = View.GONE
            absent.visibility = View.VISIBLE
            return
        }
        absent.visibility = View.GONE
        scroll.visibility = View.VISIBLE
        container.visibility = View.VISIBLE
        val density = container.resources.displayMetrics.density
        val fixed = (52 * density).toInt()
        val token = Any()
        scroll.tag = token
        scroll.post {
            if (scroll.tag != token) return@post
            val viewport = scroll.width - scroll.paddingLeft - scroll.paddingRight
            val column = if (days.size in 1..8 && viewport > 0) viewport / days.size else fixed
            fillDays(container, days, column.coerceAtLeast((36 * density).toInt()))
        }
    }

    fun areas(container: LinearLayout, absent: TextView, areas: List<ActivityAreaBar>?) {
        container.removeAllViews()
        if (areas.isNullOrEmpty()) {
            container.visibility = View.GONE
            absent.visibility = View.VISIBLE
            return
        }
        absent.visibility = View.GONE
        container.visibility = View.VISIBLE
        val context = container.context
        val density = context.resources.displayMetrics.density
        val ink = ContextCompat.getColor(context, R.color.teo_text)
        val trackColor = ContextCompat.getColor(context, R.color.teo_neutral)
        val fillColor = ContextCompat.getColor(context, R.color.teo_primary)
        val max = areas.maxOf { it.value }.coerceAtLeast(1)
        areas.forEachIndexed { index, area ->
            val block = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { if (index > 0) topMargin = (12 * density).toInt() }
            }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = (36 * density).toInt()
            }
            row.addView(TextView(context).apply {
                text = area.label
                setTextColor(ink)
                textSize = 15f
                maxLines = 2
                layoutParams = LinearLayout.LayoutParams((104 * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            })
            val track = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(0, (10 * density).toInt(), 1f).apply {
                    marginStart = (8 * density).toInt()
                    marginEnd = (8 * density).toInt()
                }
                background = rounded(trackColor, 5f * density)
                weightSum = max.toFloat()
            }
            track.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, area.value.toFloat())
                background = rounded(fillColor, 5f * density)
            })
            track.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, (max - area.value).toFloat())
            })
            row.addView(track)
            row.addView(TextView(context).apply {
                text = area.value.toString()
                setTextColor(ink)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.END
                minWidth = (28 * density).toInt()
            })
            block.addView(row)
            container.addView(block)
        }
    }

    private fun fillDays(container: LinearLayout, days: List<ActivityDayBar>, columnWidth: Int) {
        container.removeAllViews()
        val context = container.context
        val density = context.resources.displayMetrics.density
        val max = days.maxOf { it.value }.coerceAtLeast(1)
        val track = (96 * density).toInt()
        val barWidth = (18 * density).toInt()
        val ink = ContextCompat.getColor(context, R.color.teo_text)
        val secondary = ContextCompat.getColor(context, R.color.teo_secondary)
        val fill = ContextCompat.getColor(context, R.color.teo_primary)
        for (day in days) {
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                layoutParams = LinearLayout.LayoutParams(columnWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            column.addView(TextView(context).apply {
                text = day.value.toString()
                setTextColor(ink)
                textSize = 12f
                gravity = Gravity.CENTER
            })
            val host = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, track).apply {
                    topMargin = (4 * density).toInt()
                    bottomMargin = (4 * density).toInt()
                }
            }
            val height = ((track * day.value) / max.toFloat()).toInt().let { raw ->
                if (day.value > 0) raw.coerceAtLeast((4 * density).toInt()) else (2 * density).toInt()
            }
            host.addView(View(context).apply {
                layoutParams = FrameLayout.LayoutParams(barWidth, height, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                background = rounded(fill, 6f * density)
            })
            column.addView(host)
            column.addView(TextView(context).apply {
                text = day.label
                setTextColor(secondary)
                textSize = 12f
                gravity = Gravity.CENTER
                maxLines = 1
            })
            container.addView(column)
        }
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        cornerRadius = radius
        setColor(color)
    }
}
