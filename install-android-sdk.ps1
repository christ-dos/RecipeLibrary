# Install Android command-line tools, platform-tools, platforms and build-tools
# Run PowerShell as Administrator before executing.

$ErrorActionPreference = 'Stop'

$sdkRoot = "$env:USERPROFILE\AppData\Local\Android\Sdk"
if (-not (Test-Path $sdkRoot)) {
  New-Item -ItemType Directory -Force -Path $sdkRoot | Out-Null
}

$zipUrl = 'https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip'
$zipPath = Join-Path $sdkRoot 'cmdline-tools.zip'

Write-Host "Downloading command-line tools to $zipPath ..."
Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath

Write-Host "Extracting..."
Expand-Archive -Path $zipPath -DestinationPath $sdkRoot -Force

# Normalize folder layout: move contents into cmdline-tools/latest
$cmdlineRoot = Join-Path $sdkRoot 'cmdline-tools'
$latestPath = Join-Path $cmdlineRoot 'latest'
if (-not (Test-Path $latestPath)) { New-Item -ItemType Directory -Force -Path $latestPath | Out-Null }

# Move any extracted subfolders' contents into cmdline-tools/latest
Get-ChildItem -Path $cmdlineRoot -Directory | ForEach-Object {
    if ($_.Name -ne 'latest') {
        Get-ChildItem -Path $_.FullName -Force | ForEach-Object {
            Move-Item -Path $_.FullName -Destination $latestPath -Force
        }
    }
}

Remove-Item -Path $zipPath -Force -ErrorAction SilentlyContinue

# Ensure environment variables (persist)
setx ANDROID_SDK_ROOT $sdkRoot | Out-Null
setx ANDROID_HOME $sdkRoot | Out-Null
$env:ANDROID_SDK_ROOT = $sdkRoot
$env:ANDROID_HOME = $sdkRoot

$sdkManager = Join-Path $latestPath 'bin\sdkmanager.bat'
if (-not (Test-Path $sdkManager)) { Write-Error "sdkmanager not found at $sdkManager" }

Write-Host "Installing platform-tools, platforms and build-tools (may take several minutes)..."
& $sdkManager --sdk_root=$sdkRoot "platform-tools" "platforms;android-34" "build-tools;34.0.0" "cmdline-tools;latest"

Write-Host "Accepting licenses..."
cmd /c "echo y | `"$sdkManager`" --licenses --sdk_root=$sdkRoot"

Write-Host "Android SDK components installed. You may need to restart your shell to pick up environment variables."