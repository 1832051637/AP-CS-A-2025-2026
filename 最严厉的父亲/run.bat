@echo off
setlocal

set "PROJECT_DIR=%~dp0"
set "MAVEN_VERSION=3.9.16"
set "MAVEN_TOOLS=%PROJECT_DIR%.tools"
set "MAVEN_HOME=%MAVEN_TOOLS%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%TEMP%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

where java >nul 2>nul
if errorlevel 1 (
    echo Java was not found in PATH.
    echo Install JDK 21, then reopen your terminal.
    echo Example: winget install EclipseAdoptium.Temurin.21.JDK
    exit /b 1
)

where mvn >nul 2>nul
if errorlevel 1 (
    echo Maven was not found in PATH.
    echo Downloading Maven %MAVEN_VERSION% into "%MAVEN_TOOLS%"...

    if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
        if not exist "%MAVEN_TOOLS%" mkdir "%MAVEN_TOOLS%"
        powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri $env:MAVEN_URL -OutFile $env:MAVEN_ZIP; Expand-Archive -Force -Path $env:MAVEN_ZIP -DestinationPath $env:MAVEN_TOOLS"
        if errorlevel 1 (
            echo Failed to download Maven.
            echo You can install Maven manually from https://maven.apache.org/download.cgi
            exit /b 1
        )
    )

    call "%MAVEN_HOME%\bin\mvn.cmd" -f "%PROJECT_DIR%pom.xml" org.openjfx:javafx-maven-plugin:0.0.8:run %*
    exit /b %ERRORLEVEL%
)

call mvn -f "%PROJECT_DIR%pom.xml" org.openjfx:javafx-maven-plugin:0.0.8:run %*
exit /b %ERRORLEVEL%
