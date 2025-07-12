#!/bin/bash

echo "🚀 Building Library Management System JAR..."

# Create temporary directory for extracted dependencies
mkdir -p temp_jar_build
cd temp_jar_build

# Extract all dependency JARs
echo "📦 Extracting dependencies..."
jar xf ../sqlite-jdbc-3.45.0.0.jar
jar xf ../slf4j-api-2.0.9.jar  
jar xf ../slf4j-simple-2.0.9.jar

# Remove META-INF to avoid conflicts (keep only our manifest)
rm -rf META-INF

# Copy our compiled classes
echo "📋 Copying application classes..."
cp ../*.class .

# Copy database file
cp ../library.db .

# Create the final JAR with our manifest
echo "🔧 Creating executable JAR..."
jar cfm ../LibraryManagementSystem-Standalone.jar ../MANIFEST.MF .

# Clean up
cd ..
rm -rf temp_jar_build

echo "✅ Standalone JAR created: LibraryManagementSystem-Standalone.jar"
echo "🎯 Run with: java -jar LibraryManagementSystem-Standalone.jar"

# Show file size
ls -lh LibraryManagementSystem-Standalone.jar
