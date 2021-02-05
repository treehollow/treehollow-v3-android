package com.github.treehollow.ui.mainscreen.timeline

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * Apply a spacing around items of a grid. It works with GridLayoutManager and StaggeredGridLayoutManager
 */
class GridSpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val (spanCount, spanIndex, spanSize) = extractGridData(parent, view)
        outRect.left = (spacing * ((spanCount - spanIndex) / spanCount.toFloat())).toInt()
        outRect.right = (spacing * ((spanIndex + spanSize) / spanCount.toFloat())).toInt()
        outRect.bottom = spacing
    }

    private fun extractGridData(parent: RecyclerView, view: View): GridItemData {
        val layoutManager = parent.layoutManager
        if (layoutManager is GridLayoutManager) {
            return extractGridLayoutData(layoutManager, view)
        } else if (layoutManager is StaggeredGridLayoutManager) {
            return extractStaggeredGridLayoutData(layoutManager, view)
        } else if (layoutManager is LinearLayoutManager) {
            return extractLinearLayoutData()
        } else {
            throw UnsupportedOperationException("Bad layout params")
        }
    }

    private fun extractGridLayoutData(layoutManager: GridLayoutManager, view: View): GridItemData {
        val lp: GridLayoutManager.LayoutParams = view.layoutParams as GridLayoutManager.LayoutParams
        return GridItemData(
            layoutManager.spanCount,
            lp.spanIndex,
            lp.spanSize
        )
    }

    private fun extractLinearLayoutData(
    ): GridItemData {
        return GridItemData(
            1,
            0,
            1
        )
    }

    private fun extractStaggeredGridLayoutData(
        layoutManager: StaggeredGridLayoutManager,
        view: View
    ): GridItemData {
        val lp: StaggeredGridLayoutManager.LayoutParams =
            view.layoutParams as StaggeredGridLayoutManager.LayoutParams
        return GridItemData(
            layoutManager.spanCount,
            lp.spanIndex,
            if (lp.isFullSpan) layoutManager.spanCount else 1
        )
    }

    internal data class GridItemData(val spanCount: Int, val spanIndex: Int, val spanSize: Int)
}