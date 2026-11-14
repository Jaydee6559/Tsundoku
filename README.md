# 📚 Tsundoku

> *積ん読 (tsundoku)*: buying books and letting them pile up unread.

**Tsundoku** is a calm, offline Android app for keeping a big EPUB novel library in order.
It finds every EPUB on your phone, keeps the list up to date by itself, and tracks what you read,
while your favourite reader app ([ReadEra](https://readera.org/)) stays the place where you actually read.

**Version 1.0** · Android 11+ · Kotlin + Jetpack Compose · no internet access

---

## ✨ Features

### Your library, found automatically
- 🔍 Finds **every `.epub` on internal storage**, wherever it is (Download, Telegram, Documents…)
- ⚡ **Live updates**: new downloads appear within seconds; moved, renamed or deleted files are noticed
- 🔄 Background sync every few hours, even when the app is closed
- 🚫 Excluded folders, for anything you don't want in the library
- 🧩 Reads title, author, series, volume, cover and **tags** from each EPUB, without ever changing the file

### Browse your way
- 🖼️ Cover **grid** (small / medium / large / covers only) or detailed **list**
- 🔎 Instant search across titles, authors, series and tags
- ↕️ Sort by recently added, title, author, series or size
- 🏷️ Filter by reading status, series, **location** (e.g. copies ReadEra saved) and **tag**
- 👆 Long-press to select many books; swipe to set status or delete

### Track your reading
- 📖 Status: Plan to read · Reading · Completed · On hold · Dropped
- 📈 Chapter progress, ⭐ rating, notes and your own tags
- ▶️ **Open in ReadEra** with one tap
- 🏠 Home: continue reading, an **Up next** queue, recently added
- 📚 **Series** with missing-volume detection ("Missing Vol 3") and new-volume badges
- 📊 **Reading stats** and a yearly wrap-up
- 🧱 **Home-screen widget** showing the book you're reading

### Safe by design
- 🗑️ Deleting moves books to **Trash** (Undo, restore for 30 days)
- 🧑‍🤝‍🧑 **Duplicate finder** for books saved twice
- 💾 **Daily backups** of all your reading data (statuses, progress, notes, tags, queue), with restore
- 📤 **Share → Tsundoku** to add an EPUB from Chrome, Telegram or a file manager
- 🔒 **No internet permission at all.** Nothing you do ever leaves your phone.

---

## 📥 Install

1. Install [Obtainium](https://github.com/ImranR98/Obtainium) on your phone.
2. In Obtainium, **Add App** and paste this repository's URL.
3. Tap **Install**. Future updates arrive through Obtainium and keep your data.

Or download the APK from the [Releases](../../releases) page.

On first launch Tsundoku asks for **All files access**, so it can find EPUBs in every folder.
It never edits your EPUB files.

---

## 🛠️ Built with

| Layer | Tech |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose · Material 3 · Glance (widget) |
| Data | Room (SQLite) · DataStore |
| Background | Coroutines · Flow · WorkManager · FileObserver |
| Build | Gradle wrapper · GitHub Actions (signed releases) |

Titles use the [Lora](https://github.com/cyrealtype/Lora-Cyrillic) typeface under the
SIL Open Font License ([license](docs/licenses/Lora-OFL.txt)).

### Build it yourself
```bash
./gradlew :app:assembleDebug
```
Needs JDK 17 and the Android SDK. Release signing uses keys from environment variables only
(see [docs/SETUP-SIGNING.md](docs/SETUP-SIGNING.md)); no keys are stored in this repository.

---

## 🔐 Privacy

- No internet permission: no accounts, analytics, ads or tracking.
- Your library and reading data stay in the app's private storage and in backups on your own phone.
- EPUBs are only read, never modified. Deleting always goes through Trash first.

## 📄 License

[MIT](LICENSE)
