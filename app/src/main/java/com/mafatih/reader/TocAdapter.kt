package com.mafatih.reader

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TocAdapter(
    private val root: TocNode,
    private val onLeafClick: (TocNode) -> Unit
) : RecyclerView.Adapter<TocAdapter.VH>() {

    private val expanded = HashSet<Int>() // node.index used as key (unique per heading)
    private var rows: List<TocRow> = emptyList()

    init {
        // Expand top-level (باب) nodes by default so the drawer isn't just a flat empty list.
        for (child in root.children) expanded.add(child.index)
        rebuild()
    }

    private fun rebuild() {
        val list = ArrayList<TocRow>()
        fun walk(node: TocNode, depth: Int) {
            for (child in node.children) {
                val hasKids = child.children.isNotEmpty()
                val isExp = expanded.contains(child.index)
                list.add(TocRow(child, depth, hasKids, isExp))
                if (hasKids && isExp) walk(child, depth + 1)
            }
        }
        walk(root, 0)
        rows = list
    }

    fun refresh() {
        rebuild()
        notifyDataSetChanged()
    }

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_toc, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.title.text = row.node.title
        val startPad = 16 + row.depth * 20
        holder.title.setPadding(0, holder.title.paddingTop, 0, holder.title.paddingBottom)
        holder.itemView.setPadding(
            dp(holder.itemView, startPad), holder.itemView.paddingTop,
            dp(holder.itemView, 16), holder.itemView.paddingBottom
        )

        when (row.node.lvl) {
            1 -> { holder.title.textSize = 16f; holder.title.setTypeface(null, android.graphics.Typeface.BOLD) }
            2 -> { holder.title.textSize = 15f; holder.title.setTypeface(null, android.graphics.Typeface.BOLD) }
            else -> { holder.title.textSize = 14f; holder.title.setTypeface(null, android.graphics.Typeface.NORMAL) }
        }

        if (row.hasChildren) {
            holder.expandIcon.visibility = android.view.View.VISIBLE
            holder.expandIcon.rotation = if (row.isExpanded) 180f else 0f
        } else {
            holder.expandIcon.visibility = android.view.View.INVISIBLE
        }

        holder.itemView.setOnClickListener {
            if (row.hasChildren) {
                if (expanded.contains(row.node.index)) expanded.remove(row.node.index)
                else expanded.add(row.node.index)
                refresh()
            } else {
                onLeafClick(row.node)
            }
        }
        holder.itemView.setOnLongClickListener {
            onLeafClick(row.node)
            true
        }
    }

    private fun dp(view: android.view.View, value: Int): Int =
        (value * view.resources.displayMetrics.density).toInt()

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tocTitle)
        val expandIcon: ImageView = view.findViewById(R.id.expandIcon)
    }
}
