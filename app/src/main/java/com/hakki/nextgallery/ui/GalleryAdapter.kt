package com.hakki.nextgallery.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.hakki.nextgallery.R
import com.hakki.nextgallery.model.GalleryListItem
import java.util.concurrent.atomic.AtomicBoolean

private const val TYPE_HEADER = 0
private const val TYPE_PHOTO = 1

class GalleryAdapter(
    private val authHeader: String,
    private val thumbUrlFor: (String) -> String,
    private val onPhotoClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<GalleryListItem> = emptyList()

    val currentRows: List<GalleryListItem>
        get() = rows

    fun submit(newRows: List<GalleryListItem>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = when (rows[position]) {
        is GalleryListItem.Header -> TYPE_HEADER
        is GalleryListItem.Photo -> TYPE_PHOTO
    }

    override fun getItemCount() = rows.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inflater.inflate(R.layout.item_header, parent, false))
        } else {
            PhotoVH(inflater.inflate(R.layout.item_photo, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is GalleryListItem.Header -> (holder as HeaderVH).bind(row.label)
            is GalleryListItem.Photo -> (holder as PhotoVH).bind(row)
        }
    }

    inner class HeaderVH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.textHeaderDate)
        fun bind(label: String) {
            text.text = label
        }
    }

    inner class PhotoVH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.imageThumb)

        fun bind(photo: GalleryListItem.Photo) {
            val url = GlideUrl(
                thumbUrlFor(photo.item.relativePath),
                LazyHeaders.Builder().addHeader("Authorization", authHeader).build()
            )
            Glide.with(image)
                .load(url)
                .error(android.R.drawable.stat_notify_error)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (!errorShown.getAndSet(true)) {
                            val cause = e?.rootCauses?.firstOrNull()?.toString()
                                ?: e?.message
                                ?: "unknown error"
                            Toast.makeText(
                                itemView.context,
                                "Image load failed: $cause",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean = false
                })
                .centerCrop()
                .into(image)
            itemView.setOnClickListener { onPhotoClick(photo.originalIndex) }
        }
    }

    companion object {
        private val errorShown = AtomicBoolean(false)
    }
}
