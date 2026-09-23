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
