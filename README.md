# 📚 Library Management System

**Computer Science Lab Project - Third Semester**  
**Chandigarh University**  
**Author:** Deepak Pandey

---

## 🎓 Academic Project Overview

This comprehensive Library Management System was developed as part of the **Java Programming Lab** coursework during the **Third Semester** at **Chandigarh University**. The project demonstrates advanced Java programming concepts, GUI development with Swing, database integration, and software engineering principles.

## 🚀 Project Versions

This repository contains **two implementations** of the same Library Management System:

### 1. 🖥️ **Original Java Desktop Application** (Main Project)
- **Technology**: Java Swing + SQLite
- **Type**: Desktop GUI Application
- **Purpose**: Academic lab assignment
- **Features**: Complete library management with advanced GUI
- **Download**: [LibraryManagementSystem-Standalone.jar](https://github.com/848deepak/library-management-system/raw/main/LibraryManagementSystem-Standalone.jar)

### 2. 🌐 **Modern Web Application** (Bonus Implementation)
- **Technology**: React.js + Node.js + Express + SQLite
- **Type**: Full-stack Web Application
- **Purpose**: Showcase and deployment
- **Features**: Same functionality with modern web interface
- **Live Demo**: [https://library-management-system-git-main-deepaks-projects-65bf3b81.vercel.app](https://library-management-system-git-main-deepaks-projects-65bf3b81.vercel.app)

---

## 🚀 Quick Access

### 📱 **Try the Web App (Live Demo)**
👉 **[Open Library Management System](https://library-management-system-git-main-deepaks-projects-65bf3b81.vercel.app)**

**Demo Credentials:**
- **Student Login**: `student1` / `password`
- **Admin Login**: `admin` / `admin123`

### 💻 **Download Desktop App (JAR)**
👉 **[Download LibraryManagementSystem-Standalone.jar](https://github.com/848deepak/library-management-system/raw/main/LibraryManagementSystem-Standalone.jar)**
- Requires Java 8+
- Cross-platform (Windows, Mac, Linux)
- 13MB standalone executable

---

## ✨ Features (Both Versions)

### 📖 **Core Library Management**
- **Book Management**: Add, remove, search, and categorize books
- **Student Authentication**: UID-based login system
- **Borrowing System**: Issue and return books with due dates
- **Search & Filter**: Advanced search by title, author, category, ISBN
- **Real-time Statistics**: Live library analytics

### 🎮 **Gamification System**
- **Points System**: Earn points for borrowing and returning books
- **Student Leaderboard**: Competitive ranking system
- **Achievement Tracking**: Monitor student activities

### 👨‍💼 **Admin Panel**
- **Complete Book Management**: CRUD operations
- **Student Monitoring**: Track borrowing patterns
- **Library Analytics**: Comprehensive statistics
- **Database Management**: Automated data handling

### 🔐 **Security Features**
- **Student Authentication**: Validated UID format (YYDEPTnnnnn)
- **Admin Authentication**: Password-protected admin access
- **Data Validation**: Input sanitization and error handling

---

## 🖥️ Java Desktop Application (Original)

### **System Requirements**
- Java JDK 11 or higher
- SQLite JDBC Driver (included)
- SLF4J Libraries (included)

### **How to Run**
```bash
# On macOS/Linux
./run.sh

# On Windows
run.bat

# Manual execution
javac -cp .:sqlite-jdbc-3.45.0.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar LibraryManagementSystem.java
java -cp .:sqlite-jdbc-3.45.0.0.jar:slf4j-api-2.0.9.jar:slf4j-simple-2.0.9.jar LibraryManagementSystem
```

### **Login Credentials**
- **Student**: Any valid UID (e.g., 23BCS12345, 22BCE10001, 21BBA10002)
- **Admin**: Password `admin123`

### **Key Java Concepts Demonstrated**
- **Object-Oriented Programming**: Classes, inheritance, encapsulation
- **GUI Development**: Java Swing components and event handling
- **Database Integration**: SQLite with JDBC
- **Exception Handling**: Robust error management
- **File I/O Operations**: Database file management
- **Design Patterns**: MVC architecture implementation

---

## 🌐 Web Application (Bonus)

### **Technology Stack**
- **Frontend**: React.js with modern CSS
- **Backend**: Node.js with Express.js
- **Database**: SQLite
- **API**: RESTful API design

### **How to Run**
```bash
cd web-version
npm install
cd client && npm install && npm run build && cd ..
npm start
```

### **Live Demo**
Access at: `http://localhost:3000`

### **Deployment Ready**
- ✅ Heroku compatible
- ✅ Vercel ready
- ✅ Railway deployable
- ✅ Docker containerizable

---

## 📊 Technical Achievements

### **Database Design**
- **Normalized Schema**: Efficient relational database structure
- **Sample Data**: 70+ books across 12 categories
- **Data Integrity**: Foreign key constraints and validation

### **User Interface**
- **Intuitive Design**: User-friendly interface for both students and admins
- **Responsive Layout**: Adapts to different screen sizes (web version)
- **Professional Styling**: Modern, clean aesthetic

### **Software Engineering**
- **Modular Architecture**: Well-organized, maintainable code
- **Error Handling**: Comprehensive exception management
- **Documentation**: Detailed code comments and README

---

## 📚 Academic Learning Outcomes

### **Programming Skills Developed**
- Advanced Java programming and OOP concepts
- GUI development with Java Swing
- Database programming with JDBC
- Event-driven programming
- Exception handling and debugging

### **Software Engineering Concepts**
- Requirements analysis and system design
- User interface design principles
- Database design and normalization
- Testing and quality assurance
- Documentation and version control

### **Problem-Solving Skills**
- Complex system architecture design
- Multi-user system considerations
- Data validation and security
- Performance optimization
- User experience design

---

## 🗂️ Project Structure

```
library-management-system/
├── 📁 Java Desktop Application (Original)
│   ├── LibraryManagementSystem.java    # Main application class
│   ├── Library.java                    # Core library logic
│   ├── Book.java                       # Book entity class
│   ├── StudentAuth.java                # Authentication system
│   ├── GamificationSystem.java         # Points and achievements
│   ├── DatabaseManager.java            # Database operations
│   ├── library.db                      # SQLite database
│   ├── run.sh / run.bat                # Run scripts
│   └── *.jar                          # Required libraries
│
├── 📁 web-version/ (Bonus Implementation)
│   ├── server.js                       # Express.js backend
│   ├── client/                         # React frontend
│   ├── package.json                    # Dependencies
│   └── README.md                       # Web version docs
│
└── 📄 README.md                        # This file
```

---

## 🎯 Demo Instructions

### **For Academic Evaluation:**

1. **Launch Application**: Run the Java desktop version
2. **Student Demo**: Login with UID `23BCS12345`
   - Browse library catalog
   - Search for books
   - Borrow and return books
   - Check leaderboard
3. **Admin Demo**: Login with password `admin123`
   - Add new books
   - View statistics
   - Monitor student activity

### **For Online Showcase:**
- Web version deployed at: [Your-Deployment-URL]
- Same functionality in modern web interface
- Mobile-friendly for presentations

---

## 🏆 Project Highlights

- ✅ **Complete Implementation**: All required features functional
- ✅ **Professional Quality**: Production-ready code
- ✅ **Excellent Documentation**: Comprehensive README and comments
- ✅ **Modern UI/UX**: Intuitive and attractive interface
- ✅ **Scalable Design**: Easy to extend and modify
- ✅ **Real-world Application**: Practical library management solution

---

## 📞 Contact Information

**Student**: Deepak Pandey  
**University**: Chandigarh University  
**Course**: Computer Science Engineering  
**Semester**: Third Semester  
**Subject**: Java Programming Lab  

---

## 📜 License

This project is developed for academic purposes as part of the Computer Science curriculum at Chandigarh University.

---

**Note**: This project demonstrates comprehensive understanding of Java programming, software engineering principles, and modern web development technologies. Both implementations showcase the same core functionality with different technological approaches, highlighting versatility in software development.

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