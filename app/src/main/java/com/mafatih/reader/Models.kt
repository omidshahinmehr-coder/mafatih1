package com.mafatih.reader

/**
 * A single line/paragraph in the book.
 * type: "h1" | "h2" | "h3" (headings) | "a" (Arabic invocation text) | "f" (Persian translation)
 */
data class Rec(
    val type: String,
    val text: String
)

data class TocNode(
    val title: String,
    val lvl: Int,
    val index: Int,
    val children: MutableList<TocNode> = mutableListOf()
)

/** Flattened row used to render the TOC RecyclerView (respecting expand/collapse state). */
data class TocRow(
    val node: TocNode,
    val depth: Int,
    val hasChildren: Boolean,
    val isExpanded: Boolean
)

data class SearchResult(
    val recordIndex: Int,
    val breadcrumb: String,
    val text: String,
    val type: String,
    val matchStart: Int = -1,
    val matchEnd: Int = -1
)
