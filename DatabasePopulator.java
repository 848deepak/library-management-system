import java.sql.*;
import java.util.Random;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class to populate the database with a large collection of books and students
 */
public class DatabasePopulator {
    private static final Random random = new Random();
    
    // Lists of sample data for generating books
    private static final String[] FICTION_TITLES = {
        "The Silent Patient", "Where the Crawdads Sing", "The Midnight Library", 
        "Verity", "It Ends with Us", "The Four Winds", "The Last Thing He Told Me",
        "The Guest List", "The Vanishing Half", "The Invisible Life of Addie LaRue",
        "The Seven Husbands of Evelyn Hugo", "The Push", "People We Meet on Vacation",
        "The Paper Palace", "Malibu Rising", "The Maidens", "Apples Never Fall",
        "The Lincoln Highway", "Cloud Cuckoo Land", "The Overstory", "American Dirt",
        "Anxious People", "The Nightingale", "The Great Alone", "Eleanor Oliphant Is Completely Fine",
        "Little Fires Everywhere", "The Dutch House", "A Gentleman in Moscow", "Daisy Jones & The Six",
        "Normal People", "Such a Fun Age", "Olive, Again", "The Water Dancer", "Circe",
        "The Giver of Stars", "The Silent Patient", "A Little Life", "All the Light We Cannot See",
        "The Night Circus", "Red, White & Royal Blue", "The House in the Cerulean Sea",
        "The Song of Achilles", "The Thursday Murder Club", "Project Hail Mary", "Klara and the Sun"
    };
    
    private static final String[] NONFICTION_TITLES = {
        "Atomic Habits", "Educated", "Becoming", "Untamed", "Sapiens", 
        "Maybe You Should Talk to Someone", "Talking to Strangers", "Greenlights",
        "The Body Keeps the Score", "Between the World and Me", "Born a Crime",
        "Thinking, Fast and Slow", "Educated", "Bad Blood", "Hillbilly Elegy",
        "The Immortal Life of Henrietta Lacks", "When Breath Becomes Air", 
        "Killers of the Flower Moon", "Caste", "The Splendid and the Vile",
        "Just Mercy", "Empire of Pain", "The Code Breaker", "The Sum of Us",
        "What Happened to You?", "The Warmth of Other Suns", "Hood Feminism",
        "A Promised Land", "The Power of Habit", "Outliers", "Noise",
        "Quiet", "Braiding Sweetgrass", "The New Jim Crow", "How to Do Nothing",
        "Crying in H Mart", "Minor Feelings", "Breath", "Midnight in Chernobyl",
        "The Subtle Art of Not Giving a F*ck", "Range", "Thinking in Systems"
    };
    
    private static final String[] TECHNICAL_TITLES = {
        "Clean Code", "The Pragmatic Programmer", "Design Patterns", 
        "Refactoring", "Introduction to Algorithms", "Code Complete",
        "The Clean Coder", "Head First Design Patterns", "Cracking the Coding Interview",
        "You Don't Know JS", "Effective Java", "Python Crash Course",
        "Learning Python", "JavaScript: The Good Parts", "Eloquent JavaScript",
        "The C Programming Language", "Java Concurrency in Practice", "Deep Learning",
        "Machine Learning Yearning", "Artificial Intelligence: A Modern Approach",
        "Data Science for Business", "Hands-On Machine Learning", "Database System Concepts",
        "Fluent Python", "Building Microservices", "Domain-Driven Design", "Site Reliability Engineering",
        "The DevOps Handbook", "Release It!", "Working Effectively with Legacy Code"
    };
    
