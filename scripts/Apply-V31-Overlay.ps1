# Apply to a LOCAL clone of SocialWork-Flashcards, preserving .git, user changes and main-card integrity.
# Usage: .\scripts\Apply-V31-Overlay.ps1 -ProjectDir 'D:\AndroidProjects\SocialWork-Flashcards'
param(
    [Parameter(Mandatory = $true)][string]$ProjectDir,
    [string]$OverlayDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)
$ErrorActionPreference = 'Stop'
$ProjectDir = (Resolve-Path $ProjectDir).Path
$OverlayDir = (Resolve-Path $OverlayDir).Path
if ($ProjectDir -eq $OverlayDir) { throw 'Cannot use the patch package itself as the target project.' }
$manifestFile = Join-Path $OverlayDir 'V31_OVERLAY_MANIFEST.json'
if (-not (Test-Path $manifestFile)) { throw "Missing overlay manifest: $manifestFile" }
$manifest = Get-Content -Raw -Encoding UTF8 $manifestFile | ConvertFrom-Json
if ($manifest.schema_version -ne 'V31_PATCH_1') { throw 'Unexpected overlay format.' }
$oldMain = Join-Path $ProjectDir 'app\src\main\assets\cards.json'
if (-not (Test-Path $oldMain)) { throw 'This is not the expected Android project root (cards.json missing).' }
$expectedMaster = $manifest.protected_main_cards_sha256
$actualMaster = (Get-FileHash -LiteralPath $oldMain -Algorithm SHA256).Hash.ToLowerInvariant()
if ($expectedMaster -ne $actualMaster) { throw "Protected main-cards base differs: $actualMaster. Aborted without writing." }
# PRE-FLIGHT: verify every source file and all target prior versions before writing anything.
foreach ($entry in $manifest.files) {
    $relative = [string]$entry.path
    if ([IO.Path]::IsPathRooted($relative) -or $relative.StartsWith('..') -or $relative.Contains('/../') -or $relative.Contains('\\..\\')) { throw "Unsafe path: $relative" }
    $src = Join-Path $OverlayDir $relative
    $dst = Join-Path $ProjectDir $relative
    if (-not (Test-Path -LiteralPath $src -PathType Leaf)) { throw "Missing patch source: $relative" }
    $srcHash = (Get-FileHash -LiteralPath $src -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($srcHash -ne $entry.new_sha256) { throw "Patch source hash mismatched: $relative" }
    if ($null -ne $entry.expected_old_sha256 -and $entry.expected_old_sha256 -ne '') {
        if (-not (Test-Path -LiteralPath $dst -PathType Leaf)) { throw "Base file missing: $relative" }
        $oldHash = (Get-FileHash -LiteralPath $dst -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($oldHash -ne $entry.expected_old_sha256) { throw "Local file changed, cannot overwrite automatically: $relative. Use manual merge." }
    } elseif (Test-Path -LiteralPath $dst) { throw "Patch would collide with an existing new file: $relative" }
}
$backupDir = Join-Path (Split-Path $ProjectDir -Parent) ((Split-Path $ProjectDir -Leaf) + '_backup_before_v31_' + (Get-Date -Format 'yyyyMMdd_HHmmss'))
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
$applied = New-Object 'System.Collections.Generic.List[string]'
try {
    foreach ($entry in $manifest.files) {
        $relative = [string]$entry.path
        $dst = Join-Path $ProjectDir $relative
        if (Test-Path -LiteralPath $dst) {
            $save = Join-Path $backupDir $relative
            New-Item -ItemType Directory -Path (Split-Path $save -Parent) -Force | Out-Null
            Copy-Item -LiteralPath $dst -Destination $save -Force
        }
        New-Item -ItemType Directory -Path (Split-Path $dst -Parent) -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $OverlayDir $relative) -Destination $dst -Force
        $applied.Add($relative)
    }
} catch {
    foreach ($relative in $applied) {
        $dst = Join-Path $ProjectDir $relative
        $saved = Join-Path $backupDir $relative
        if (Test-Path $saved) { Copy-Item -LiteralPath $saved -Destination $dst -Force }
        elseif (Test-Path $dst) { Remove-Item -LiteralPath $dst -Force }
    }
    throw
}
Write-Host "PASS: V3.1 patch installed: $($applied.Count) files. Backup folder: $backupDir"
Write-Host 'Next: python scripts/verify_v31_integrated.py ; git diff ; push a feature branch to trigger GitHub Actions.'
