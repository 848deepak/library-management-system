#!/bin/bash

echo "Setting up Library Management System..."

# Check if dependencies exist, download them if not
if [ ! -f "sqlite-jdbc-3.45.0.0.jar" ]; then
  echo "Downloading SQLite JDBC driver..."
  curl -O https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.0.0/sqlite-jdbc-3.45.0.0.jar
fi

if [ ! -f "slf4j-api-2.0.9.jar" ]; then
  echo "Downloading SLF4J API..."
  curl -O https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
fi

if [ ! -f "slf4j-simple-2.0.9.jar" ]; then
  echo "Downloading SLF4J Simple implementation..."
  curl -O https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar
fi

# Compile the project
echo "Compiling the project..."
javac -cp .:sqlite-jdbc-3.45.0.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar LibraryManagementSystem.java

# Run the application
echo "Starting Library Management System..."
java -cp .:sqlite-jdbc-3.45.0.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar LibraryManagementSystem 