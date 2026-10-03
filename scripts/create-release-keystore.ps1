param(
  [string]$Alias = "socialwork",
  [string]$Output = "socialwork-release.keystore"
)

Write-Host "This creates the ONE signing key that should be reused for every future release." -ForegroundColor Cyan
Write-Host "Keep the keystore and passwords safe. Do NOT commit the keystore to Git." -ForegroundColor Yellow

keytool -genkeypair -v `
  -keystore $Output `
  -alias $Alias `
  -keyalg RSA `
  -keysize 2048 `
  -validity 10000

if ($LASTEXITCODE -ne 0) { throw "keytool failed" }

$bytes = [System.IO.File]::ReadAllBytes((Resolve-Path $Output))
$b64 = [Convert]::ToBase64String($bytes)
$txt = "$Output.base64.txt"
[System.IO.File]::WriteAllText($txt, $b64)

Write-Host "Created: $Output" -ForegroundColor Green
Write-Host "Created Base64 copy for GitHub Secret: $txt" -ForegroundColor Green
Write-Host "GitHub Secrets to add:" -ForegroundColor Cyan
Write-Host "ANDROID_KEYSTORE_BASE64 = contents of $txt"
Write-Host "ANDROID_KEYSTORE_PASSWORD = keystore password"
Write-Host "ANDROID_KEY_ALIAS = $Alias"
Write-Host "ANDROID_KEY_PASSWORD = key password"
