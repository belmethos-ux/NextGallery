package com.hakki.nextgallery

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.hakki.nextgallery.api.NextcloudClient
import com.hakki.nextgallery.databinding.ActivityMainBinding
import com.hakki.nextgallery.model.GalleryListItem
import com.hakki.nextgallery.model.PhotoItem
import com.hakki.nextgallery.ui.GalleryAdapter
import com.hakki.nextgallery.util.Prefs
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val GRID_SPAN = 3

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: Prefs
    private lateinit var client: NextcloudClient
    private lateinit var adapter: GalleryAdapter

    private var allPhotos: List<PhotoItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = Prefs(this)
        if (!prefs.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        client = NextcloudClient(prefs.serverUrl, prefs.username, prefs.appPassword)

        val authHeader = client.basicAuthHeader()
        adapter = GalleryAdapter(
            authHeader = authHeader,
            thumbUrlFor = { relPath -> client.thumbnailUrl(relPath) },
            onPhotoClick = { index -> openViewer(index) }
        )

        val layoutManager = GridLayoutManager(this, GRID_SPAN)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val row = adapter.currentRows.getOrNull(position)
                return if (row is GalleryListItem.Header) GRID_SPAN else 1
            }
        }
        binding.recyclerGallery.layoutManager = layoutManager
        binding.recyclerGallery.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadPhotos() }

        loadPhotos()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_logout) {
            prefs.clear()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadPhotos() {
        setLoading(true)
        lifecycleScope.launch {
            val photos = try {
                client.listPhotosRecursive(prefs.photosFolder)
            } catch (e: Exception) {
                emptyList()
            }
            allPhotos = photos
            sharedPhotoList = photos
            sharedClient = client
            adapter.submit(buildRows(photos))
            setLoading(false)
            binding.emptyState.visibility = if (photos.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun openViewer(index: Int) {
        val intent = Intent(this, PhotoViewerActivity::class.java).apply {
            putExtra(PhotoViewerActivity.EXTRA_INDEX, index)
        }
        startActivity(intent)
    }

    private fun setLoading(loading: Boolean) {
        binding.mainProgress.visibility = if (loading && allPhotos.isEmpty()) View.VISIBLE else View.GONE
        binding.swipeRefresh.isRefreshing = loading && allPhotos.isNotEmpty()
    }

    /** Groups the flat, newest-first photo list into "Today / Yesterday / Month Day, Year" sections. */
    private fun buildRows(photos: List<PhotoItem>): List<GalleryListItem> {
        val rows = mutableListOf<GalleryListItem>()
        var lastLabel: String? = null
        photos.forEachIndexed { index, photo ->
            val label = dateLabel(photo.modified.time)
            if (label != lastLabel) {
                rows.add(GalleryListItem.Header(label))
                lastLabel = label
            }
            rows.add(GalleryListItem.Photo(photo, index))
        }
        return rows
    }

    private fun dateLabel(timeMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        fun sameDay(a: Calendar, b: Calendar) =
            a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

        return when {
            sameDay(cal, today) -> "Today"
            sameDay(cal, yesterday) -> "Yesterday"
            cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) ->
                SimpleDateFormat("MMMM d", Locale.getDefault()).format(cal.time)
            else ->
                SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(cal.time)
        }
    }

    companion object {
        /** Shared with PhotoViewerActivity so it doesn't have to re-fetch or re-authenticate. */
        var sharedPhotoList: List<PhotoItem> = emptyList()
        var sharedClient: NextcloudClient? = null
    }
}
