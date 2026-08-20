package com.mafatih.reader

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReaderAdapter(
    private val onHeadingLongPress: ((Int) -> Unit)? = null,
    private val onBookmarkToggle: ((Int, Boolean) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_H1 = 0
        const val TYPE_H2 = 1
        const val TYPE_H3 = 2
        const val TYPE_AR = 3
        const val TYPE_FA = 4
    }

    data class Style(
        val typeface: Typeface?,
        val colors: ThemeColors,
        val fontScale: Float
    )

    private var style = Style(null, ThemePalette.LIGHT, 1.0f)

    /** Positions in the RecyclerView -> index into DataRepository.records */
    private var displayIndices: IntArray = IntArray(0)

    /** Reverse lookup: record index -> position (or -1 if filtered out) */
    private var recordToPosition: IntArray = IntArray(0)

    fun setShowTranslation(show: Boolean) {
        val all = DataRepository.records
        val list = ArrayList<Int>(all.size)
        for (i in all.indices) {
            if (show || all[i].type != "f") list.add(i)
        }
        displayIndices = list.toIntArray()
        recordToPosition = IntArray(all.size) { -1 }
        for (pos in displayIndices.indices) {
            recordToPosition[displayIndices[pos]] = pos
        }
        notifyDataSetChanged()
    }

    fun updateStyle(newStyle: Style) {
        style = newStyle
        notifyDataSetChanged()
    }

    /** Returns the adapter position for a given record index, snapping forward to the
     *  nearest visible row if that exact record is currently hidden (e.g. a filtered translation). */
    fun positionForRecordIndex(recordIndex: Int): Int {
        if (recordIndex < 0 || recordIndex >= recordToPosition.size) return -1
        var i = recordIndex
        while (i < recordToPosition.size) {
            val pos = recordToPosition[i]
            if (pos >= 0) return pos
            i++
        }
        return -1
    }

    /** Returns the record index currently shown at a given adapter position, or -1. */
    fun recordIndexAt(position: Int): Int {
        if (position < 0 || position >= displayIndices.size) return -1
        return displayIndices[position]
    }

    /** Refreshes only the visible star icons (call after a bookmark toggle) without
     *  reloading the whole list. */
    fun notifyBookmarkChanged(recordIndex: Int) {
        val pos = positionForRecordIndex(recordIndex)
        if (pos >= 0) notifyItemChanged(pos)
    }

    override fun getItemCount(): Int = displayIndices.size

    override fun getItemViewType(position: Int): Int {
        val rec = DataRepository.records[displayIndices[position]]
        return when (rec.type) {
            "h1" -> TYPE_H1
            "h2" -> TYPE_H2
            "h3" -> TYPE_H3
            "a" -> TYPE_AR
            else -> TYPE_FA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == TYPE_H3) {
            val view = inflater.inflate(R.layout.item_heading3, parent, false)
            return H3VH(view)
        }
        val layoutRes = when (viewType) {
            TYPE_H1 -> R.layout.item_heading1
            TYPE_H2 -> R.layout.item_heading2
            TYPE_AR -> R.layout.item_content_ar
            else -> R.layout.item_content_fa
        }
        val view = inflater.inflate(layoutRes, parent, false)
        return RowVH(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val recordIndex = displayIndices[position]
        val rec = DataRepository.records[recordIndex]
        val c = style.colors

        if (holder is H3VH) {
            holder.text.text = rec.text
            holder.itemView.setBackgroundColor(c.pageBackground)
            holder.text.textSize = 15f * style.fontScale
            holder.text.setTextColor(c.h3Text)

            val bookmarked = PrefsManager.isBookmarked(holder.itemView.context, recordIndex)
            holder.star.setImageResource(if (bookmarked) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
            holder.star.setOnClickListener {
                val nowBookmarked = PrefsManager.toggleBookmark(holder.itemView.context, recordIndex)
                holder.star.setImageResource(if (nowBookmarked) R.drawable.ic_star_filled else R.drawable.ic_star_outline)
                onBookmarkToggle?.invoke(recordIndex, nowBookmarked)
            }
            if (onHeadingLongPress != null) {
                holder.itemView.setOnLongClickListener {
                    onHeadingLongPress.invoke(recordIndex)
                    true
                }
            }
            return
        }

        val vh = holder as RowVH
        vh.text.text = rec.text
        if (rec.type == "a" || rec.type == "f") {
            vh.text.typeface = style.typeface
        }

        vh.itemView.setBackgroundColor(
            when (rec.type) {
                "h1" -> c.h1Bg
                "h2" -> c.h2Bg
                else -> c.pageBackground
            }
        )
        val baseSize = when (rec.type) {
            "h1" -> 20f
            "h2" -> 17f
            "a" -> 19f
            else -> 14.5f
        }
        vh.text.textSize = baseSize * style.fontScale
        vh.text.setTextColor(
            when (rec.type) {
                "h1" -> c.h1Text
                "h2" -> c.h2Text
                "a" -> c.arText
                else -> c.faText
            }
        )
    }

    class RowVH(view: View) : RecyclerView.ViewHolder(view) {
        // item_heading1/2, item_content_ar/fa are each a single root TextView.
        val text: TextView = view as TextView
    }

    class H3VH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.headingText)
        val star: ImageButton = view.findViewById(R.id.starBtn)
    }
}
