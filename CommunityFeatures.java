import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages community features like book reviews, ratings, and reading lists
 */
public class CommunityFeatures {
    private Connection connection;
    
    /**
     * Creates a new CommunityFeatures with database connection
     */
    public CommunityFeatures(Connection connection) {
        this.connection = connection;
        createCommunityTables();
    }
    
    /**
     * Creates necessary tables for community features
     */
    private void createCommunityTables() {
        try {
            Statement stmt = connection.createStatement();
            
            // Create book reviews table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS book_reviews (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "book_id INTEGER NOT NULL, " +
                "student_id INTEGER NOT NULL, " +
                "review_text TEXT NOT NULL, " +
                "rating INTEGER NOT NULL, " + // 1-5 stars
                "submission_date TEXT NOT NULL, " +
                "likes INTEGER DEFAULT 0, " +
                "FOREIGN KEY (book_id) REFERENCES books(id), " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "UNIQUE(book_id, student_id)" +
                ")"
            );
            
            // Create reading lists table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS reading_lists (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "list_name TEXT NOT NULL, " +
                "description TEXT, " +
                "is_public BOOLEAN DEFAULT 0, " +
                "created_date TEXT NOT NULL, " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "UNIQUE(student_id, list_name)" +
                ")"
            );
            
            // Create reading list items table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS reading_list_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "list_id INTEGER NOT NULL, " +
                "book_id INTEGER NOT NULL, " +
                "date_added TEXT NOT NULL, " +
                "notes TEXT, " +
                "FOREIGN KEY (list_id) REFERENCES reading_lists(id), " +
                "FOREIGN KEY (book_id) REFERENCES books(id), " +
                "UNIQUE(list_id, book_id)" +
                ")"
            );
            
            // Create book discussion table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS book_discussions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "book_id INTEGER NOT NULL, " +
                "student_id INTEGER NOT NULL, " +
                "parent_id INTEGER, " + // NULL for top-level comments
                "comment_text TEXT NOT NULL, " +
                "submission_date TEXT NOT NULL, " +
                "likes INTEGER DEFAULT 0, " +
                "FOREIGN KEY (book_id) REFERENCES books(id), " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "FOREIGN KEY (parent_id) REFERENCES book_discussions(id)" +
                ")"
            );
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error creating community tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Adds a book review
     * @return ID of the new review, or -1 if failed
     */
    public int addBookReview(int bookId, int studentId, String reviewText, int rating) {
        try {
            // First check if this student already reviewed this book
            PreparedStatement checkStmt = connection.prepareStatement(
                "SELECT id FROM book_reviews WHERE book_id = ? AND student_id = ?"
            );
            
            checkStmt.setInt(1, bookId);
            checkStmt.setInt(2, studentId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                // Update existing review
                int reviewId = rs.getInt("id");
                rs.close();
                checkStmt.close();
                
                PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE book_reviews SET review_text = ?, rating = ?, " +
                    "submission_date = ? WHERE id = ?"
                );
                
                updateStmt.setString(1, reviewText);
                updateStmt.setInt(2, rating);
                updateStmt.setString(3, LocalDateTime.now().toString());
                updateStmt.setInt(4, reviewId);
                
                updateStmt.executeUpdate();
                updateStmt.close();
                
                // Update book's rating
                updateBookRating(bookId);
                
                return reviewId;
            }
            
            rs.close();
            checkStmt.close();
            
            // Insert new review
            PreparedStatement insertStmt = connection.prepareStatement(
                "INSERT INTO book_reviews (book_id, student_id, review_text, rating, submission_date) " +
                "VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            
            insertStmt.setInt(1, bookId);
            insertStmt.setInt(2, studentId);
            insertStmt.setString(3, reviewText);
            insertStmt.setInt(4, rating);
            insertStmt.setString(5, LocalDateTime.now().toString());
            
            int rowsAffected = insertStmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int reviewId = generatedKeys.getInt(1);
                    generatedKeys.close();
                    insertStmt.close();
                    
                    // Update book's rating
                    updateBookRating(bookId);
                    
                    return reviewId;
                }
            }
            
            insertStmt.close();
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error adding book review: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Updates a book's overall rating based on reviews
     */
    private void updateBookRating(int bookId) {
        try {
            // Calculate average rating
            PreparedStatement calcStmt = connection.prepareStatement(
                "SELECT AVG(rating) as avg_rating, COUNT(*) as count " +
                "FROM book_reviews WHERE book_id = ?"
            );
            
            calcStmt.setInt(1, bookId);
            ResultSet rs = calcStmt.executeQuery();
            
            if (rs.next()) {
                double avgRating = rs.getDouble("avg_rating");
                int count = rs.getInt("count");
                
                // Update book rating
                PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE books SET rating = ?, rating_count = ? WHERE id = ?"
                );
                
                updateStmt.setDouble(1, avgRating);
                updateStmt.setInt(2, count);
                updateStmt.setInt(3, bookId);
                updateStmt.executeUpdate();
                updateStmt.close();
            }
            
            rs.close();
            calcStmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error updating book rating: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a reading list
     * @return ID of the new list, or -1 if failed
     */
    public int createReadingList(int studentId, String listName, String description, boolean isPublic) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO reading_lists (student_id, list_name, description, is_public, created_date) " +
                "VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            
            stmt.setInt(1, studentId);
            stmt.setString(2, listName);
            stmt.setString(3, description);
            stmt.setBoolean(4, isPublic);
            stmt.setString(5, LocalDateTime.now().toString());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int listId = generatedKeys.getInt(1);
                    generatedKeys.close();
                    stmt.close();
                    return listId;
                }
            }
            
            stmt.close();
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error creating reading list: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Adds a book to a reading list
     * @return true if successfully added
     */
    public boolean addBookToReadingList(int listId, int bookId, String notes) {
        try {
            // Check if the book is already in the list
            PreparedStatement checkStmt = connection.prepareStatement(
                "SELECT id FROM reading_list_items WHERE list_id = ? AND book_id = ?"
            );
            
            checkStmt.setInt(1, listId);
            checkStmt.setInt(2, bookId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                // Book already in list, update notes if provided
                if (notes != null && !notes.isEmpty()) {
                    int itemId = rs.getInt("id");
                    rs.close();
                    checkStmt.close();
                    
                    PreparedStatement updateStmt = connection.prepareStatement(
                        "UPDATE reading_list_items SET notes = ? WHERE id = ?"
                    );
                    
                    updateStmt.setString(1, notes);
                    updateStmt.setInt(2, itemId);
                    updateStmt.executeUpdate();
                    updateStmt.close();
                }
                return true;
            }
            
            rs.close();
            checkStmt.close();
            
            // Add book to list
            PreparedStatement insertStmt = connection.prepareStatement(
                "INSERT INTO reading_list_items (list_id, book_id, date_added, notes) " +
                "VALUES (?, ?, ?, ?)"
            );
            
            insertStmt.setInt(1, listId);
            insertStmt.setInt(2, bookId);
            insertStmt.setString(3, LocalDateTime.now().toString());
            insertStmt.setString(4, notes);
            
            int rowsAffected = insertStmt.executeUpdate();
            insertStmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error adding book to reading list: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Adds a comment to a book discussion
     * @param bookId Book ID
     * @param studentId Student ID
     * @param commentText Comment text
     * @param parentId Parent comment ID (null for top-level comments)
     * @return ID of the new comment, or -1 if failed
     */
    public int addBookComment(int bookId, int studentId, String commentText, Integer parentId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO book_discussions (book_id, student_id, parent_id, comment_text, submission_date) " +
                "VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            
            stmt.setInt(1, bookId);
            stmt.setInt(2, studentId);
            if (parentId != null) {
                stmt.setInt(3, parentId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setString(4, commentText);
            stmt.setString(5, LocalDateTime.now().toString());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int commentId = generatedKeys.getInt(1);
                    generatedKeys.close();
                    stmt.close();
                    return commentId;
                }
            }
            
            stmt.close();
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error adding book comment: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Gets reviews for a book
     * @param bookId Book ID
     * @return List of reviews
     */
    public List<BookReview> getBookReviews(int bookId) {
        List<BookReview> reviews = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT r.*, s.uid, s.first_name || ' ' || s.last_name as student_name " +
                "FROM book_reviews r " +
                "JOIN students s ON r.student_id = s.id " +
                "WHERE r.book_id = ? " +
                "ORDER BY r.submission_date DESC"
            );
            
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                BookReview review = new BookReview(
                    rs.getInt("id"),
                    rs.getInt("book_id"),
                    rs.getInt("student_id"),
                    rs.getString("uid"),
                    rs.getString("student_name"),
                    rs.getString("review_text"),
                    rs.getInt("rating"),
                    LocalDateTime.parse(rs.getString("submission_date")),
                    rs.getInt("likes")
                );
                reviews.add(review);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting book reviews: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reviews;
    }
    
    /**
     * Gets reading lists for a student
     * @param studentId Student ID
     * @return List of reading lists
     */
    public List<ReadingList> getStudentReadingLists(int studentId) {
        List<ReadingList> lists = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT l.*, " +
                "(SELECT COUNT(*) FROM reading_list_items i WHERE i.list_id = l.id) as book_count " +
                "FROM reading_lists l " +
                "WHERE l.student_id = ? " +
                "ORDER BY l.created_date DESC"
            );
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ReadingList list = new ReadingList(
                    rs.getInt("id"),
                    rs.getInt("student_id"),
                    rs.getString("list_name"),
                    rs.getString("description"),
                    rs.getBoolean("is_public"),
                    LocalDateTime.parse(rs.getString("created_date")),
                    rs.getInt("book_count")
                );
                lists.add(list);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting student reading lists: " + e.getMessage());
            e.printStackTrace();
        }
        
        return lists;
    }
    
    /**
     * Gets books in a reading list
     * @param listId Reading list ID
     * @return List of books with notes
     */
    public List<ReadingListItem> getReadingListBooks(int listId) {
        List<ReadingListItem> items = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT i.*, b.title, b.author, b.isbn, b.is_available " +
                "FROM reading_list_items i " +
                "JOIN books b ON i.book_id = b.id " +
                "WHERE i.list_id = ? " +
                "ORDER BY i.date_added DESC"
            );
            
            stmt.setInt(1, listId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                ReadingListItem item = new ReadingListItem(
                    rs.getInt("id"),
                    rs.getInt("list_id"),
                    rs.getInt("book_id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("isbn"),
                    rs.getBoolean("is_available"),
                    LocalDateTime.parse(rs.getString("date_added")),
                    rs.getString("notes")
                );
                items.add(item);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting reading list books: " + e.getMessage());
            e.printStackTrace();
        }
        
        return items;
    }
    
    /**
     * Gets discussion comments for a book
     * @param bookId Book ID
     * @return List of comments
     */
    public List<BookComment> getBookComments(int bookId) {
        List<BookComment> comments = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT d.*, s.uid, s.first_name || ' ' || s.last_name as student_name " +
                "FROM book_discussions d " +
                "JOIN students s ON d.student_id = s.id " +
                "WHERE d.book_id = ? " +
                "ORDER BY d.submission_date ASC"
            );
            
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                BookComment comment = new BookComment(
                    rs.getInt("id"),
                    rs.getInt("book_id"),
                    rs.getInt("student_id"),
                    rs.getString("uid"),
                    rs.getString("student_name"),
                    rs.getObject("parent_id", Integer.class),
                    rs.getString("comment_text"),
                    LocalDateTime.parse(rs.getString("submission_date")),
                    rs.getInt("likes")
                );
                comments.add(comment);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting book comments: " + e.getMessage());
            e.printStackTrace();
        }
        
        return comments;
    }
    
    /**
     * Represents a book review
     */
    public static class BookReview {
        private int id;
        private int bookId;
        private int studentId;
        private String studentUid;
        private String studentName;
        private String reviewText;
        private int rating;
        private LocalDateTime submissionDate;
        private int likes;
        
        public BookReview(int id, int bookId, int studentId, String studentUid, String studentName,
                         String reviewText, int rating, LocalDateTime submissionDate, int likes) {
            this.id = id;
            this.bookId = bookId;
            this.studentId = studentId;
            this.studentUid = studentUid;
            this.studentName = studentName;
            this.reviewText = reviewText;
            this.rating = rating;
            this.submissionDate = submissionDate;
            this.likes = likes;
        }
        
        // Getters
        public int getId() { return id; }
        public int getBookId() { return bookId; }
        public int getStudentId() { return studentId; }
        public String getStudentUid() { return studentUid; }
        public String getStudentName() { return studentName; }
        public String getReviewText() { return reviewText; }
        public int getRating() { return rating; }
        public LocalDateTime getSubmissionDate() { return submissionDate; }
        public int getLikes() { return likes; }
        
        public String getFormattedDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
            return submissionDate.format(formatter);
        }
        
        public String getStarRating() {
            return "★".repeat(rating) + "☆".repeat(5 - rating);
        }
    }
    
    /**
     * Represents a reading list
     */
    public static class ReadingList {
        private int id;
        private int studentId;
        private String name;
        private String description;
        private boolean isPublic;
        private LocalDateTime createdDate;
        private int bookCount;
        
        public ReadingList(int id, int studentId, String name, String description,
                          boolean isPublic, LocalDateTime createdDate, int bookCount) {
            this.id = id;
            this.studentId = studentId;
            this.name = name;
            this.description = description;
            this.isPublic = isPublic;
            this.createdDate = createdDate;
            this.bookCount = bookCount;
        }
        
        // Getters
        public int getId() { return id; }
        public int getStudentId() { return studentId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public boolean isPublic() { return isPublic; }
        public LocalDateTime getCreatedDate() { return createdDate; }
        public int getBookCount() { return bookCount; }
        
        public String getVisibility() {
            return isPublic ? "Public" : "Private";
        }
        
        public String getFormattedDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            return createdDate.format(formatter);
        }
    }
    
    /**
     * Represents a book in a reading list
     */
    public static class ReadingListItem {
        private int id;
        private int listId;
        private int bookId;
        private String bookTitle;
        private String bookAuthor;
        private String bookIsbn;
        private boolean isAvailable;
        private LocalDateTime dateAdded;
        private String notes;
        
        public ReadingListItem(int id, int listId, int bookId, String bookTitle, String bookAuthor,
                              String bookIsbn, boolean isAvailable, LocalDateTime dateAdded, String notes) {
            this.id = id;
            this.listId = listId;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.bookAuthor = bookAuthor;
            this.bookIsbn = bookIsbn;
            this.isAvailable = isAvailable;
            this.dateAdded = dateAdded;
            this.notes = notes;
        }
        
        // Getters
        public int getId() { return id; }
        public int getListId() { return listId; }
        public int getBookId() { return bookId; }
        public String getBookTitle() { return bookTitle; }
        public String getBookAuthor() { return bookAuthor; }
        public String getBookIsbn() { return bookIsbn; }
        public boolean isAvailable() { return isAvailable; }
        public LocalDateTime getDateAdded() { return dateAdded; }
        public String getNotes() { return notes; }
        
        public String getFormattedDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            return dateAdded.format(formatter);
        }
    }
    
    /**
     * Represents a book discussion comment
     */
    public static class BookComment {
        private int id;
        private int bookId;
        private int studentId;
        private String studentUid;
        private String studentName;
        private Integer parentId;
        private String commentText;
        private LocalDateTime submissionDate;
        private int likes;
        
        public BookComment(int id, int bookId, int studentId, String studentUid, String studentName,
                          Integer parentId, String commentText, LocalDateTime submissionDate, int likes) {
            this.id = id;
            this.bookId = bookId;
            this.studentId = studentId;
            this.studentUid = studentUid;
            this.studentName = studentName;
            this.parentId = parentId;
            this.commentText = commentText;
            this.submissionDate = submissionDate;
            this.likes = likes;
        }
        
        // Getters
        public int getId() { return id; }
        public int getBookId() { return bookId; }
        public int getStudentId() { return studentId; }
        public String getStudentUid() { return studentUid; }
        public String getStudentName() { return studentName; }
        public Integer getParentId() { return parentId; }
        public String getCommentText() { return commentText; }
        public LocalDateTime getSubmissionDate() { return submissionDate; }
        public int getLikes() { return likes; }
        
        public boolean isReply() {
            return parentId != null;
        }
        
        public String getFormattedDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
            return submissionDate.format(formatter);
        }
    }
} 