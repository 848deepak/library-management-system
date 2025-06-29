# Library Management System

A comprehensive library management system with student authentication, book management, and gamification features.

## Features

- Book management (add, remove, search)
- Student authentication
- Book borrowing and returning
- Book ratings and reviews
- Gamification system with achievements and points
- Department-specific user management
- Admin and student interfaces

## Requirements

- Java JDK 11 or higher
- Internet connection (for first run to download dependencies)

## Quick Start

### On macOS/Linux:

1. Open Terminal in the project directory
2. Run: `./run.sh`

### On Windows:

1. Open Command Prompt in the project directory
2. Run: `run.bat`

## Running Manually

If you prefer to run the application manually:

1. Download the required dependencies:
   ```
   curl -O https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.45.0.0/sqlite-jdbc-3.45.0.0.jar
   curl -O https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar
   curl -O https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar
   ```

2. Compile the project:
   ```
   # On macOS/Linux
   javac -cp .:sqlite-jdbc-3.45.0.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar LibraryManagementSystem.java

   # On Windows
   javac -cp .;sqlite-jdbc-3.45.0.0.jar;slf4j-api-2.0.9.jar;slf4j-simple-2.0.9.jar LibraryManagementSystem.java
   ```

3. Run the application:
   ```
   # On macOS/Linux
   java -cp .:sqlite-jdbc-3.45.0.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar LibraryManagementSystem

   # On Windows
   java -cp .;sqlite-jdbc-3.45.0.0.jar;slf4j-api-2.0.9.jar;slf4j-simple-2.0.9.jar LibraryManagementSystem
   ```

## Using the Library Management System

### Student Login

1. Enter a valid student UID (e.g., 23BCS12345)
2. Valid UIDs follow the format: YYDEPTnnnnn (YY=year, DEPT=department code, nnnnn=5 digit number)
3. Example valid UIDs: 23BCS12345, 22BCE10001, 21BBA10002

### Admin Access

The default admin password is "admin123"

### Earning Achievements (Gamification)

Perform these actions to earn achievements and points:
1. Borrow books - Use the "Borrow Book" button
2. Return books on time - Use the "Return Book" button
3. Rate books - Use the "Rate Book" button

To view your achievements:
1. Log in as a student
2. Click the "My Achievements" button in the Gamification section

To check your ranking:
1. Log in as a student
2. Click the "Leaderboard" button in the Gamification section 