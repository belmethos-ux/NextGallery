package com.hakki.nextgallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.target.Target
import com.hakki.nextgallery.databinding.ActivityPhotoViewerBinding
import com.hakki.nextgallery.databinding.ItemPhotoPageBinding
import com.hakki.nextgallery.model.PhotoItem

/**
 * Fullscreen, swipeable, pinch-to-zoom viewer over the photo list the gallery
 * screen already loaded (see MainActivity.sharedPhotoList / sharedClient).
 */
class PhotoViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photos = MainActivity.sharedPhotoList
        val client = MainActivity.sharedClient
        val startIndex = intent.getIntExtra(EXTRA_INDEX, 0)

        if (photos.isEmpty() || client == null) {
            finish()
            return
        }

        val authHeader = client.basicAuthHeader()
        binding.photoPager.adapter = PagerAdapter(photos, authHeader) { relPath ->
            client.originalUrl(relPath)
        }
        binding.photoPager.setCurrentItem(startIndex, false)
    }

    companion object {
        const val EXTRA_INDEX = "extra_index"
    }
}

private class PagerAdapter(
    private val photos: List<PhotoItem>,
    private val authHeader: String,
    private val originalUrlFor: (String) -> String
) : RecyclerView.Adapter<PagerAdapter.PageVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageVH {
        val binding = ItemPhotoPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PageVH(binding)
    }

    override fun getItemCount() = photos.size

    override fun onBindViewHolder(holder: PageVH, position: Int) {
        holder.bind(photos[position])
    }

    inner class PageVH(private val binding: ItemPhotoPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoItem) {
            binding.pageProgress.visibility = View.VISIBLE
            val url = GlideUrl(
                originalUrlFor(photo.relativePath),
                LazyHeaders.Builder().addHeader("Authorization", authHeader).build()
            )
            Glide.with(binding.photoView)
                .load(url)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.pageProgress.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.pageProgress.visibility = View.GONE
                        return false
                    }
                })
                .into(binding.photoView)
        }
    }
}
