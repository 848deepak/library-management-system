import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates analytics and reports for library usage
 */
public class ReportingSystem {
    private Connection connection;
    
    /**
     * Creates a new ReportingSystem with database connection
     */
    public ReportingSystem(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Gets total book count by category
     * @return Map of category to count
     */
    public Map<String, Integer> getBookCountByCategory() {
        Map<String, Integer> categoryMap = new TreeMap<>();
        
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT category, COUNT(*) as count " +
                "FROM books " +
                "GROUP BY category " +
                "ORDER BY count DESC"
            );
            
            while (rs.next()) {
                String category = rs.getString("category");
                int count = rs.getInt("count");
                categoryMap.put(category, count);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting book count by category: " + e.getMessage());
            e.printStackTrace();
        }
        
        return categoryMap;
    }
    
    /**
     * Gets books borrowed by month over a specified period
     * @param startDate Start date for the report
     * @param endDate End date for the report
     * @return Map of month to borrow count
     */
    public Map<String, Integer> getMonthlyBorrowingActivity(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> monthlyActivity = new LinkedHashMap<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT strftime('%Y-%m', borrow_date) as month, COUNT(*) as count " +
                "FROM borrowing_history " +
                "WHERE borrow_date BETWEEN ? AND ? " +
                "GROUP BY month " +
                "ORDER BY month"
            );
            
