@ECHO OFF
REM AI-assisted: minimal Maven wrapper for Windows.
REM Downloads and caches the Maven version pinned in .mvn/wrapper/maven-wrapper.properties,
REM then runs it with the given arguments. Lets a fresh clone build without Maven pre-installed.

SETLOCAL ENABLEDELAYEDEXPANSION

SET WRAPPER_DIR=%~dp0.mvn\wrapper
SET PROPS_FILE=%WRAPPER_DIR%\maven-wrapper.properties
SET CACHE_DIR=%USERPROFILE%\.m2\wrapper\dists

IF NOT EXIST "%PROPS_FILE%" (
    ECHO Could not find %PROPS_FILE% 1>&2
    EXIT /B 1
)

FOR /F "usebackq tokens=1,* delims==" %%A IN ("%PROPS_FILE%") DO (
    IF "%%A"=="distributionUrl" SET DIST_URL=%%B
)

IF "%DIST_URL%"=="" (
    ECHO distributionUrl not set in %PROPS_FILE% 1>&2
    EXIT /B 1
)

FOR %%F IN ("%DIST_URL%") DO SET DIST_ZIP_NAME=%%~nxF
SET DIST_DIR_NAME=%DIST_ZIP_NAME:-bin.zip=%
SET MAVEN_HOME=%CACHE_DIR%\%DIST_DIR_NAME%

IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
    ECHO Downloading %DIST_URL% ...
    IF NOT EXIST "%CACHE_DIR%" MKDIR "%CACHE_DIR%"
    SET ZIP_PATH=%CACHE_DIR%\%DIST_ZIP_NAME%
    powershell -NoProfile -ExecutionPolicy Bypass -Command "try { Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP_PATH%' -UseBasicParsing } catch { Write-Error $_; exit 1 }"
    IF ERRORLEVEL 1 (
        ECHO Failed to download Maven distribution from %DIST_URL% 1>&2
        EXIT /B 1
    )
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%ZIP_PATH%' -DestinationPath '%CACHE_DIR%' -Force"
    IF ERRORLEVEL 1 (
        ECHO Failed to extract Maven distribution 1>&2
        EXIT /B 1
    )
    DEL "%ZIP_PATH%"
)

CALL "%MAVEN_HOME%\bin\mvn.cmd" %*
EXIT /B %ERRORLEVEL%
