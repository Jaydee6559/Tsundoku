# Changelog

## 1.0

The first complete version of Tsundoku.

**Library**
- Finds every EPUB on internal storage (Android's file index plus a folder walk that skips media folders)
- Live updates for folders that hold books, plus a background sync every 6 hours
- Recognises moved and renamed books, so their reading data follows them
- Reads title, author, series, volume, cover, chapter count and tags from each EPUB
- Excluded folders; hidden folders (including Tsundoku's own Trash) are never scanned

**Browsing**
- Grid (small, medium, large, covers only) and list views, with show/hide options for details
- Instant search across titles, authors, series, EPUB tags and your tags
- Sorting, plus filters for status, series, location (e.g. ReadEra copies) and tag
- Multi-select and swipe actions

**Tracking**
- Reading status, chapter progress, rating, notes and your own tags
- Open in ReadEra
- Home with continue reading, an Up next queue and recently added
- Series with missing-volume detection and new-volume badges
- Reading stats and a yearly wrap-up
- Home-screen widget for the book you're reading

**Safety**
- Trash with Undo, restore and 30-day auto-delete
- Duplicate finder (exact copies and possible duplicates)
- Daily JSON backups with restore (matched by file fingerprint, so restores survive moves)
- Share → Tsundoku for EPUBs, with a duplicate warning
- Crash reports shown in the app on the next launch
- No internet permission

**Fixes before 1.0**
- Scans failed on Android because of a regex Android's engine rejects
- Deleted books came back because Android's file index still listed files in the Trash folder