            stmt.setString(1, startDate.toString());
            stmt.setString(2, endDate.toString());
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String month = rs.getString("month");
                int count = rs.getInt("count");
                monthlyActivity.put(month, count);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting monthly borrowing activity: " + e.getMessage());
            e.printStackTrace();
        }
        
        return monthlyActivity;
    }
    
    /**
     * Gets top borrowed books within a date range
     * @param startDate Start date
     * @param endDate End date
     * @param limit Maximum number of books to return
     * @return List of book borrow statistics
     */
    public List<BookBorrowStat> getTopBorrowedBooks(LocalDate startDate, LocalDate endDate, int limit) {
        List<BookBorrowStat> topBooks = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT b.isbn, b.title, b.author, COUNT(*) as borrow_count " +
                "FROM borrowing_history h " +
                "JOIN books b ON h.book_id = b.id " +
                "WHERE h.borrow_date BETWEEN ? AND ? " +
                "GROUP BY b.id " +
                "ORDER BY borrow_count DESC " +
                "LIMIT ?"
            );
            
            stmt.setString(1, startDate.toString());
            stmt.setString(2, endDate.toString());
            stmt.setInt(3, limit);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                BookBorrowStat stat = new BookBorrowStat(
                    rs.getString("isbn"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getInt("borrow_count")
                );
                topBooks.add(stat);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting top borrowed books: " + e.getMessage());
            e.printStackTrace();
        }
        
        return topBooks;
    }
    
    /**
     * Gets borrowing statistics by department
     * @param startDate Start date
     * @param endDate End date
     * @return Map of department name to borrow count
     */
    public Map<String, Integer> getBorrowingByDepartment(LocalDate startDate, LocalDate endDate) {
        Map<String, Integer> departmentStats = new LinkedHashMap<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT SUBSTR(s.uid, 3, 3) as dept_code, COUNT(*) as count " +
                "FROM borrowing_history h " +
                "JOIN students s ON h.student_id = s.id " +
                "WHERE h.borrow_date BETWEEN ? AND ? " +
                "GROUP BY dept_code " +
                "ORDER BY count DESC"
            );
            
            stmt.setString(1, startDate.toString());
            stmt.setString(2, endDate.toString());
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String deptCode = rs.getString("dept_code");
                int count = rs.getInt("count");
                // Convert department code to name
                String deptName = StudentAuth.getDepartmentName(deptCode);
                departmentStats.put(deptName, count);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting department statistics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return departmentStats;
    }
    
    /**
     * Gets overdue books statistics
     * @return Map with overdue statistics
     */
    public Map<String, Object> getOverdueStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Statement stmt = connection.createStatement();
            
            // Count currently overdue books
            ResultSet rs1 = stmt.executeQuery(
                "SELECT COUNT(*) as count FROM books " +
                "WHERE is_available = 0 AND due_date < DATE('now')"
            );
            
            if (rs1.next()) {
                stats.put("currentOverdueCount", rs1.getInt("count"));
            }
            rs1.close();
            
            // Count total fines
            ResultSet rs2 = stmt.executeQuery(
                "SELECT SUM(fine_amount) as total FROM fines WHERE is_paid = 0"
            );
            
            if (rs2.next()) {
                stats.put("unpaidFinesTotal", rs2.getDouble("total"));
            }
            rs2.close();
            
            // Count overdue books by days overdue
            ResultSet rs3 = stmt.executeQuery(
                "SELECT " +
                "COUNT(CASE WHEN julianday('now') - julianday(due_date) BETWEEN 1 AND 7 THEN 1 END) as week1, " +
                "COUNT(CASE WHEN julianday('now') - julianday(due_date) BETWEEN 8 AND 14 THEN 1 END) as week2, " +
                "COUNT(CASE WHEN julianday('now') - julianday(due_date) BETWEEN 15 AND 30 THEN 1 END) as month1, " +
                "COUNT(CASE WHEN julianday('now') - julianday(due_date) > 30 THEN 1 END) as older " +
                "FROM books WHERE is_available = 0 AND due_date < DATE('now')"
            );
            
            if (rs3.next()) {
                Map<String, Integer> overdueBreakdown = new HashMap<>();
                overdueBreakdown.put("1-7 days", rs3.getInt("week1"));
                overdueBreakdown.put("8-14 days", rs3.getInt("week2"));
                overdueBreakdown.put("15-30 days", rs3.getInt("month1"));
                overdueBreakdown.put("30+ days", rs3.getInt("older"));
                stats.put("overdueBreakdown", overdueBreakdown);
            }
            rs3.close();
            
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting overdue statistics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * Gets student activity statistics
     * @param limit Number of top students to return
     * @return Map with student activity statistics
     */
    public Map<String, Object> getStudentActivityStatistics(int limit) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get most active students
            PreparedStatement stmt1 = connection.prepareStatement(
                "SELECT s.id, s.uid, s.first_name || ' ' || s.last_name as name, " +
                "COUNT(*) as borrow_count " +
                "FROM borrowing_history h " +
                "JOIN students s ON h.student_id = s.id " +
                "GROUP BY s.id " +
                "ORDER BY borrow_count DESC " +
                "LIMIT ?"
            );
            
            stmt1.setInt(1, limit);
            ResultSet rs1 = stmt1.executeQuery();
            
            List<Map<String, Object>> activeStudents = new ArrayList<>();
            while (rs1.next()) {
                Map<String, Object> student = new HashMap<>();
                student.put("id", rs1.getInt("id"));
                student.put("uid", rs1.getString("uid"));
                student.put("name", rs1.getString("name"));
                student.put("borrowCount", rs1.getInt("borrow_count"));
                activeStudents.add(student);
            }
            stats.put("mostActiveStudents", activeStudents);
            rs1.close();
            stmt1.close();
            
            // Get inactive students (no borrows in last 3 months)
            PreparedStatement stmt2 = connection.prepareStatement(
                "SELECT COUNT(*) as count FROM students " +
                "WHERE id NOT IN (SELECT DISTINCT student_id FROM borrowing_history " +
                "WHERE borrow_date > date('now', '-3 months'))"
            );
            
            ResultSet rs2 = stmt2.executeQuery();
            if (rs2.next()) {
                stats.put("inactiveStudentsCount", rs2.getInt("count"));
            }
            rs2.close();
            stmt2.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting student activity statistics: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * Gets popular categories by year
     * @param year The year to analyze
     * @return Map of category to borrow count
     */
    public Map<String, Integer> getPopularCategoriesByYear(int year) {
        Map<String, Integer> categoryStats = new LinkedHashMap<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT b.category, COUNT(*) as count " +
                "FROM borrowing_history h " +
                "JOIN books b ON h.book_id = b.id " +
                "WHERE strftime('%Y', h.borrow_date) = ? " +
                "GROUP BY b.category " +
                "ORDER BY count DESC"
            );
            
            stmt.setString(1, String.valueOf(year));
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String category = rs.getString("category");
                int count = rs.getInt("count");
                categoryStats.put(category, count);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting popular categories: " + e.getMessage());
            e.printStackTrace();
        }
        
        return categoryStats;
    }
    
    /**
     * Gets usage trends over time
     * @param periodMonths Number of months to analyze
     * @return Map with usage trend statistics
     */
    public Map<String, Object> getUsageTrends(int periodMonths) {
        Map<String, Object> trends = new HashMap<>();
        
        try {
            // Set up date ranges
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusMonths(periodMonths);
            
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT " +
                "strftime('%Y-%m', borrow_date) as month, " +
                "COUNT(*) as borrow_count, " +
                "COUNT(DISTINCT student_id) as unique_students " +
                "FROM borrowing_history " +
                "WHERE borrow_date BETWEEN ? AND ? " +
                "GROUP BY month " +
                "ORDER BY month"
            );
            
            stmt.setString(1, startDate.toString());
            stmt.setString(2, endDate.toString());
            
            ResultSet rs = stmt.executeQuery();
            
            Map<String, Integer> borrowsByMonth = new LinkedHashMap<>();
            Map<String, Integer> studentsByMonth = new LinkedHashMap<>();
            
            while (rs.next()) {
                String month = rs.getString("month");
                int borrowCount = rs.getInt("borrow_count");
                int uniqueStudents = rs.getInt("unique_students");
                
                borrowsByMonth.put(month, borrowCount);
                studentsByMonth.put(month, uniqueStudents);
            }
            
            trends.put("borrowsByMonth", borrowsByMonth);
            trends.put("uniqueStudentsByMonth", studentsByMonth);
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting usage trends: " + e.getMessage());
            e.printStackTrace();
        }
        
        return trends;
    }
    
    /**
     * Generate a full library usage report
     * @param startDate Start date
     * @param endDate End date
     * @return Complete report data
     */
    public Map<String, Object> generateFullReport(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> report = new HashMap<>();
        
        // Basic summary
        report.put("reportPeriod", Map.of(
            "startDate", startDate.format(DateTimeFormatter.ISO_DATE),
            "endDate", endDate.format(DateTimeFormatter.ISO_DATE)
        ));
        
        try {
            Statement stmt = connection.createStatement();
            
            // Total book count
            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) as count FROM books");
            if (rs1.next()) {
                report.put("totalBooks", rs1.getInt("count"));
            }
            rs1.close();
            
            // Available books
            ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) as count FROM books WHERE is_available = 1");
            if (rs2.next()) {
                report.put("availableBooks", rs2.getInt("count"));
            }
            rs2.close();
            
            // Total students
            ResultSet rs3 = stmt.executeQuery("SELECT COUNT(*) as count FROM students");
            if (rs3.next()) {
                report.put("totalStudents", rs3.getInt("count"));
            }
            rs3.close();
            
            // Total borrowings in period
            PreparedStatement pstmt1 = connection.prepareStatement(
                "SELECT COUNT(*) as count FROM borrowing_history " +
                "WHERE borrow_date BETWEEN ? AND ?"
            );
            pstmt1.setString(1, startDate.toString());
            pstmt1.setString(2, endDate.toString());
            ResultSet rs4 = pstmt1.executeQuery();
            if (rs4.next()) {
                report.put("totalBorrowings", rs4.getInt("count"));
            }
            rs4.close();
            pstmt1.close();
            
            // Active students in period
            PreparedStatement pstmt2 = connection.prepareStatement(
                "SELECT COUNT(DISTINCT student_id) as count FROM borrowing_history " +
                "WHERE borrow_date BETWEEN ? AND ?"
            );
            pstmt2.setString(1, startDate.toString());
            pstmt2.setString(2, endDate.toString());
            ResultSet rs5 = pstmt2.executeQuery();
            if (rs5.next()) {
                report.put("activeStudents", rs5.getInt("count"));
            }
            rs5.close();
            pstmt2.close();
            
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Add detailed statistics
        report.put("booksByCategory", getBookCountByCategory());
        report.put("topBooks", getTopBorrowedBooks(startDate, endDate, 10));
        report.put("borrowingByDepartment", getBorrowingByDepartment(startDate, endDate));
        report.put("overdueStats", getOverdueStatistics());
        report.put("studentActivity", getStudentActivityStatistics(10));
        report.put("monthlyActivity", getMonthlyBorrowingActivity(startDate, endDate));
        
        return report;
    }
    
    /**
     * Represents book borrowing statistics
     */
    public static class BookBorrowStat {
        private String isbn;
        private String title;
        private String author;
        private int borrowCount;
        
        public BookBorrowStat(String isbn, String title, String author, int borrowCount) {
            this.isbn = isbn;
            this.title = title;
            this.author = author;
            this.borrowCount = borrowCount;
        }
        
        // Getters
        public String getIsbn() { return isbn; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public int getBorrowCount() { return borrowCount; }
        
        @Override
        public String toString() {
            return title + " by " + author + " (ISBN: " + isbn + ") - Borrowed " + borrowCount + " times";
        }
    }
} 