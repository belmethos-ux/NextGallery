package com.hakki.nextgallery.api

import com.hakki.nextgallery.model.PhotoItem
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "gif", "heic", "webp", "bmp")
private val VIDEO_EXT = setOf("mp4", "mov", "3gp", "mkv", "avi", "webm")

/**
 * Thin wrapper around Sardine (WebDAV) for browsing a Nextcloud account,
 * plus helpers for building thumbnail / original-image URLs that Glide can
 * load with Basic Auth headers.
 */
class NextcloudClient(
    private val serverUrl: String,
    private val username: String,
    private val appPassword: String
) {
    private val sardine: OkHttpSardine by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        OkHttpSardine(client).apply { setCredentials(username, appPassword) }
    }

    private val filesRoot: String
        get() = "$serverUrl/remote.php/dav/files/${encode(username)}"

    /**
     * Recursively walks [folder] (relative to the files root, "" = root) and
     * returns every image/video found, newest first.
     */
    suspend fun listPhotosRecursive(folder: String, maxDepth: Int = 8): List<PhotoItem> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<PhotoItem>()
            walk(folder.trim('/'), maxDepth, results)
            results.sortedByDescending { it.modified }
        }

    private fun walk(relFolder: String, depthLeft: Int, out: MutableList<PhotoItem>) {
        if (depthLeft <= 0) return
        val folderUrl = if (relFolder.isEmpty()) filesRoot else "$filesRoot/${encodePath(relFolder)}"

        val entries: List<DavResource> = try {
            sardine.list(folderUrl, 1)
        } catch (e: Exception) {
            return // folder unreadable / doesn't exist — skip quietly
        }

        for (entry in entries) {
            val entryRelPath = hrefToRelativePath(entry.href.toString())
            // Sardine includes the folder itself as the first result — skip it.
            if (entryRelPath.trim('/') == relFolder.trim('/')) continue

            if (entry.isDirectory) {
                walk(entryRelPath.trim('/'), depthLeft - 1, out)
            } else {
                val ext = entry.name.substringAfterLast('.', "").lowercase()
                val isImage = ext in IMAGE_EXT
                val isVideo = ext in VIDEO_EXT
                if (isImage || isVideo) {
                    out.add(
                        PhotoItem(
                            relativePath = entryRelPath.trim('/'),
                            davHref = entry.href.toString(),
                            name = entry.name,
                            modified = entry.modified ?: java.util.Date(0),
                            isVideo = isVideo
                        )
                    )
                }
            }
        }
    }

    /** Nextcloud's thumbnail endpoint — fast, server-generated, small payload. */
    fun thumbnailUrl(relativePath: String, size: Int = 300): String =
        "$serverUrl/index.php/apps/files/api/v1/thumbnail/$size/$size/${encodePath(relativePath)}"

    /** Full-resolution original, fetched straight over WebDAV. */
    fun originalUrl(relativePath: String): String =
        "$filesRoot/${encodePath(relativePath)}"

    fun basicAuthHeader(): String =
        okhttp3.Credentials.basic(username, appPassword)

    private fun hrefToRelativePath(href: String): String {
        val decoded = URLDecoder.decode(href, "UTF-8")
        val marker = "/remote.php/dav/files/$username/"
        val idx = decoded.indexOf(marker)
        return if (idx >= 0) decoded.substring(idx + marker.length) else decoded
    }

    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun encodePath(path: String) =
        path.trim('/').split("/").joinToString("/") { encode(it) }
}
