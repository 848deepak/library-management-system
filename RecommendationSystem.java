import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RecommendationSystem {
    private Connection connection;
    private static final int MAX_RECOMMENDATIONS = 5;

    public RecommendationSystem(Connection connection) {
        this.connection = connection;
        setupRecommendationSystem();
    }

    private void setupRecommendationSystem() {
        try {
            // Create interest categories table if not exists
            String createInterestTable = "CREATE TABLE IF NOT EXISTS student_interests (" +
                    "uid VARCHAR(10) NOT NULL, " +
                    "category VARCHAR(50) NOT NULL, " +
                    "interest_level INT NOT NULL, " +
                    "PRIMARY KEY (uid, category))";
            
            try (PreparedStatement stmt = connection.prepareStatement(createInterestTable)) {
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error setting up recommendation system: " + e.getMessage());
        }
    }

    public List<Book> getRecommendedBooks(String studentId) {
        List<Book> recommendations = new ArrayList<>();
        
        try {
            // Collect recommendations from different strategies
            recommendations.addAll(getRecommendationsByInterest(studentId));
            recommendations.addAll(getRecommendationsByHistory(studentId));
            recommendations.addAll(getPopularBooks());
            
            // Remove duplicates and limit to MAX_RECOMMENDATIONS
            List<Book> uniqueRecommendations = removeDuplicates(recommendations);
            return uniqueRecommendations.subList(0, Math.min(uniqueRecommendations.size(), MAX_RECOMMENDATIONS));
            
        } catch (SQLException e) {
            System.err.println("Error generating recommendations: " + e.getMessage());
        }
        
        return recommendations;
    }
    
    private List<Book> getRecommendationsByInterest(String studentId) throws SQLException {
        List<Book> recommendations = new ArrayList<>();
        
        String query = "SELECT b.* FROM books b " +
                "JOIN student_interests si ON b.category = si.category " +
                "WHERE si.uid = ? " +
                "AND b.isbn NOT IN (SELECT isbn FROM borrowing_records WHERE student_id = ? AND return_date IS NULL) " +
                "ORDER BY si.interest_level DESC, b.rating DESC " +
                "LIMIT ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentId);
            stmt.setString(2, studentId);
            stmt.setInt(3, MAX_RECOMMENDATIONS);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recommendations.add(extractBookFromResultSet(rs));
                }
            }
        }
        
        return recommendations;
    }
    
    private List<Book> getRecommendationsByHistory(String studentId) throws SQLException {
        List<Book> recommendations = new ArrayList<>();
        
        // Find books similar to what the student has borrowed before
        String query = "SELECT b.* FROM books b " +
                "WHERE b.category IN (SELECT DISTINCT b2.category FROM books b2 " +
                "JOIN borrowing_records br ON b2.isbn = br.isbn " +
                "WHERE br.student_id = ?) " +
                "AND b.isbn NOT IN (SELECT isbn FROM borrowing_records WHERE student_id = ?) " +
                "AND b.status = 'Available' " +
                "ORDER BY b.rating DESC " +
                "LIMIT ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, studentId);
            stmt.setString(2, studentId);
            stmt.setInt(3, MAX_RECOMMENDATIONS);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recommendations.add(extractBookFromResultSet(rs));
                }
            }
        }
        
        return recommendations;
    }
    
    private List<Book> getPopularBooks() throws SQLException {
        List<Book> recommendations = new ArrayList<>();
        
        // Get most borrowed books that are currently available
        String query = "SELECT b.*, COUNT(br.isbn) as borrow_count " +
                "FROM books b " +
                "JOIN borrowing_records br ON b.isbn = br.isbn " +
                "WHERE b.status = 'Available' " +
                "GROUP BY b.isbn " +
                "ORDER BY borrow_count DESC, b.rating DESC " +
                "LIMIT ?";
                
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, MAX_RECOMMENDATIONS);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recommendations.add(extractBookFromResultSet(rs));
                }
            }
        }
        
        return recommendations;
    }
    
    private Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        String isbn = rs.getString("isbn");
        String title = rs.getString("title");
        String author = rs.getString("author");
        int year = rs.getInt("year");
        String category = rs.getString("category");
        double rating = rs.getDouble("rating");
        String status = rs.getString("status");
        
        return new Book(isbn, title, author, year, category, rating, status);
    }
    
    private List<Book> removeDuplicates(List<Book> books) {
        Set<String> seenISBNs = new HashSet<>();
        List<Book> uniqueBooks = new ArrayList<>();
        
        for (Book book : books) {
            if (!seenISBNs.contains(book.getIsbn())) {
                seenISBNs.add(book.getIsbn());
                uniqueBooks.add(book);
            }
        }
        
        return uniqueBooks;
    }
    
    public void updateStudentInterest(String studentId, String category, int interestLevel) {
        if (interestLevel < 1 || interestLevel > 5) {
            throw new IllegalArgumentException("Interest level must be between 1 and 5");
        }
        
        try {
            String upsert = "INSERT INTO student_interests (uid, category, interest_level) " +
                    "VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE interest_level = ?";
                    
            try (PreparedStatement stmt = connection.prepareStatement(upsert)) {
                stmt.setString(1, studentId);
                stmt.setString(2, category);
                stmt.setInt(3, interestLevel);
                stmt.setInt(4, interestLevel);
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error updating student interests: " + e.getMessage());
        }
    }
    
    public Map<String, Integer> getStudentInterests(String studentId) {
        Map<String, Integer> interests = new HashMap<>();
        
        try {
            String query = "SELECT category, interest_level FROM student_interests WHERE uid = ?";
            
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, studentId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        interests.put(rs.getString("category"), rs.getInt("interest_level"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving student interests: " + e.getMessage());
        }
        
        return interests;
    }
    
    public void analyzeStudentBorrowingHistory(String studentId) {
        try {
            // Find most borrowed categories by the student
            String query = "SELECT b.category, COUNT(*) as borrow_count " +
                    "FROM borrowing_records br " +
                    "JOIN books b ON br.isbn = b.isbn " +
                    "WHERE br.student_id = ? " +
                    "GROUP BY b.category " +
                    "ORDER BY borrow_count DESC";
                    
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, studentId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    int maxCount = 0;
                    Map<String, Integer> categoryCounts = new HashMap<>();
                    
                    // First pass to find max count
                    while (rs.next()) {
                        String category = rs.getString("category");
                        int count = rs.getInt("borrow_count");
                        categoryCounts.put(category, count);
                        
                        if (count > maxCount) {
                            maxCount = count;
                        }
                    }
                    
                    // Update interest levels based on borrow count
                    for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                        String category = entry.getKey();
                        int count = entry.getValue();
                        
                        // Scale from 1-5 based on borrow count
                        int interestLevel = (int) Math.ceil(5.0 * count / maxCount);
                        if (interestLevel < 1) interestLevel = 1;
                        
                        updateStudentInterest(studentId, category, interestLevel);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error analyzing borrowing history: " + e.getMessage());
        }
    }
} 