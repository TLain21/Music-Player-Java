@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo     Building Aurora Music Player (Java 21)
echo ===================================================

if not exist bin mkdir bin

set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot
if exist "%JAVA_HOME%\bin\javac.exe" (
    set JAVAC_CMD="%JAVA_HOME%\bin\javac.exe"
    set JAR_CMD="%JAVA_HOME%\bin\jar.exe"
) else (
    set JAVAC_CMD=javac
    set JAR_CMD=jar
)

echo Compiling Java source files...
powershell -Command "Get-ChildItem -Path 'src\main\java' -Filter '*.java' -Recurse | ForEach-Object { '\"' + $_.FullName.Replace('\', '/') + '\"' } | Set-Content -Encoding ASCII sources.txt"

%JAVAC_CMD% -encoding UTF-8 -d bin -cp "lib/*" @sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed!
    del sources.txt 2>nul
    exit /b 1
)
del sources.txt 2>nul

echo Creating manifest...
(
echo Manifest-Version: 1.0
echo Main-Class: com.aurora.Main
echo Class-Path: lib/flatlaf-3.5.4.jar lib/jaudiotagger-3.0.1.jar lib/jflac-codec-1.5.2.jar lib/jlayer-1.0.1.4.jar lib/jorbis-0.0.17.4.jar lib/mp3spi-1.9.5.4.jar lib/tritonus-share-0.3.7.4.jar lib/vorbisspi-1.0.3.3.jar
echo.
) > manifest.txt

echo Packaging aurora-player.jar...
%JAR_CMD% cfm aurora-player.jar manifest.txt -C bin .
del manifest.txt 2>nul

echo.
echo ===================================================
echo   [SUCCESS] Build Complete: aurora-player.jar
echo   Run using run.bat or java -jar aurora-player.jar
echo ===================================================
