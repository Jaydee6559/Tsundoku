# Setup: signing key, releases, Obtainium and USB installs

One-time setup. After this, every push to `main` publishes a signed APK that
your phone can update to **without losing data**.

> 🔒 The signing key and its password **never** go into this repo.
> They live on your PC (plus a backup) and in GitHub **Secrets** (encrypted).

---

## 1. Create the signing key (PowerShell, once)

Run these one at a time in a PowerShell terminal (VS Code: `` Ctrl+` ``).

Make a private folder **outside** the repo:

```powershell
New-Item -ItemType Directory -Force "$HOME\Documents\Tsundoku-keys"
```

Create the key. It asks for a password twice: pick a strong one and save it in a password manager.

```powershell
& "C:\Program Files\Java\jdk-24\bin\keytool.exe" -genkeypair -v -keystore "$HOME\Documents\Tsundoku-keys\tsundoku-release.jks" -storetype PKCS12 -alias tsundoku -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Tsundoku"
```

`-dname "CN=Tsundoku"` keeps your real name out of the certificate (anyone can read an APK's certificate).

### ⚠️ Back it up now
Copy `tsundoku-release.jks` to a USB stick or a private cloud folder, together with the password.
If the key is lost, future updates can't install over the existing app.

---

## 2. Add the key to GitHub Secrets

Copy the key file to your clipboard as text (base64):

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$HOME\Documents\Tsundoku-keys\tsundoku-release.jks")) | Set-Clipboard
```

GitHub → your repo → **Settings → Secrets and variables → Actions → New repository secret**.
Create these 4 secrets (name must match exactly):

| Name | Value |
|---|---|
| `KEYSTORE_BASE64` | Paste (Ctrl+V) the clipboard text |
| `KEYSTORE_PASSWORD` | The password you chose |
| `KEY_ALIAS` | `tsundoku` |
| `KEY_PASSWORD` | The same password (PKCS12 keys use one password) |

Then clear the clipboard:

```powershell
Set-Clipboard -Value " "
```

---

## 3. Publish the first release

Push any commit to `main` (or GitHub → **Actions → Release (signed APK) → Run workflow**).
When it finishes, **Releases** (right side of the repo page) shows
`Tsundoku 1.0.N` with `tsundoku-1.0.N.apk` attached.

If the run fails at **"Check signing secrets exist"**, a secret name is misspelled or missing.

---

## 4. Obtainium on your phone (automatic updates)

1. Install **Obtainium** from its official GitHub releases page or F-Droid.
2. Obtainium → **Add App**.
3. **App source URL:** your repo's address, `https://github.com/<your-username>/Tsundoku`.
4. Since 1.0, builds are normal releases: **Include prereleases** is no longer needed (leaving it on is harmless).
5. Tap **Add**, then **Install**. Allow "Install unknown apps" for Obtainium when asked.
6. Optional: Obtainium settings → turn on background update checks.

From now on: push → wait about 5 minutes → Obtainium shows an update → tap **Update**.

> The old test build from Batch 1 is a separate app (`Tsundoku` debug, package `com.tsundoku.app.debug`).
> You can uninstall it.

---

## 5. One-command install over USB / wireless debugging (optional)

### Install Platform Tools (adb), once
1. Download **SDK Platform-Tools for Windows** from the official Android developer site.
2. Extract it to a folder, for example `C:\Android\platform-tools`.
3. Add that folder to PATH: Start → "Edit environment variables for your account" →
   **Path → Edit → New** → `C:\Android\platform-tools` → OK. Restart VS Code.
4. Check that it worked:

```powershell
adb version
```

### Connect the phone
- **USB:** plug in → accept the "Allow USB debugging?" prompt (only for your own PC).
- **Wireless:** Developer options → Wireless debugging → *Pair device with pairing code*, then:

```powershell
adb pair <ip:port-shown-for-pairing>
```

```powershell
adb connect <ip:port-shown-on-wireless-debugging-screen>
```

### Run it (from the repo folder)

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-latest.ps1
```

Extras:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-latest.ps1 -Launch
```

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-latest.ps1 -Logcat
```

`-Logcat` opens the app and streams its log. Send that output when something crashes.

> Turn USB / wireless debugging **off** when you're not developing.