    private static final String[] AUTHORS = {
        "Jane Austen", "Stephen King", "J.K. Rowling", "Ernest Hemingway", 
        "F. Scott Fitzgerald", "Toni Morrison", "George Orwell", "Virginia Woolf",
        "James Baldwin", "Haruki Murakami", "Gabriel García Márquez", "Alice Walker",
        "Chimamanda Ngozi Adichie", "Neil Gaiman", "Margaret Atwood", "Khaled Hosseini",
        "Celeste Ng", "Jhumpa Lahiri", "Zadie Smith", "Colson Whitehead",
        "Anthony Doerr", "Donna Tartt", "Kazuo Ishiguro", "Isabel Allende",
        "Arundhati Roy", "Salman Rushdie", "Michael Lewis", "Malcolm Gladwell",
        "Elizabeth Gilbert", "Yuval Noah Harari", "Ta-Nehisi Coates", "Michelle Obama",
        "Rebecca Solnit", "David Sedaris", "Mary Oliver", "Ocean Vuong",
        "Don Miguel Ruiz", "Brené Brown", "Ibram X. Kendi", "Robert Greene",
        "Mark Manson", "James Clear", "Tara Westover", "Glennon Doyle",
        "Trevor Noah", "Robert Martin", "Martin Fowler", "Eric Evans"
    };
    
    private static final String[] CATEGORIES = {
        "Fiction", "Non-Fiction", "Mystery", "Thriller", "Science Fiction", 
        "Fantasy", "Romance", "Historical Fiction", "Biography", "Memoir",
        "Self-Help", "Business", "Science", "Philosophy", "Poetry",
        "Technical", "Computer Science", "Programming", "Data Science",
        "Personal Development", "Psychology", "History", "Economics"
    };
    
    private static final String[] STUDENT_FIRST_NAMES = {
        "Aiden", "Emma", "Liam", "Olivia", "Noah", "Ava", "William", "Sophia", "James", "Isabella",
        "Oliver", "Charlotte", "Benjamin", "Amelia", "Elijah", "Mia", "Lucas", "Harper", "Mason", "Evelyn",
        "Logan", "Abigail", "Alexander", "Emily", "Ethan", "Elizabeth", "Jacob", "Sofia", "Michael", "Avery",
        "Daniel", "Ella", "Henry", "Scarlett", "Jackson", "Grace", "Sebastian", "Chloe", "Amir", "Victoria",
        "Matthew", "Riley", "Samuel", "Aria", "David", "Lily", "Joseph", "Aubrey", "Carter", "Zoey",
        "Owen", "Hannah", "Wyatt", "Lillian", "John", "Addison", "Jack", "Layla", "Luke", "Natalie",
        "Jayden", "Alexa", "Dylan", "Preeti", "Grayson", "Samira", "Levi", "Fatima", "Isaac", "Wei",
        "Ryan", "Ming", "Nathan", "Jin", "Caleb", "Anika", "Muhammad", "Zara", "Luca", "Yuna"
    };
    
