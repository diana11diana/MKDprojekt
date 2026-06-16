@ECHO OFF
SETLOCAL
SET "BASE_DIR=%~dp0"
SET "MAVEN_CACHE=%TEMP%\dsms-maven-wrapper"
SET "MAVEN_HOME=%MAVEN_CACHE%\apache-maven-3.9.9"
SET "MAVEN_ZIP=%MAVEN_CACHE%\apache-maven-3.9.9-bin.zip"

IF NOT EXIST "%MAVEN_HOME%\bin\mvn.cmd" (
  ECHO Maven 3.9.9 is not installed in the project. Downloading...
  IF NOT EXIST "%MAVEN_CACHE%" MKDIR "%MAVEN_CACHE%"
  powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '%MAVEN_ZIP%'; Expand-Archive -LiteralPath '%MAVEN_ZIP%' -DestinationPath '%MAVEN_CACHE%' -Force"
  IF ERRORLEVEL 1 EXIT /B 1
)

CALL "%MAVEN_HOME%\bin\mvn.cmd" %*
ENDLOCAL
