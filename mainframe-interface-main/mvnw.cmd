@ECHO OFF
REM AI-assisted: minimal Maven wrapper for Windows.
REM Downloads and caches the Maven version pinned in .mvn/wrapper/maven-wrapper.properties,
REM then runs it with the given arguments. Lets a fresh clone build without Maven pre-installed.
REM Variables are set outside IF blocks and failures jump to :fail, because cmd expands
REM %VAR% inside a block before the block runs, and EXIT /B inside a block can lose its code.

SETLOCAL

SET "WRAPPER_DIR=%~dp0.mvn\wrapper"
SET "PROPS_FILE=%WRAPPER_DIR%\maven-wrapper.properties"
SET "CACHE_DIR=%USERPROFILE%\.m2\wrapper\dists"

IF NOT EXIST "%PROPS_FILE%" SET "ERROR_TEXT=Could not find %PROPS_FILE%" & GOTO fail

SET "DIST_URL="
FOR /F "usebackq tokens=1,* delims==" %%A IN ("%PROPS_FILE%") DO IF "%%A"=="distributionUrl" SET "DIST_URL=%%B"
IF "%DIST_URL%"=="" SET "ERROR_TEXT=distributionUrl not set in %PROPS_FILE%" & GOTO fail

FOR %%F IN ("%DIST_URL%") DO SET "DIST_ZIP_NAME=%%~nxF"
SET "DIST_DIR_NAME=%DIST_ZIP_NAME:-bin.zip=%"
SET "MAVEN_HOME=%CACHE_DIR%\%DIST_DIR_NAME%"
SET "ZIP_PATH=%CACHE_DIR%\%DIST_ZIP_NAME%"

IF EXIST "%MAVEN_HOME%\bin\mvn.cmd" GOTO run

ECHO Downloading %DIST_URL% ...
IF NOT EXIST "%CACHE_DIR%" MKDIR "%CACHE_DIR%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference = 'SilentlyContinue'; try { Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP_PATH%' -UseBasicParsing } catch { Write-Error $_; exit 1 }"
IF ERRORLEVEL 1 SET "ERROR_TEXT=Failed to download Maven distribution from %DIST_URL%" & GOTO fail
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference = 'SilentlyContinue'; try { Expand-Archive -LiteralPath '%ZIP_PATH%' -DestinationPath '%CACHE_DIR%' -Force } catch { Write-Error $_; exit 1 }"
IF ERRORLEVEL 1 SET "ERROR_TEXT=Failed to extract Maven distribution" & GOTO fail
DEL "%ZIP_PATH%"
IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" SET "ERROR_TEXT=Maven was not found at %MAVEN_HOME% after extracting" & GOTO fail

:run
CALL "%MAVEN_HOME%\bin\mvn.cmd" %*
EXIT /B %ERRORLEVEL%

:fail
ECHO %ERROR_TEXT% 1>&2
EXIT /B 1
