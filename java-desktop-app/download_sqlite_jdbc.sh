#!/bin/bash

# Download SQLite JDBC Driver
echo "Downloading SQLite JDBC Driver..."
curl -L -o sqlite-jdbc-3.43.0.0.jar https://github.com/xerial/sqlite-jdbc/releases/download/3.43.0.0/sqlite-jdbc-3.43.0.0.jar
echo "Download complete. The SQLite JDBC driver has been saved as sqlite-jdbc-3.43.0.0.jar"
echo "To compile and run the application with database support, use:"
echo "javac -cp .:sqlite-jdbc-3.43.0.0.jar *.java"
echo "java -cp .:sqlite-jdbc-3.43.0.0.jar LibraryManagementSystem" 