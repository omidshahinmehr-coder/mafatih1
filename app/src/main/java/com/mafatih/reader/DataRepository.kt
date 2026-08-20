package com.mafatih.reader

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads assets/data.json (produced offline from the original mafatih.htm) and exposes
 * the flat record list, per-record breadcrumb (for search results) and the TOC tree.
 */
object DataRepository {

    var records: List<Rec> = emptyList()
        private set

    var breadcrumbs: List<String> = emptyList()
        private set

    var normalized: List<String> = emptyList()
        private set

    /** For each record, maps each char index in `normalized[i]` back to the
     *  corresponding char index in the original `records[i].text` — used to
     *  locate exact match offsets (for highlighting) after normalization. */
    var normalizedMaps: List<IntArray> = emptyList()
        private set

    var tocRoot: TocNode = TocNode("مفاتیح الجنان", 0, 0)
        private set

    @Volatile
    var isLoaded: Boolean = false
        private set

    fun load(context: Context) {
        if (isLoaded) return

        val jsonText = context.assets.open("data.json").use { input ->
            BufferedReader(InputStreamReader(input, "UTF-8")).readText()
        }
        val root = JSONObject(jsonText)
        val recArray: JSONArray = root.getJSONArray("records")

        val recs = ArrayList<Rec>(recArray.length())
        val crumbs = ArrayList<String>(recArray.length())
        val norm = ArrayList<String>(recArray.length())
        val normMaps = ArrayList<IntArray>(recArray.length())

        var h1: String? = null
        var h2: String? = null
        var h3: String? = null

        for (i in 0 until recArray.length()) {
            val pair = recArray.getJSONArray(i)
            val type = pair.getString(0)
            val text = pair.getString(1)
            recs.add(Rec(type, text))
            val (normText, normMap) = normalizeCore(text)
            norm.add(normText)
            normMaps.add(normMap)

            when (type) {
                "h1" -> { h1 = text; h2 = null; h3 = null }
                "h2" -> { h2 = text; h3 = null }
                "h3" -> { h3 = text }
            }
            val parts = listOfNotNull(h1, h2, if (type == "h3") null else h3)
            crumbs.add(parts.joinToString(" › "))
        }

        records = recs
        breadcrumbs = crumbs
        normalized = norm
        normalizedMaps = normMaps

        val tocJson = root.getJSONObject("toc")
        tocRoot = parseToc(tocJson)

        isLoaded = true
    }

    private fun parseToc(obj: JSONObject): TocNode {
        val node = TocNode(
            title = obj.optString("title"),
            lvl = obj.optInt("lvl"),
            index = obj.optInt("index")
        )
        val children = obj.optJSONArray("children")
        if (children != null) {
            for (i in 0 until children.length()) {
                node.children.add(parseToc(children.getJSONObject(i)))
            }
        }
        return node
    }

    /**
     * Normalizes Arabic/Persian text for forgiving search:
     * unifies ي/ی and ك/ک, strips harakat (diacritics), tatweel and zero-width marks.
     */
    fun normalize(input: String): String = normalizeCore(input).first

    /** Same as [normalize] but also returns a map from each output char index
     *  back to its source index in [input], so match offsets can be translated
     *  back to the original (un-normalized) text for highlighting. */
    private fun normalizeCore(input: String): Pair<String, IntArray> {
        val sb = StringBuilder(input.length)
        val map = ArrayList<Int>(input.length)
        for ((idx, ch) in input.withIndex()) {
            when (ch) {
                '\u064A', '\u0649' -> { sb.append('\u06CC'); map.add(idx) } // ي, ى -> ی
                '\u0643' -> { sb.append('\u06A9'); map.add(idx) } // ك -> ک
                '\u0629' -> { sb.append('\u0647'); map.add(idx) } // ة -> ه
                '\u200F', '\u200C', '\u200E', '\u0640' -> {} // RLM, ZWNJ, LRM, tatweel -> drop
                else -> {
                    val code = ch.code
                    // strip Arabic combining diacritics (harakat) U+064B..U+065F, U+0670
                    if (code in 0x064B..0x065F || code == 0x0670) {
                        // skip
                    } else {
                        sb.append(ch)
                        map.add(idx)
                    }
                }
            }
        }
        var start = 0
        var end = sb.length
        while (start < end && sb[start].isWhitespace()) start++
        while (end > start && sb[end - 1].isWhitespace()) end--
        val trimmedText = sb.substring(start, end)
        val trimmedMap = IntArray(end - start) { map[start + it] }
        return trimmedText to trimmedMap
    }

    fun search(query: String, limit: Int = 300): List<SearchResult> {
        if (query.isBlank()) return emptyList()
        val q = normalize(query)
        if (q.isEmpty()) return emptyList()
        val results = ArrayList<SearchResult>()
        for (i in records.indices) {
            val normText = normalized[i]
            val matchAt = normText.indexOf(q)
            if (matchAt >= 0) {
                val r = records[i]
                val map = normalizedMaps[i]
                val origStart = map[matchAt]
                val origEndExclusive = map[matchAt + q.length - 1] + 1
                results.add(SearchResult(i, breadcrumbs[i], r.text, r.type, origStart, origEndExclusive))
                if (results.size >= limit) break
            }
        }
        return results
    }
}
