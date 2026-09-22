# NextGallery

A native Android app that browses your self-hosted Nextcloud photo library
in a Google Photos–style grid: date-grouped thumbnails, pull-to-refresh, and
a fullscreen pinch-to-zoom swipeable viewer.

It talks directly to your Nextcloud instance over WebDAV — no server-side
app needed, and nothing is uploaded anywhere else.

## What it does

- **Login screen** — server URL, username, app password, and an optional
  subfolder to limit browsing (e.g. `Photos`, leave blank for the whole account).
- **Gallery grid** — recursively walks the folder, groups everything by
  "Today / Yesterday / Month Day" like Google Photos, newest first. Thumbnails
  come from Nextcloud's built-in thumbnail endpoint so it stays fast even with
  thousands of files.
- **Fullscreen viewer** — swipe between photos, pinch to zoom, loads the
  original file over WebDAV.
- Credentials are stored in `EncryptedSharedPreferences`, not plaintext.

## What it doesn't do (yet)

This is a solid first build, not a Google Photos clone in full: no camera
auto-upload, no offline caching beyond Glide's disk cache, no video playback
(video files are listed but tapping one currently tries to load it as an
image — swap in an `ExoPlayer` page for those if you want that).

## Before you open it in Android Studio

1. **Get an app password**, don't use your real Nextcloud password:
   Nextcloud → Settings → Security → *Devices & Sessions* → Create new app password.
2. Make sure your server is reachable from the phone — since you're already
   running Nextcloud behind a Cloudflare Tunnel at `nc.idealmakina.co`, just
   use `https://nc.idealmakina.co` as the server URL and you're set.

## Building it

1. Open this folder in **Android Studio** (Hedgehog/2023.1 or newer). It will
   offer to generate the Gradle wrapper jar automatically on first sync —
   accept that, since the jar itself isn't included here (binary files don't
   travel well outside a build environment). If it doesn't prompt you,
   run **File → Sync Project with Gradle Files**.
2. First sync will pull in Sardine (WebDAV), Glide, and PhotoView from
   JitPack/Maven Central — this needs an internet connection once.
3. Run it on a device or emulator (`minSdk 26`, Android 8.0+).

## Project layout

```
app/src/main/java/com/hakki/nextgallery/
  LoginActivity.kt          server/user/pass entry + validates by listing the folder once
  MainActivity.kt           the photo grid + date headers + pull-to-refresh
  PhotoViewerActivity.kt    fullscreen ViewPager2 with pinch-zoom pages
  api/NextcloudClient.kt    WebDAV walk, thumbnail URL, original URL, auth header
  model/PhotoItem.kt        data classes for a media file / grid row
  ui/GalleryAdapter.kt      RecyclerView adapter (header rows span full width)
  util/Prefs.kt             encrypted storage for server/user/pass/folder
```

## Getting an APK without installing Android Studio

This repo includes a GitHub Actions workflow (`.github/workflows/build-apk.yml`)
that builds a debug APK in the cloud and hands it back to you as a download —
no local Android SDK needed.

1. Create a new (can be private) GitHub repo and push this folder to it:
   ```
   cd NextGallery
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<you>/NextGallery.git
   git push -u origin main
   ```
2. On GitHub, open the repo's **Actions** tab. The "Build APK" workflow runs
   automatically on that push (or trigger it manually via **Run workflow**).
3. Once it finishes (a couple of minutes), open the completed run and scroll
   to **Artifacts** — download `NextGallery-debug-apk`, which contains
   `app-debug.apk`.
4. Copy that APK to your phone and install it. Since it's not from the Play
   Store, Android will ask you to allow installs from that source
   (Settings → apps → "install unknown apps", pick the app you used to open
   the file — Files, Chrome, whichever).

This produces a **debug** build, which is fine for your own phone. If you
ever want to distribute it more broadly, it would need to be signed with a
release key instead.

## Easy next steps

- **Video playback**: swap the `PhotoView` page for an `ExoPlayer` page when
  `PhotoItem.isVideo` is true.
- **Camera auto-upload**: add a `WorkManager` job that watches
  `MediaStore` for new photos and `sardine.put()`s them to a configurable
  upload folder.
- **Offline albums**: Glide already disk-caches thumbnails; add a "keep
  originals offline" toggle per album for full offline viewing.
