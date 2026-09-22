package com.hakki.nextgallery.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.hakki.nextgallery.R
import com.hakki.nextgallery.model.GalleryListItem

private const val TYPE_HEADER = 0
private const val TYPE_PHOTO = 1

class GalleryAdapter(
    private val authHeader: String,
    private val thumbUrlFor: (String) -> String,
    private val onPhotoClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<GalleryListItem> = emptyList()

    /** Read-only snapshot, used by the GridLayoutManager's SpanSizeLookup. */
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
            Glide.with(image).load(url).centerCrop().into(image)
            itemView.setOnClickListener { onPhotoClick(photo.originalIndex) }
        }
    }
}
