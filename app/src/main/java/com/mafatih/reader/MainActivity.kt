package com.mafatih.reader

import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var readerList: RecyclerView
    private lateinit var searchResultsList: RecyclerView
    private lateinit var tocList: RecyclerView
    private lateinit var searchInput: android.widget.EditText
    private lateinit var searchClearBtn: android.widget.ImageButton
    private lateinit var settingsBtn: android.widget.ImageButton
    private lateinit var bookmarksBtn: android.widget.ImageButton
    private lateinit var loadingSpinner: android.widget.ProgressBar
    private lateinit var emptyResults: android.widget.TextView
    private lateinit var fabTop: FloatingActionButton
    private lateinit var layoutManager: LinearLayoutManager

    private lateinit var readerAdapter: ReaderAdapter
    private lateinit var searchAdapter: SearchAdapter
    private var tocAdapter: TocAdapter? = null

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        readerList = findViewById(R.id.readerList)
        searchResultsList = findViewById(R.id.searchResultsList)
        tocList = findViewById(R.id.tocList)
        searchInput = findViewById(R.id.searchInput)
        searchClearBtn = findViewById(R.id.searchClearBtn)
        settingsBtn = findViewById(R.id.settingsBtn)
        bookmarksBtn = findViewById(R.id.bookmarksBtn)
        loadingSpinner = findViewById(R.id.loadingSpinner)
        emptyResults = findViewById(R.id.emptyResults)
        fabTop = findViewById(R.id.fabTop)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        layoutManager = LinearLayoutManager(this)
        readerList.layoutManager = layoutManager
        readerAdapter = ReaderAdapter(
            onHeadingLongPress = { idx -> shareRecordAt(idx) },
            onBookmarkToggle = { _, nowBookmarked ->
                val msg = if (nowBookmarked) R.string.bookmark_added else R.string.bookmark_removed
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
        readerList.adapter = readerAdapter

        searchResultsList.layoutManager = LinearLayoutManager(this)
        searchAdapter = SearchAdapter(onClick = { result -> onSearchResultTap(result) })
        searchResultsList.adapter = searchAdapter

        tocList.layoutManager = LinearLayoutManager(this)

        readerList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                fabTop.visibility = if (firstVisible > 8) View.VISIBLE else View.GONE
            }
        })
        fabTop.setOnClickListener {
            readerList.scrollToPosition(0)
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString().orEmpty()
                searchClearBtn.visibility = if (text.isNotEmpty()) View.VISIBLE else View.GONE
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                val r = Runnable { performSearch(text) }
                searchRunnable = r
                searchHandler.postDelayed(r, 220)
            }
        })
        searchClearBtn.setOnClickListener {
            searchInput.setText("")
        }

        settingsBtn.setOnClickListener { showSettingsSheet() }
        bookmarksBtn.setOnClickListener { showBookmarksSheet() }

        loadingSpinner.visibility = View.VISIBLE
        thread {
            DataRepository.load(applicationContext)
            runOnUiThread {
                loadingSpinner.visibility = View.GONE
                onDataReady()
            }
        }
    }

    private fun onDataReady() {
        val showTranslation = PrefsManager.getShowTranslation(this)
        readerAdapter.setShowTranslation(showTranslation)
        applyStyle()

        tocAdapter = TocAdapter(DataRepository.tocRoot) { node ->
            jumpToRecord(node.index)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        tocList.adapter = tocAdapter

        restoreLastReadPosition()
    }

    /** Silently resumes reading where the user left off last time (if any). */
    private fun restoreLastReadPosition() {
        val lastIndex = PrefsManager.getLastReadIndex(this)
        if (lastIndex < 0) return
        val pos = readerAdapter.positionForRecordIndex(lastIndex)
        if (pos > 0) {
            readerList.post { layoutManager.scrollToPositionWithOffset(pos, 24) }
        }
    }

    /** Persists the record currently at the top of the reader so the app can
     *  resume from the same spot next time it's opened. */
    private fun saveLastReadPosition() {
        if (!DataRepository.isLoaded) return
        if (readerList.visibility != View.VISIBLE) return // don't overwrite while in search mode
        val firstPos = layoutManager.findFirstVisibleItemPosition()
        if (firstPos < 0) return
        val recordIndex = readerAdapter.recordIndexAt(firstPos)
        if (recordIndex >= 0) {
            PrefsManager.setLastReadIndex(this, recordIndex)
        }
    }

    override fun onPause() {
        super.onPause()
        saveLastReadPosition()
    }

    private fun applyStyle() {
        val font = PrefsManager.getFont(this)
        val typeface = if (font == AppFont.ESTEDAD) {
            try { androidx.core.content.res.ResourcesCompat.getFont(this, R.font.estedad_family) }
            catch (e: Exception) { Typeface.DEFAULT }
        } else Typeface.DEFAULT

        val theme = PrefsManager.getTheme(this)
        val colors = ThemePalette.forTheme(theme)
        val scale = PrefsManager.getFontScale(this)

        readerAdapter.updateStyle(ReaderAdapter.Style(typeface, colors, scale))
        readerList.setBackgroundColor(colors.pageBackground)
        window.decorView.setBackgroundColor(colors.pageBackground)
    }

    private fun performSearch(query: String) {
        if (!DataRepository.isLoaded) return
        if (query.isBlank()) {
            searchResultsList.visibility = View.GONE
            emptyResults.visibility = View.GONE
            readerList.visibility = View.VISIBLE
            return
        }
        readerList.visibility = View.GONE
        val results = DataRepository.search(query)
        searchAdapter.submit(results)
        searchResultsList.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
        emptyResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onSearchResultTap(result: SearchResult) {
        searchInput.setText("")
        readerList.visibility = View.VISIBLE
        searchResultsList.visibility = View.GONE
        jumpToRecord(result.recordIndex)
    }

    private fun jumpToRecord(recordIndex: Int) {
        val pos = readerAdapter.positionForRecordIndex(recordIndex)
        if (pos >= 0) {
            layoutManager.scrollToPositionWithOffset(pos, 24)
        }
    }

    private fun shareRecordAt(recordIndex: Int) {
        if (recordIndex < 0 || recordIndex >= DataRepository.records.size) return
        val text = DataRepository.records[recordIndex].text
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, text)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun showBookmarksSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_bookmarks, null)
        dialog.setContentView(view)

        val list = view.findViewById<RecyclerView>(R.id.bookmarksList)
        val empty = view.findViewById<android.widget.TextView>(R.id.bookmarksEmpty)
        list.layoutManager = LinearLayoutManager(this)

        val bookmarkedIndices = PrefsManager.getBookmarks(this).sorted()
        val items = bookmarkedIndices.mapNotNull { idx ->
            if (idx in DataRepository.records.indices) {
                val rec = DataRepository.records[idx]
                SearchResult(idx, DataRepository.breadcrumbs[idx], rec.text, rec.type)
            } else null
        }

        val adapter = SearchAdapter(onClick = { result ->
            dialog.dismiss()
            readerList.visibility = View.VISIBLE
            searchResultsList.visibility = View.GONE
            jumpToRecord(result.recordIndex)
        })
        list.adapter = adapter
        adapter.submit(items)

        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        list.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE

        dialog.show()
    }

    private fun showSettingsSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_settings, null)
        dialog.setContentView(view)

        val fontGroup = view.findViewById<RadioGroup>(R.id.fontRadioGroup)
        val themeGroup = view.findViewById<RadioGroup>(R.id.themeRadioGroup)
        val translationSwitch = view.findViewById<SwitchMaterial>(R.id.translationSwitch)
        val fontSizeLabel = view.findViewById<android.widget.TextView>(R.id.fontSizeLabel)
        val fontSizeMinus = view.findViewById<android.widget.Button>(R.id.fontSizeMinus)
        val fontSizePlus = view.findViewById<android.widget.Button>(R.id.fontSizePlus)

        when (PrefsManager.getFont(this)) {
            AppFont.SYSTEM -> fontGroup.check(R.id.fontSystem)
            AppFont.ESTEDAD -> fontGroup.check(R.id.fontEstedad)
        }
        when (PrefsManager.getTheme(this)) {
            AppTheme.LIGHT -> themeGroup.check(R.id.themeLight)
            AppTheme.SEPIA -> themeGroup.check(R.id.themeSepia)
            AppTheme.DARK -> themeGroup.check(R.id.themeDark)
            AppTheme.NIGHT -> themeGroup.check(R.id.themeNight)
        }
        translationSwitch.isChecked = PrefsManager.getShowTranslation(this)

        var scale = PrefsManager.getFontScale(this)
        fun refreshScaleLabel() { fontSizeLabel.text = "${(scale * 100).toInt()}%" }
        refreshScaleLabel()

        fontGroup.setOnCheckedChangeListener { _, checkedId ->
            val f = if (checkedId == R.id.fontEstedad) AppFont.ESTEDAD else AppFont.SYSTEM
            PrefsManager.setFont(this, f)
            applyStyle()
        }
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val t = when (checkedId) {
                R.id.themeSepia -> AppTheme.SEPIA
                R.id.themeDark -> AppTheme.DARK
                R.id.themeNight -> AppTheme.NIGHT
                else -> AppTheme.LIGHT
            }
            PrefsManager.setTheme(this, t)
            applyStyle()
        }
        translationSwitch.setOnCheckedChangeListener { _, checked ->
            PrefsManager.setShowTranslation(this, checked)
            readerAdapter.setShowTranslation(checked)
        }
        fontSizeMinus.setOnClickListener {
            scale = (scale - 0.1f).coerceAtLeast(0.7f)
            PrefsManager.setFontScale(this, scale)
            refreshScaleLabel()
            applyStyle()
        }
        fontSizePlus.setOnClickListener {
            scale = (scale + 0.1f).coerceAtMost(1.8f)
            PrefsManager.setFontScale(this, scale)
            refreshScaleLabel()
            applyStyle()
        }

        view.findViewById<android.widget.TextView>(R.id.aboutBtn).setOnClickListener {
            dialog.dismiss()
            startActivity(android.content.Intent(this, AboutActivity::class.java))
        }

        dialog.show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else if (searchResultsList.visibility == View.VISIBLE) {
            searchInput.setText("")
        } else {
            super.onBackPressed()
        }
    }
}
