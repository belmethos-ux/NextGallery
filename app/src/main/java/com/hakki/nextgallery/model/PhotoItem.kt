package com.hakki.nextgallery.model

import java.util.Date

/**
 * A single media file (photo or video) found on the Nextcloud server.
 *
 * @property relativePath path relative to the user's files root, e.g. "Photos/2024/img.jpg"
 *                         — this is what the thumbnail API and WebDAV GET both need.
 * @property davHref      the full WebDAV href returned by PROPFIND, used to fetch the original.
 */
data class PhotoItem(
    val relativePath: String,
    val davHref: String,
    val name: String,
    val modified: Date,
    val isVideo: Boolean
)

/** Rows fed to the gallery RecyclerView: either a date section header or a photo. */
sealed class GalleryListItem {
    data class Header(val label: String) : GalleryListItem()
    data class Photo(val item: PhotoItem, val originalIndex: Int) : GalleryListItem()
}
