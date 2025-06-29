import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

/**
 * DatabaseManager handles all database operations for the Library Management System
 */
public class DatabaseManager {
    private Connection connection;
    private static final String DB_NAME = "library.db";
    private GamificationSystem gamificationSystem;

    /**
     * Constructor initializes the database connection and creates tables if they don't exist
     */
    public DatabaseManager() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Connect to database (creates file if it doesn't exist)
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_NAME);
            
            // Create tables if they don't exist
            createTables();
            
            // Initialize gamification system
            this.gamificationSystem = new GamificationSystem(connection);
            
            System.out.println("Database connection established.");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found. Please add the SQLite JDBC library to your project.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Error connecting to the database.");
            e.printStackTrace();
        }
    }

    /**
     * Creates the necessary tables in the database if they don't exist
     */
    private void createTables() throws SQLException {
        Statement statement = connection.createStatement();
        
        // Drop tables if they're corrupted
        try {
            File dbFile = new File("library.db");
            if (dbFile.exists() && dbFile.length() < 100) {
                // Database file is suspiciously small - might be corrupt
                System.out.println("Database file is suspiciously small. Recreating tables.");
                statement.execute("DROP TABLE IF EXISTS books");
                statement.execute("DROP TABLE IF EXISTS students");
            }
        } catch (Exception e) {
            System.out.println("Error checking database file: " + e.getMessage());
        }
        
        // Create books table
        System.out.println("Creating books table if it doesn't exist...");
        statement.execute(
            "CREATE TABLE IF NOT EXISTS books (" +
            "isbn TEXT PRIMARY KEY, " +
            "title TEXT NOT NULL, " +
            "author TEXT NOT NULL, " +
            "publication_year INTEGER, " +
            "category TEXT, " +
            "is_available BOOLEAN NOT NULL DEFAULT 1, " +
            "borrower_name TEXT, " +
            "borrower_uid TEXT, " +
            "due_date TEXT, " +
            "total_rating REAL DEFAULT 0, " +
            "rating_count INTEGER DEFAULT 0" +
            ")"
        );
        
        // Check if books table exists and has required columns
        try {
            ResultSet rs = statement.executeQuery("PRAGMA table_info(books)");
            boolean hasBorrowerUid = false;
            while (rs.next()) {
                String columnName = rs.getString("name");
                if (columnName.equals("borrower_uid")) {
                    hasBorrowerUid = true;
                    break;
                }
            }
            rs.close();
            
            if (!hasBorrowerUid) {
                System.out.println("Adding borrower_uid column to books table...");
                statement.execute("ALTER TABLE books ADD COLUMN borrower_uid TEXT");
            }
        } catch (SQLException e) {
            System.out.println("Error checking books table columns: " + e.getMessage());
        }
        
        // Create students table
        statement.execute(
            "CREATE TABLE IF NOT EXISTS students (" +
            "uid TEXT PRIMARY KEY, " +
            "name TEXT NOT NULL, " +
            "department TEXT, " +
            "enrollment_year TEXT, " +
            "last_login TEXT" +
            ")"
        );
        
        statement.close();
        System.out.println("Database tables created successfully.");
    }
    
    /**
     * Checks if a book already exists in the database
     */
    public boolean bookExists(String isbn) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT isbn FROM books WHERE isbn = ?"
            );
            
            statement.setString(1, isbn);
            ResultSet rs = statement.executeQuery();
            
            boolean exists = rs.next();
            rs.close();
            statement.close();
            
            return exists;
        } catch (SQLException e) {
            System.err.println("Error checking if book exists in database.");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Adds a book to the database if it doesn't already exist
     */
    public boolean addBook(Book book) {
        try {
            // Check if book already exists
            if (bookExists(book.getIsbn())) {
                System.out.println("Book already exists in database: " + book.getTitle() + " (ISBN: " + book.getIsbn() + ")");
                return true;
            }
            
            System.out.println("Adding to database: " + book.getTitle() + " (ISBN: " + book.getIsbn() + ")");
            
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO books (isbn, title, author, publication_year, category, is_available) VALUES (?, ?, ?, ?, ?, ?)"
            );
            
            statement.setString(1, book.getIsbn());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getAuthor());
            statement.setInt(4, book.getPublicationYear());
            statement.setString(5, book.getCategory());
            statement.setBoolean(6, book.isAvailable());
            
            int rowsAffected = statement.executeUpdate();
            statement.close();
            
            if (rowsAffected > 0) {
                System.out.println("Successfully added to database: " + book.getTitle());
            } else {
                System.out.println("Failed to add book to database: " + book.getTitle());
            }
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error adding book to database: " + book.getTitle() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Removes a book from the database by ISBN
     */
    public boolean removeBook(String isbn) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM books WHERE isbn = ?"
            );
            
            statement.setString(1, isbn);
            
            int rowsAffected = statement.executeUpdate();
            statement.close();
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error removing book from database.");
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Updates a book's borrowing status with student UID
     */
    public boolean updateBookBorrowStatus(String isbn, boolean isAvailable, String borrowerName, String borrowerUID, LocalDate dueDate) {
        try {
            PreparedStatement statement;
            
            if (isAvailable) {
                // Returning a book
                statement = connection.prepareStatement(
                    "UPDATE books SET is_available = ?, borrower_name = NULL, borrower_uid = NULL, due_date = NULL WHERE isbn = ?"
                );
                
                statement.setBoolean(1, isAvailable);
                statement.setString(2, isbn);
                
                // Track returning activity for gamification
                if (borrowerUID != null) {
                    int studentId = getStudentIdFromUID(borrowerUID);
                    if (studentId > 0) {
                        LocalDate today = LocalDate.now();
                        // Check if book was returned on time for achievement
                        ResultSet rs = statement.executeQuery("SELECT due_date FROM books WHERE isbn = '" + isbn + "'");
                        if (rs.next()) {
                            String dueDateStr = rs.getString("due_date");
                            if (dueDateStr != null) {
                                LocalDate bookDueDate = LocalDate.parse(dueDateStr);
                                if (!today.isAfter(bookDueDate)) {
                                    // Book returned on time
                                    gamificationSystem.trackActivity(studentId, GamificationSystem.ACHIEVEMENT_ONTIME);
                                }
                            }
                        }
                        rs.close();
                        
                        // Track general return activity
                        gamificationSystem.trackActivity(studentId, GamificationSystem.ACHIEVEMENT_RETURN);
                    }
                }
            } else {
                // Borrowing a book
                statement = connection.prepareStatement(
                    "UPDATE books SET is_available = ?, borrower_name = ?, borrower_uid = ?, due_date = ? WHERE isbn = ?"
                );
                
                statement.setBoolean(1, isAvailable);
                statement.setString(2, borrowerName);
                statement.setString(3, borrowerUID);
                statement.setString(4, dueDate != null ? dueDate.toString() : null);
                statement.setString(5, isbn);
                
                // Track borrowing activity for gamification
                if (borrowerUID != null) {
                    int studentId = getStudentIdFromUID(borrowerUID);
                    if (studentId > 0) {
                        gamificationSystem.trackActivity(studentId, GamificationSystem.ACHIEVEMENT_BORROW);
                    }
                }
            }
            
            int rowsAffected = statement.executeUpdate();
            statement.close();
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating book borrow status: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets student ID from UID
     */
    private int getStudentIdFromUID(String uid) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT rowid as id FROM students WHERE uid = ?"
            );
            statement.setString(1, uid);
            ResultSet rs = statement.executeQuery();
            
            int id = -1;
            if (rs.next()) {
                id = rs.getInt("id");
            }
            
            rs.close();
            statement.close();
            
            return id;
        } catch (SQLException e) {
            System.err.println("Error getting student ID: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * Updates a book's rating
     */
    public boolean updateBookRating(String isbn, double newRating) {
        return updateBookRating(isbn, newRating, null);
    }
    
    /**
     * Updates a book's rating with student UID for gamification tracking
     * @param isbn Book ISBN
     * @param newRating Rating value
     * @param reviewerUID Student UID for gamification tracking
     * @return Success status
     */
    public boolean updateBookRating(String isbn, double newRating, String reviewerUID) {
        try {
            // First get current rating info
            PreparedStatement getStatement = connection.prepareStatement(
                "SELECT total_rating, rating_count FROM books WHERE isbn = ?"
            );
            getStatement.setString(1, isbn);
            ResultSet rs = getStatement.executeQuery();
            
            if (rs.next()) {
                double totalRating = rs.getDouble("total_rating");
                int ratingCount = rs.getInt("rating_count");
                
                // Calculate new values
                double newTotalRating = totalRating + newRating;
                int newRatingCount = ratingCount + 1;
                
                // Update the book
                PreparedStatement updateStatement = connection.prepareStatement(
                    "UPDATE books SET total_rating = ?, rating_count = ? WHERE isbn = ?"
                );
                
                updateStatement.setDouble(1, newTotalRating);
                updateStatement.setInt(2, newRatingCount);
                updateStatement.setString(3, isbn);
                
                int rowsAffected = updateStatement.executeUpdate();
                
                // Track review activity for gamification if reviewer UID is provided
                if (reviewerUID != null) {
                    int studentId = getStudentIdFromUID(reviewerUID);
                    if (studentId > 0) {
                        gamificationSystem.trackActivity(studentId, GamificationSystem.ACHIEVEMENT_REVIEW);
                    }
                }
                
                updateStatement.close();
                
                return rowsAffected > 0;
            }
            
            rs.close();
            getStatement.close();
            return false;
        } catch (SQLException e) {
            System.err.println("Error updating book rating.");
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets all books from the database
     */
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM books");
            
            while (rs.next()) {
                Book book = createBookFromResultSet(rs);
                books.add(book);
            }
            
            rs.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving books from database.");
            e.printStackTrace();
        }
        
        return books;
    }
    
    /**
     * Gets available books from the database
     */
    public List<Book> getAvailableBooks() {
        List<Book> books = new ArrayList<>();
        
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM books WHERE is_available = 1");
            
            while (rs.next()) {
                Book book = createBookFromResultSet(rs);
                books.add(book);
            }
            
            rs.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error retrieving available books from database.");
            e.printStackTrace();
        }
        
        return books;
    }
    
    /**
     * Searches for books by various criteria
     */
    public List<Book> searchBooks(String column, String searchTerm) {
        List<Book> books = new ArrayList<>();
        
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM books WHERE " + column + " LIKE ?"
            );
            
            statement.setString(1, "%" + searchTerm + "%");
            
            ResultSet rs = statement.executeQuery();
            
            while (rs.next()) {
                Book book = createBookFromResultSet(rs);
                books.add(book);
            }
            
            rs.close();
            statement.close();
        } catch (SQLException e) {
            System.err.println("Error searching books in database.");
            e.printStackTrace();
        }
        
        return books;
    }
    
    /**
     * Helper method to create a Book object from a database result set
     */
    private Book createBookFromResultSet(ResultSet rs) throws SQLException {
        int bookId = rs.getInt("id");
        String isbn = rs.getString("isbn");
        String title = rs.getString("title");
        String author = rs.getString("author");
        int year = rs.getInt("publication_year");
        String category = rs.getString("category");
        boolean isAvailable = rs.getBoolean("is_available");
        double totalRating = rs.getDouble("total_rating");
        int ratingCount = rs.getInt("rating_count");
        String shelfLocation = rs.getString("shelf_location");
        
        Book book = new Book(title, author, isbn, year, category);
        book.setAvailable(isAvailable);
        
        // If the book is not available, get borrower information from the borrowing_history table
        if (!isAvailable) {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT s.first_name, s.last_name, h.due_date " +
                "FROM borrowing_history h " +
                "JOIN students s ON h.student_id = s.id " +
                "WHERE h.book_id = ? AND h.is_returned = 0"
            );
            
            stmt.setInt(1, bookId);
            ResultSet borrowerRs = stmt.executeQuery();
            
            if (borrowerRs.next()) {
                String borrowerFirstName = borrowerRs.getString("first_name");
                String borrowerLastName = borrowerRs.getString("last_name");
                String borrowerName = borrowerFirstName + " " + borrowerLastName;
                String dueDateStr = borrowerRs.getString("due_date");
                
                book.setBorrowerName(borrowerName);
                
                // Set due date if it exists
                if (dueDateStr != null && !dueDateStr.isEmpty()) {
                    LocalDate dueDate = LocalDate.parse(dueDateStr);
                    book.setDueDate(dueDate);
                }
            }
            
            borrowerRs.close();
            stmt.close();
        }
        
        // Set rating if it exists
        if (ratingCount > 0) {
            double avgRating = totalRating / ratingCount;
            
            // We need to directly set the rating fields 
            for (int i = 0; i < ratingCount; i++) {
                book.addRating(avgRating);
            }
        }
        
        return book;
    }
    
    /**
     * Closes the database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection.");
            e.printStackTrace();
        }
    }

    /**
     * Registers or updates a student in the database
     */
    public boolean registerStudent(String uid, String fullName) {
        try {
            // Extract first and last name from full name
            String firstName = fullName;
            String lastName = "";
            
            if (fullName.contains(" ")) {
                String[] nameParts = fullName.split(" ", 2);
                firstName = nameParts[0];
                lastName = nameParts[1];
            }
            
            // Check if student already exists
            PreparedStatement checkStatement = connection.prepareStatement(
                "SELECT id FROM students WHERE uid = ?"
            );
            checkStatement.setString(1, uid);
            ResultSet rs = checkStatement.executeQuery();
            
            boolean exists = rs.next();
            rs.close();
            checkStatement.close();
            
            // Parse department code and enrollment year from UID if it's valid
            String deptCode = "";
            String enrollmentYear = "";
            
            if (StudentAuth.validateUID(uid)) {
                deptCode = StudentAuth.getDepartmentCode(uid);
                enrollmentYear = "20" + StudentAuth.getEnrollmentYear(uid);
            }
            
            PreparedStatement statement;
            if (exists) {
                // Update existing student
                statement = connection.prepareStatement(
                    "UPDATE students SET first_name = ?, last_name = ?, last_login = ? WHERE uid = ?"
                );
                statement.setString(1, firstName);
                statement.setString(2, lastName);
                statement.setString(3, LocalDate.now().toString());
                statement.setString(4, uid);
            } else {
                // Add new student
                statement = connection.prepareStatement(
                    "INSERT INTO students (uid, first_name, last_name, department_code, department, " +
                    "enrollment_year, email, active, last_login) VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)"
                );
                statement.setString(1, uid);
                statement.setString(2, firstName);
                statement.setString(3, lastName);
                statement.setString(4, deptCode);
                statement.setString(5, StudentAuth.getDepartmentName(deptCode));
                statement.setString(6, enrollmentYear);
                statement.setString(7, firstName.toLowerCase() + "." + lastName.toLowerCase() + "@university.edu");
                statement.setString(8, LocalDate.now().toString());
            }
            
            int rowsAffected = statement.executeUpdate();
            statement.close();
            
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error registering student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets student information from the database
     */
    public ResultSet getStudentInfo(String uid) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM students WHERE uid = ?"
            );
            statement.setString(1, uid);
            return statement.executeQuery();
        } catch (SQLException e) {
            System.err.println("Error retrieving student information.");
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets books borrowed by a specific student
     */
    public List<Book> getBooksBorrowedByStudent(String uid) {
        List<Book> books = new ArrayList<>();
        
        try {
            // Use the active_loans view to get all currently borrowed books
            PreparedStatement statement = connection.prepareStatement(
                "SELECT b.* FROM books b " +
                "JOIN borrowing_history h ON b.id = h.book_id " +
                "JOIN students s ON h.student_id = s.id " +
                "WHERE s.uid = ? AND h.is_returned = 0"
            );
            
            statement.setString(1, uid);
            ResultSet rs = statement.executeQuery();
            
            while (rs.next()) {
                Book book = createBookFromResultSet(rs);
                books.add(book);
            }
            
            rs.close();
            statement.close();
            
            if (books.isEmpty()) {
                System.out.println("No books found borrowed by student with UID: " + uid);
            } else {
                System.out.println("Found " + books.size() + " books borrowed by student with UID: " + uid);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving books borrowed by student: " + e.getMessage());
            e.printStackTrace();
        }
        
        return books;
    }

    /**
     * Gets the count of books in the database
     * @return The number of books in the database
     */
    public int getBookCount() {
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT COUNT(*) as count FROM books");
            
            int count = 0;
            if (rs.next()) {
                count = rs.getInt("count");
            }
            
            rs.close();
            statement.close();
            
            return count;
        } catch (SQLException e) {
            System.err.println("Error getting book count: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Gets the gamification system instance
     * @return The gamification system
     */
    public GamificationSystem getGamificationSystem() {
        return gamificationSystem;
    }
} 