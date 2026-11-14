<#
.SYNOPSIS
    Downloads the newest Tsundoku release APK from GitHub and installs it on the
    phone connected with USB or wireless debugging.

.EXAMPLE
    .\scripts\install-latest.ps1            # download + install newest build
    .\scripts\install-latest.ps1 -Logcat    # ...then show Tsundoku's live log (Ctrl+C to stop)
    .\scripts\install-latest.ps1 -Launch    # ...then open the app

.NOTES
    Needs only adb (Android Platform Tools). No GitHub login: the repo is public.
    Updates keep app data because every release is signed with the same key.
#>
[CmdletBinding()]
param(
    [switch]$Logcat,
    [switch]$Launch
)

$ErrorActionPreference = 'Stop'
$PackageName = 'com.tsundoku.app'

function Fail($msg) { Write-Host "x $msg" -ForegroundColor Red; exit 1 }
function Step($msg) { Write-Host "> $msg" -ForegroundColor Cyan }

# --- 1. Tools and phone ------------------------------------------------------
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Fail "adb not found. Install Android Platform Tools and add its folder to PATH (see docs/SETUP-SIGNING.md)."
}

$devices = (& adb devices) | Select-Object -Skip 1 | Where-Object { $_ -match '\S+\s+device$' }
if (-not $devices) {
    Fail "No phone found. Connect with USB (allow the debugging prompt) or pair wireless debugging, then retry."
}
if (@($devices).Count -gt 1) {
    Fail "More than one device connected. Disconnect the extras (or set ANDROID_SERIAL) and retry."
}

# --- 2. Which repo? (read from git, so nothing is hard-coded) ----------------
$remote = (& git -C $PSScriptRoot remote get-url origin) 2>$null
if ($remote -notmatch 'github\.com[:/](?<owner>[^/]+)/(?<repo>[^/.]+)') {
    Fail "Couldn't read the GitHub repo from 'git remote'. Run this inside the Tsundoku folder."
}
$repo = "$($Matches.owner)/$($Matches.repo)"

# --- 3. Newest release (includes dev pre-releases) ---------------------------
Step "Checking newest release of $repo ..."
$headers = @{ 'User-Agent' = 'tsundoku-install-script'; 'Accept' = 'application/vnd.github+json' }
$releases = Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases?per_page=1" -Headers $headers
if (-not $releases) { Fail "No releases yet. Push to main and wait for the 'Release' workflow to finish." }
$release = @($releases)[0]

$apkAsset = $release.assets | Where-Object { $_.name -like '*.apk' } | Select-Object -First 1
$shaAsset = $release.assets | Where-Object { $_.name -like '*.apk.sha256' } | Select-Object -First 1
if (-not $apkAsset) { Fail "Release $($release.tag_name) has no APK attached." }

# --- 4. Download + verify checksum -------------------------------------------
$dir = Join-Path $env:TEMP 'tsundoku-apk'
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$apkPath = Join-Path $dir $apkAsset.name

Step "Downloading $($apkAsset.name) ($([math]::Round($apkAsset.size / 1MB, 1)) MB) ..."
Invoke-WebRequest -Uri $apkAsset.browser_download_url -OutFile $apkPath -Headers $headers -UseBasicParsing

if ($shaAsset) {
    $shaPath = "$apkPath.sha256"
    Invoke-WebRequest -Uri $shaAsset.browser_download_url -OutFile $shaPath -Headers $headers -UseBasicParsing
    $expected = ((Get-Content -Raw -Path $shaPath) -split '\s+')[0].Trim().ToLower()
    Remove-Item $shaPath -Force
    $actual = (Get-FileHash -Algorithm SHA256 -Path $apkPath).Hash.ToLower()
    if ($expected -ne $actual) { Remove-Item $apkPath -Force; Fail "Checksum mismatch. Download deleted, try again." }
    Write-Host "  checksum OK" -ForegroundColor DarkGray
}

# --- 5. Install --------------------------------------------------------------
Step "Installing on phone ..."
$out = (& adb install -r $apkPath) 2>&1 | Out-String
if ($out -notmatch 'Success') {
    if ($out -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') {
        Fail "The installed Tsundoku was signed with a different key (an old test build). Uninstall it on the phone once, then rerun."
    }
    Fail "Install failed:`n$out"
}
Remove-Item $apkPath -Force -ErrorAction SilentlyContinue
Write-Host "Installed $($release.tag_name)" -ForegroundColor Green

# --- 6. Optional extras ------------------------------------------------------
if ($Launch -or $Logcat) {
    & adb shell monkey -p $PackageName -c android.intent.category.LAUNCHER 1 | Out-Null
}
if ($Logcat) {
    Start-Sleep -Seconds 2
    $appPid = (& adb shell pidof $PackageName).Trim()
    if (-not $appPid) { Fail "App isn't running, so there's no log to show." }
    Step "Live log for $PackageName (Ctrl+C to stop) ..."
    & adb logcat --pid=$appPid
}
