# Aurora Music Player Launcher
$jdkPath = "C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot\bin\java.exe"
$javaExe = if (Test-Path $jdkPath) { $jdkPath } else { "java" }

if (-not (Test-Path "aurora-player.jar")) {
    Write-Host "Building aurora-player.jar..." -ForegroundColor Cyan
    cmd /c build.bat
}

Write-Host "Launching Aurora Music Player..." -ForegroundColor Green
Start-Process $javaExe -ArgumentList "-jar aurora-player.jar" -WorkingDirectory $PSScriptRoot