    private static final String[] STUDENT_LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
        "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
        "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores",
        "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts",
        "Patel", "Khan", "Chen", "Wang", "Li", "Zhang", "Singh", "Kumar", "Kim", "Park",
        "Ali", "Hassan", "Sharma", "Gupta", "Cohen", "Rossi", "Ferrari", "Muller", "Schmidt", "Weber"
    };
    
    private static final String[] DEPARTMENTS = {
        "Computer Science", "Electrical Engineering", "Mechanical Engineering", "Civil Engineering", 
        "Chemical Engineering", "Biology", "Chemistry", "Physics", "Mathematics", "Business",
        "Economics", "Psychology", "Sociology", "Political Science", "History", 
        "English Literature", "Philosophy", "Fine Arts", "Music", "Medicine"
    };
    
    // Department codes
    private static final String[] DEPT_CODES = {
        "BCS", "BEE", "BME", "BCE", "BCH", "BIO", "CHM", "PHY", "MTH", "BUS",
        "ECO", "PSY", "SOC", "POL", "HIS", "ENG", "PHL", "ART", "MUS", "MED"
    };
    
    /**
     * Populates the database with 150+ books and 50+ students
     */
    public static void populateDatabase() {
        // First clear the existing database
        try {
            clearDatabase();
            System.out.println("Database cleared. Starting population...");
            
            Connection conn = DriverManager.getConnection("jdbc:sqlite:library.db");
            
            // Create necessary tables if they don't exist
            Statement stmt = conn.createStatement();
            
            // Create books table with all necessary columns
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS books (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "isbn TEXT UNIQUE NOT NULL, " +
                "title TEXT NOT NULL, " +
                "author TEXT NOT NULL, " +
                "publication_year INTEGER, " +
                "category TEXT, " +
                "is_available BOOLEAN NOT NULL DEFAULT 1, " +
                "total_rating REAL DEFAULT 0, " +
                "rating_count INTEGER DEFAULT 0, " +
                "shelf_location TEXT, " +
                "date_added TEXT DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );
            
            // Create students table with full student information
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS students (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "uid TEXT UNIQUE NOT NULL, " +
                "first_name TEXT NOT NULL, " +
                "last_name TEXT NOT NULL, " +
                "email TEXT, " +
                "phone TEXT, " +
                "department_code TEXT, " +
                "department TEXT, " +
                "enrollment_year TEXT, " +
                "semester INTEGER, " +
                "active BOOLEAN DEFAULT 1, " +
                "date_registered TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "last_login TEXT" +
                ")"
            );
            
            // Create borrowing_history table to track all borrowing transactions
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS borrowing_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "book_id INTEGER NOT NULL, " +
                "student_id INTEGER NOT NULL, " +
                "borrow_date TEXT NOT NULL, " +
                "due_date TEXT NOT NULL, " +
                "return_date TEXT, " +
                "is_returned BOOLEAN DEFAULT 0, " +
                "FOREIGN KEY (book_id) REFERENCES books(id), " +
                "FOREIGN KEY (student_id) REFERENCES students(id)" +
                ")"
            );
            
            // Create active_loans view for easy querying of current loans
            stmt.execute(
                "CREATE VIEW IF NOT EXISTS active_loans AS " +
                "SELECT b.id as book_id, b.title, b.isbn, b.author, " +
                "s.id as student_id, s.uid, s.first_name, s.last_name, " +
                "h.borrow_date, h.due_date, h.id as loan_id " +
                "FROM borrowing_history h " +
                "JOIN books b ON h.book_id = b.id " +
                "JOIN students s ON h.student_id = s.id " +
                "WHERE h.is_returned = 0"
            );
            
            // Generate books data
            System.out.println("Adding fiction books...");
            for (int i = 0; i < 50; i++) {
                addBook(conn, FICTION_TITLES[i % FICTION_TITLES.length], getRandomAuthor(), 
                        getRandomYear(1900, 2023), "Fiction", generateISBN(), generateShelfLocation("F"));
            }
            
            System.out.println("Adding non-fiction books...");
            for (int i = 0; i < 50; i++) {
                addBook(conn, NONFICTION_TITLES[i % NONFICTION_TITLES.length], getRandomAuthor(), 
                        getRandomYear(1950, 2023), "Non-Fiction", generateISBN(), generateShelfLocation("NF"));
            }
            
            System.out.println("Adding technical books...");
            for (int i = 0; i < 30; i++) {
                addBook(conn, TECHNICAL_TITLES[i % TECHNICAL_TITLES.length], getRandomAuthor(), 
                        getRandomYear(1980, 2023), "Technical", generateISBN(), generateShelfLocation("T"));
            }
            
            System.out.println("Adding books with mixed categories...");
            for (int i = 0; i < 20; i++) {
                String category = getRandomCategory();
                String title = "Book " + (i + 1) + " of " + category;
                addBook(conn, title, getRandomAuthor(), 
                        getRandomYear(1990, 2023), category, generateISBN(), 
                        generateShelfLocation(category.substring(0, 1)));
            }
            
            // Generate student data
            System.out.println("Adding student records...");
            for (int i = 0; i < 50; i++) {
                int deptIndex = random.nextInt(DEPARTMENTS.length);
                String department = DEPARTMENTS[deptIndex];
                String deptCode = DEPT_CODES[deptIndex];
                int year = 20 + random.nextInt(5); // 20-24 (Years 2020-2024)
                
                addStudent(conn, 
                        generateUID(year, deptCode),
                        STUDENT_FIRST_NAMES[random.nextInt(STUDENT_FIRST_NAMES.length)],
                        STUDENT_LAST_NAMES[random.nextInt(STUDENT_LAST_NAMES.length)],
                        department, deptCode, "20" + year,
                        random.nextInt(8) + 1); // Semester 1-8
            }
            
            // Create some borrowing history
            System.out.println("Creating borrowing history...");
            createBorrowingHistory(conn);
            
            // Print summary
            printDatabaseSummary(conn);
            
            // Close connections
            stmt.close();
            conn.close();
            
        } catch (SQLException e) {
            System.err.println("Error populating database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates borrowing history for some students and books
     */
    private static void createBorrowingHistory(Connection conn) throws SQLException {
        // Get some random students
        List<Integer> studentIds = new ArrayList<>();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id FROM students ORDER BY RANDOM() LIMIT 20");
        while (rs.next()) {
            studentIds.add(rs.getInt("id"));
        }
        rs.close();
        
        // Get some random books
        List<Integer> bookIds = new ArrayList<>();
        rs = stmt.executeQuery("SELECT id FROM books ORDER BY RANDOM() LIMIT 40");
        while (rs.next()) {
            bookIds.add(rs.getInt("id"));
        }
        rs.close();
        
        // Create some returned loans (complete borrowing history)
        PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO borrowing_history (book_id, student_id, borrow_date, due_date, return_date, is_returned) " +
            "VALUES (?, ?, ?, ?, ?, 1)"
        );
        
        LocalDate now = LocalDate.now();
        
        for (int i = 0; i < 25; i++) {
            int bookId = bookIds.get(random.nextInt(bookIds.size()));
            int studentId = studentIds.get(random.nextInt(studentIds.size()));
            
            // Random dates in the past 90 days
            int borrowDaysAgo = random.nextInt(90) + 30; // 30-120 days ago
            LocalDate borrowDate = now.minusDays(borrowDaysAgo);
            LocalDate dueDate = borrowDate.plusDays(14); // 14 day loan period
            LocalDate returnDate = borrowDate.plusDays(random.nextInt(20)); // Return between 0-20 days after borrowing
            
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, studentId);
            pstmt.setString(3, borrowDate.toString());
            pstmt.setString(4, dueDate.toString());
            pstmt.setString(5, returnDate.toString());
            pstmt.executeUpdate();
        }
        
        // Create some active loans
        pstmt = conn.prepareStatement(
            "INSERT INTO borrowing_history (book_id, student_id, borrow_date, due_date, is_returned) " +
            "VALUES (?, ?, ?, ?, 0)"
        );
        
        // Also update the books to mark them as not available
        PreparedStatement updateBookStmt = conn.prepareStatement(
            "UPDATE books SET is_available = 0 WHERE id = ?"
        );
        
        for (int i = 25; i < 40; i++) {
            int bookId = bookIds.get(i);
            int studentId = studentIds.get(random.nextInt(studentIds.size()));
            
            // Recent borrow dates
            int borrowDaysAgo = random.nextInt(10); // 0-10 days ago
            LocalDate borrowDate = now.minusDays(borrowDaysAgo);
            LocalDate dueDate = borrowDate.plusDays(14); // 14 day loan period
            
            pstmt.setInt(1, bookId);
            pstmt.setInt(2, studentId);
            pstmt.setString(3, borrowDate.toString());
            pstmt.setString(4, dueDate.toString());
            pstmt.executeUpdate();
            
            // Update book availability
            updateBookStmt.setInt(1, bookId);
            updateBookStmt.executeUpdate();
        }
        
        pstmt.close();
        updateBookStmt.close();
        stmt.close();
    }
    
    /**
     * Prints a summary of the database contents
     */
    private static void printDatabaseSummary(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books");
        int bookCount = rs.next() ? rs.getInt(1) : 0;
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM students");
        int studentCount = rs.next() ? rs.getInt(1) : 0;
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM borrowing_history WHERE is_returned = 0");
        int activeLoansCount = rs.next() ? rs.getInt(1) : 0;
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM borrowing_history WHERE is_returned = 1");
        int returnedLoansCount = rs.next() ? rs.getInt(1) : 0;
        
        System.out.println("\nDatabase Population Summary:");
        System.out.println("---------------------------");
        System.out.println("Total Books: " + bookCount);
        System.out.println("Total Students: " + studentCount);
        System.out.println("Active Loans: " + activeLoansCount);
        System.out.println("Returned Loans: " + returnedLoansCount);
        System.out.println("---------------------------");
        
        rs.close();
        stmt.close();
    }
    
    /**
     * Clears existing database tables
     */
    private static void clearDatabase() throws SQLException {
        Connection conn = DriverManager.getConnection("jdbc:sqlite:library.db");
        Statement stmt = conn.createStatement();
        
        // Drop existing tables and views
        stmt.execute("DROP VIEW IF EXISTS active_loans");
        stmt.execute("DROP TABLE IF EXISTS borrowing_history");
        stmt.execute("DROP TABLE IF EXISTS books");
        stmt.execute("DROP TABLE IF EXISTS students");
        
        stmt.close();
        conn.close();
    }
    
    /**
     * Adds a book to the database
     */
    private static void addBook(Connection conn, String title, String author, 
                              int year, String category, String isbn, String shelfLocation) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO books (isbn, title, author, publication_year, category, is_available, shelf_location) " +
            "VALUES (?, ?, ?, ?, ?, 1, ?)"
        );
        
        pstmt.setString(1, isbn);
        pstmt.setString(2, title);
        pstmt.setString(3, author);
        pstmt.setInt(4, year);
        pstmt.setString(5, category);
        pstmt.setString(6, shelfLocation);
        
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * Adds a student to the database
     */
    private static void addStudent(Connection conn, String uid, String firstName, String lastName,
                                 String department, String deptCode, String enrollmentYear, int semester) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement(
            "INSERT INTO students (uid, first_name, last_name, email, phone, department_code, department, " +
            "enrollment_year, semester, active, last_login) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?)"
        );
        
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@university.edu";
        String phone = "555-" + (100 + random.nextInt(900)) + "-" + (1000 + random.nextInt(9000));
        
        pstmt.setString(1, uid);
        pstmt.setString(2, firstName);
        pstmt.setString(3, lastName);
        pstmt.setString(4, email);
        pstmt.setString(5, phone);
        pstmt.setString(6, deptCode);
        pstmt.setString(7, department);
        pstmt.setString(8, enrollmentYear);
        pstmt.setInt(9, semester);
        pstmt.setString(10, LocalDate.now().minusDays(random.nextInt(30)).toString());
        
        pstmt.executeUpdate();
        pstmt.close();
    }
    
    /**
     * Generates a random ISBN (13 digits)
     */
    private static String generateISBN() {
        StringBuilder isbn = new StringBuilder("978");
        for (int i = 0; i < 10; i++) {
            isbn.append(random.nextInt(10));
        }
        return isbn.toString();
    }
    
    /**
     * Generates a shelf location code
     */
    private static String generateShelfLocation(String prefix) {
        return prefix + "-" + (random.nextInt(10) + 1) + "-" + (random.nextInt(26) + 'A');
    }
    
    /**
     * Generates a student UID in format YYDEPTXXXXX
     */
    private static String generateUID(int year, String deptCode) {
        StringBuilder uid = new StringBuilder();
        uid.append(year);
        uid.append(deptCode);
        
        // Add 5 random digits
        for (int i = 0; i < 5; i++) {
            uid.append(random.nextInt(10));
        }
        
        return uid.toString();
    }
    
    /**
     * Returns a random author from the list
     */
    private static String getRandomAuthor() {
        return AUTHORS[random.nextInt(AUTHORS.length)];
    }
    
    /**
     * Returns a random category from the list
     */
    private static String getRandomCategory() {
        return CATEGORIES[random.nextInt(CATEGORIES.length)];
    }
    
    /**
     * Returns a random year between min and max (inclusive)
     */
    private static int getRandomYear(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }
    
    /**
     * Main method to run the populator as a standalone utility
     */
    public static void main(String[] args) {
        System.out.println("Starting database population...");
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Populate the database
            populateDatabase();
            
            System.out.println("Database population completed successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found. Please add the SQLite JDBC library to your project.");
            e.printStackTrace();
        }
    }
} 