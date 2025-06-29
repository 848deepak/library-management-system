@echo off
echo Setting up Library Management System...

REM Check if dependencies exist, download them if not
if not exist sqlite-jdbc-3.45.0.0.jar (
  echo Downloading SQLite JDBC driver...
  curl -O https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.0.0/sqlite-jdbc-3.45.0.0.jar
)

if not exist slf4j-api-2.0.9.jar (
  echo Downloading SLF4J API...
  curl -O https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
)

if not exist slf4j-simple-2.0.9.jar (
  echo Downloading SLF4J Simple implementation...
  curl -O https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar
)

REM Compile the project
echo Compiling the project...
javac -cp .;sqlite-jdbc-3.45.0.0.jar;slf4j-api-2.0.9.jar;slf4j-simple-2.0.9.jar LibraryManagementSystem.java

REM Run the application
echo Starting Library Management System...
java -cp .;sqlite-jdbc-3.45.0.0.jar;slf4j-api-2.0.9.jar;slf4j-simple-2.0.9.jar LibraryManagementSystem 