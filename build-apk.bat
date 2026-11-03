@echo off
setlocal EnableExtensions
cd /d "%~dp0"

if not defined ANDROID_HOME if exist "%LOCALAPPDATA%\Android\Sdk" set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if not defined ANDROID_SDK_ROOT if defined ANDROID_HOME set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
if not defined JAVA_HOME if exist "%ProgramFiles%\Microsoft\jdk-21.0.12.8-hotspot" set "JAVA_HOME=%ProgramFiles%\Microsoft\jdk-21.0.12.8-hotspot"
if not defined JAVA_HOME if exist "%ProgramFiles%\Android\Android Studio\jbr" set "JAVA_HOME=%ProgramFiles%\Android\Android Studio\jbr"
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ============================================================
echo   Ambio Mix - Building Signed Release APK
echo ============================================================

call "%~dp0gradlew.bat" assembleRelease
if errorlevel 1 (
    echo.
    echo BUILD FAILED. Check JDK and Android SDK.
    pause
    exit /b 1
)

echo.
echo ============================================================
echo   BUILD SUCCESSFUL!
echo   Signed Release APK created at:
echo   %~dp0app\build\outputs\apk\release\app-release.apk
echo ============================================================
pause
