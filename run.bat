@echo off
setlocal

set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot
if exist "%JAVA_HOME%\bin\javaw.exe" (
    set JAVA_RUN="%JAVA_HOME%\bin\javaw.exe"
) else if exist "%JAVA_HOME%\bin\java.exe" (
    set JAVA_RUN="%JAVA_HOME%\bin\java.exe"
) else (
    set JAVA_RUN=javaw
)

if not exist aurora-player.jar (
    echo Building project first...
    call build.bat
)

echo Starting Aurora Music Player...
start "" %JAVA_RUN% -jar aurora-player.jar
