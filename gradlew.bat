@echo off
setlocal EnableExtensions

set "APP_HOME=%~dp0"
if defined JAVA_HOME (
    set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA_EXE=java.exe"
)

"%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=gradlew" -jar "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" %*
exit /b %ERRORLEVEL%
