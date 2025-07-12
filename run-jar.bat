@echo off
echo Starting Library Management System...
echo.
echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 8 or higher and try again
    echo.
    pause
    exit /b 1
)

echo Java found! Starting application...
java -jar LibraryManagementSystem-Standalone.jar

if %errorlevel% neq 0 (
    echo.
    echo ERROR: Failed to start the application
    echo Make sure the JAR file is in the same directory
    pause
)
