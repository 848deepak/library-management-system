#!/bin/bash

echo "🚀 Starting Library Management System..."
echo

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ ERROR: Java is not installed or not in PATH"
    echo "Please install Java 8 or higher and try again"
    echo "Download from: https://adoptium.net/"
    exit 1
fi

echo "☕ Java found! Starting application..."
java -jar LibraryManagementSystem-Standalone.jar

if [ $? -ne 0 ]; then
    echo
    echo "❌ ERROR: Failed to start the application"
    echo "Make sure the JAR file is in the same directory"
    read -p "Press Enter to continue..."
fi
