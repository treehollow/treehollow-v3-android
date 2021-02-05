package com.github.treehollow.ui.mainscreen

import android.view.View
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2

//private const val MIN_SCALE = 0.85f
//private const val MIN_ALPHA = 0.5f

class PageTransformer(private var textView1: TextView, private var textView2: TextView) :
    ViewPager2.PageTransformer {

    override fun transformPage(view: View, position: Float) {
//        Log.i("TRANSFORM", position.toString())
        view.apply {
            val pageWidth = width
//            val pageHeight = height
            when {
                position < -1 -> { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    alpha = 0f
                }
                position <= 1 -> { // [-1,1]
                    if (position > 0F) {
                        setTextViewSizes(textView1, textView2, position)
                    }

                    // Modify the default slide transition to shrink the page as well
//                    val scaleFactor = MIN_SCALE.coerceAtLeast(1 - abs(position))
//                    val scaleFactor = (1 - abs(position))
//                    val vertMargin = pageHeight * (1 - scaleFactor) / 2
//                    val horzMargin = pageWidth * (1 - scaleFactor) / 2
//                    translationX = if (position < 0) {
//                        horzMargin - vertMargin / 2
//                    } else {
//                        horzMargin + vertMargin / 2
//                    }

//                    // Scale the page down (between MIN_SCALE and 1)
//                    scaleX = scaleFactor
//                    scaleY = scaleFactor
//
//                    // Fade the page relative to its size.
//                    alpha = (MIN_ALPHA +
//                            (((scaleFactor - MIN_SCALE) / (1 - MIN_SCALE)) * (1 - MIN_ALPHA)))
                }
                else -> { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    alpha = 0f
                }
            }
        }
    }
}

fun setTextViewSizes(textView1: TextView, textView2: TextView, position: Float) {
    textView1.textSize = ((18 + (position - 0.5) * 4).toFloat())
    textView2.textSize = ((18 - (position - 0.5) * 4).toFloat())
    textView1.alpha = 1 - (1 - position) * 0.3F
    textView2.alpha = 1 - (position) * 0.3F
    if (position < 0.5) {
        textView2.setTypeface(null, android.graphics.Typeface.BOLD)
        textView1.setTypeface(null, android.graphics.Typeface.NORMAL)
    } else {
        textView1.setTypeface(null, android.graphics.Typeface.BOLD)
        textView2.setTypeface(null, android.graphics.Typeface.NORMAL)
    }
}