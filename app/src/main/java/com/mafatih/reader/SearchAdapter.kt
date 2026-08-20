package com.mafatih.reader

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SearchAdapter(
    private val onClick: (SearchResult) -> Unit
) : RecyclerView.Adapter<SearchAdapter.VH>() {

    private var items: List<SearchResult> = emptyList()

    fun submit(list: List<SearchResult>) {
        items = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.breadcrumb.text = if (item.breadcrumb.isNotBlank()) item.breadcrumb else "مفاتیح الجنان"

        if (item.matchStart in 0 until item.text.length && item.matchEnd in item.matchStart..item.text.length) {
            val spannable = SpannableString(item.text)
            spannable.setSpan(
                BackgroundColorSpan(Color.parseColor("#FCE38A")),
                item.matchStart, item.matchEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#1A1A1A")),
                item.matchStart, item.matchEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                item.matchStart, item.matchEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            holder.text.text = spannable
        } else {
            holder.text.text = item.text
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val breadcrumb: TextView = view.findViewById(R.id.resultBreadcrumb)
        val text: TextView = view.findViewById(R.id.resultText)
    }
}
