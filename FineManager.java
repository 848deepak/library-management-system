import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages overdue fines for books in the library system
 */
public class FineManager {
    private Connection connection;
    private double dailyFineRate = 0.50; // Default fine rate: 50 cents per day
    private int gracePeriodDays = 3;     // Grace period before fines start
    private double maximumFine = 20.0;   // Maximum fine per book
    
    /**
     * Creates a new FineManager with database connection
     */
    public FineManager(Connection connection) {
        this.connection = connection;
        createFinesTable();
    }
    
    /**
     * Creates the fines table if it doesn't exist
     */
    private void createFinesTable() {
        try {
            Statement stmt = connection.createStatement();
            
            // Create fines table to track all fines
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS fines (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "loan_id INTEGER NOT NULL, " +
                "student_id INTEGER NOT NULL, " +
                "book_id INTEGER NOT NULL, " +
                "fine_amount REAL NOT NULL, " +
                "days_overdue INTEGER NOT NULL, " +
                "is_paid BOOLEAN DEFAULT 0, " +
                "payment_date TEXT, " +
                "calculated_date TEXT DEFAULT CURRENT_TIMESTAMP, " +
                "notes TEXT, " +
                "FOREIGN KEY (loan_id) REFERENCES borrowing_history(id), " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "FOREIGN KEY (book_id) REFERENCES books(id)" +
                ")"
            );
            
            // Create view for unpaid fines
            stmt.execute(
                "CREATE VIEW IF NOT EXISTS unpaid_fines AS " +
                "SELECT f.id as fine_id, f.fine_amount, f.days_overdue, " +
                "s.id as student_id, s.uid, s.first_name, s.last_name, " +
                "b.id as book_id, b.title, b.isbn, " +
                "h.due_date, h.id as loan_id " +
                "FROM fines f " +
                "JOIN borrowing_history h ON f.loan_id = h.id " +
                "JOIN books b ON f.book_id = b.id " +
                "JOIN students s ON f.student_id = s.id " +
                "WHERE f.is_paid = 0"
            );
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error creating fines table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sets the daily fine rate
     * @param rate Daily fine rate in currency units
     */
    public void setDailyFineRate(double rate) {
        if (rate >= 0) {
            this.dailyFineRate = rate;
        }
    }
    
    /**
     * Sets the grace period in days
     * @param days Number of days in grace period
     */
    public void setGracePeriodDays(int days) {
        if (days >= 0) {
            this.gracePeriodDays = days;
        }
    }
    
    /**
     * Sets the maximum fine per book
     * @param maxFine Maximum fine amount
     */
    public void setMaximumFine(double maxFine) {
        if (maxFine >= 0) {
            this.maximumFine = maxFine;
        }
    }
    
    /**
     * Calculates and records fines for all overdue books
     * @return Number of new fines recorded
     */
    public int calculateAllFines() {
        int newFinesCount = 0;
        
        try {
            // Find all overdue loans that don't have an associated fine yet
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT h.id as loan_id, h.student_id, h.book_id, h.due_date " +
                "FROM borrowing_history h " +
                "LEFT JOIN fines f ON h.id = f.loan_id " +
                "WHERE h.is_returned = 0 " +
                "AND h.due_date < ? " +
                "AND f.id IS NULL"
            );
            
            pstmt.setString(1, LocalDate.now().toString());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                int loanId = rs.getInt("loan_id");
                int studentId = rs.getInt("student_id");
                int bookId = rs.getInt("book_id");
                LocalDate dueDate = LocalDate.parse(rs.getString("due_date"));
                
                // Calculate days overdue and fine amount
                long daysOverdue = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
                
                // Apply grace period
                daysOverdue = Math.max(0, daysOverdue - gracePeriodDays);
                
                if (daysOverdue > 0) {
                    double fineAmount = daysOverdue * dailyFineRate;
                    
                    // Cap at maximum fine amount
                    fineAmount = Math.min(fineAmount, maximumFine);
                    
                    // Record the fine
                    if (recordFine(loanId, studentId, bookId, fineAmount, daysOverdue)) {
                        newFinesCount++;
                    }
                }
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error calculating fines: " + e.getMessage());
            e.printStackTrace();
        }
        
        return newFinesCount;
    }
    
    /**
     * Records a fine in the database
     */
    private boolean recordFine(int loanId, int studentId, int bookId, double fineAmount, long daysOverdue) {
        try {
            PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO fines (loan_id, student_id, book_id, fine_amount, days_overdue) " +
                "VALUES (?, ?, ?, ?, ?)"
            );
            
            pstmt.setInt(1, loanId);
            pstmt.setInt(2, studentId);
            pstmt.setInt(3, bookId);
            pstmt.setDouble(4, fineAmount);
            pstmt.setLong(5, daysOverdue);
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error recording fine: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Pays a specific fine
     * @param fineId The ID of the fine to pay
     * @param notes Optional payment notes
     * @return true if payment was successful
     */
    public boolean payFine(int fineId, String notes) {
        try {
            PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE fines SET is_paid = 1, payment_date = ?, notes = ? WHERE id = ?"
            );
            
            pstmt.setString(1, LocalDate.now().toString());
            pstmt.setString(2, notes);
            pstmt.setInt(3, fineId);
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            return result > 0;
        } catch (SQLException e) {
            System.err.println("Error paying fine: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Pays all fines for a specific student
     * @param studentId The student ID
     * @param notes Optional payment notes
     * @return Number of fines paid
     */
    public int payAllFines(int studentId, String notes) {
        try {
            PreparedStatement pstmt = connection.prepareStatement(
                "UPDATE fines SET is_paid = 1, payment_date = ?, notes = ? " +
                "WHERE student_id = ? AND is_paid = 0"
            );
            
            pstmt.setString(1, LocalDate.now().toString());
            pstmt.setString(2, notes);
            pstmt.setInt(3, studentId);
            
            int result = pstmt.executeUpdate();
            pstmt.close();
            
            return result;
        } catch (SQLException e) {
            System.err.println("Error paying all fines: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Gets all unpaid fines for a student
     * @param studentId The student ID
     * @return List of unpaid fines
     */
    public List<Fine> getUnpaidFinesForStudent(int studentId) {
        List<Fine> fines = new ArrayList<>();
        
        try {
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM unpaid_fines WHERE student_id = ?"
            );
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Fine fine = new Fine(
                    rs.getInt("fine_id"),
                    rs.getInt("loan_id"),
                    rs.getInt("student_id"),
                    rs.getString("uid"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("isbn"),
                    rs.getDouble("fine_amount"),
                    rs.getInt("days_overdue"),
                    LocalDate.parse(rs.getString("due_date"))
                );
                fines.add(fine);
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting unpaid fines: " + e.getMessage());
            e.printStackTrace();
        }
        
        return fines;
    }
    
    /**
     * Gets total unpaid fines for a student
     * @param studentId The student ID
     * @return Total amount of unpaid fines
     */
    public double getTotalUnpaidFines(int studentId) {
        double total = 0.0;
        
        try {
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT SUM(fine_amount) AS total FROM fines " +
                "WHERE student_id = ? AND is_paid = 0"
            );
            
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                total = rs.getDouble("total");
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting total fines: " + e.getMessage());
            e.printStackTrace();
        }
        
        return total;
    }
    
    /**
     * Gets all unpaid fines in the system
     * @return List of all unpaid fines
     */
    public List<Fine> getAllUnpaidFines() {
        List<Fine> fines = new ArrayList<>();
        
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM unpaid_fines");
            
            while (rs.next()) {
                Fine fine = new Fine(
                    rs.getInt("fine_id"),
                    rs.getInt("loan_id"),
                    rs.getInt("student_id"),
                    rs.getString("uid"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("isbn"),
                    rs.getDouble("fine_amount"),
                    rs.getInt("days_overdue"),
                    LocalDate.parse(rs.getString("due_date"))
                );
                fines.add(fine);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting all unpaid fines: " + e.getMessage());
            e.printStackTrace();
        }
        
        return fines;
    }
    
    /**
     * Gets total fines collected in a date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Total amount of fines collected
     */
    public double getTotalFinesCollected(LocalDate startDate, LocalDate endDate) {
        double total = 0.0;
        
        try {
            PreparedStatement pstmt = connection.prepareStatement(
                "SELECT SUM(fine_amount) AS total FROM fines " +
                "WHERE is_paid = 1 " +
                "AND payment_date BETWEEN ? AND ?"
            );
            
            pstmt.setString(1, startDate.toString());
            pstmt.setString(2, endDate.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                total = rs.getDouble("total");
            }
            
            rs.close();
            pstmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting total collected fines: " + e.getMessage());
            e.printStackTrace();
        }
        
        return total;
    }
    
    /**
     * Class representing a fine record
     */
    public static class Fine {
        private int id;
        private int loanId;
        private int studentId;
        private String studentUid;
        private String studentName;
        private int bookId;
        private String bookTitle;
        private String bookIsbn;
        private double amount;
        private int daysOverdue;
        private LocalDate dueDate;
        
        public Fine(int id, int loanId, int studentId, String studentUid, String studentName, 
                   int bookId, String bookTitle, String bookIsbn, double amount, 
                   int daysOverdue, LocalDate dueDate) {
            this.id = id;
            this.loanId = loanId;
            this.studentId = studentId;
            this.studentUid = studentUid;
            this.studentName = studentName;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.bookIsbn = bookIsbn;
            this.amount = amount;
            this.daysOverdue = daysOverdue;
            this.dueDate = dueDate;
        }
        
        // Getters
        public int getId() { return id; }
        public int getLoanId() { return loanId; }
        public int getStudentId() { return studentId; }
        public String getStudentUid() { return studentUid; }
        public String getStudentName() { return studentName; }
        public int getBookId() { return bookId; }
        public String getBookTitle() { return bookTitle; }
        public String getBookIsbn() { return bookIsbn; }
        public double getAmount() { return amount; }
        public int getDaysOverdue() { return daysOverdue; }
        public LocalDate getDueDate() { return dueDate; }
        
        @Override
        public String toString() {
            return String.format("Fine #%d: $%.2f for %s (UID: %s) - Book: '%s' (%d days overdue)",
                id, amount, studentName, studentUid, bookTitle, daysOverdue);
        }
    }
}