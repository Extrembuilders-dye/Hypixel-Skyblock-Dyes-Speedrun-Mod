@echo off
setlocal
java -version
if errorlevel 1 (
  echo Java 25 is required.
  exit /b 1
)
gradle --version
if errorlevel 1 (
  echo Gradle 9.5.1 is required. See README.md.
  exit /b 1
)
gradle clean build
if errorlevel 1 exit /b 1
echo.
echo Build complete. JARs are in build\libs\
