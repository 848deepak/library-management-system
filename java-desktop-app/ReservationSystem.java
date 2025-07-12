import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages book reservations and waiting lists
 */
public class ReservationSystem {
    private Connection connection;
    private int maxReservationsPerStudent = 5;
    private int reservationExpiryDays = 3; // Days a reservation is held after book becomes available
    
    /**
     * Creates a new ReservationSystem with database connection
     */
    public ReservationSystem(Connection connection) {
        this.connection = connection;
        createReservationTables();
    }
    
    /**
     * Creates the necessary tables for the reservation system
     */
    private void createReservationTables() {
        try {
            Statement stmt = connection.createStatement();
            
            // Create reservations table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS reservations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "book_id INTEGER NOT NULL, " +
                "student_id INTEGER NOT NULL, " +
                "reservation_date TEXT NOT NULL, " +
                "status TEXT NOT NULL, " + // 'WAITING', 'READY', 'EXPIRED', 'FULFILLED', 'CANCELLED'
                "notification_sent BOOLEAN DEFAULT 0, " +
                "expiry_date TEXT, " +
                "fulfillment_date TEXT, " +
                "queue_position INTEGER, " +
                "notes TEXT, " +
                "FOREIGN KEY (book_id) REFERENCES books(id), " +
                "FOREIGN KEY (student_id) REFERENCES students(id)" +
                ")"
            );
            
            // Create notification_log table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS notification_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "reservation_id INTEGER, " +
                "student_id INTEGER NOT NULL, " +
                "message TEXT NOT NULL, " +
                "type TEXT NOT NULL, " + // 'READY', 'EXPIRY', 'REMINDER', etc.
                "sent_date TEXT NOT NULL, " +
                "read_status BOOLEAN DEFAULT 0, " +
                "FOREIGN KEY (reservation_id) REFERENCES reservations(id), " +
                "FOREIGN KEY (student_id) REFERENCES students(id)" +
                ")"
            );
            
            // Create active_reservations view
            stmt.execute(
                "CREATE VIEW IF NOT EXISTS active_reservations AS " +
                "SELECT r.id as reservation_id, r.status, r.reservation_date, r.expiry_date, r.queue_position, " +
                "s.id as student_id, s.uid, s.first_name, s.last_name, " +
                "b.id as book_id, b.title, b.isbn, b.is_available " +
                "FROM reservations r " +
                "JOIN books b ON r.book_id = b.id " +
                "JOIN students s ON r.student_id = s.id " +
                "WHERE r.status IN ('WAITING', 'READY')"
            );
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error creating reservation tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Sets the maximum number of active reservations allowed per student
     */
    public void setMaxReservationsPerStudent(int max) {
        if (max > 0) {
            this.maxReservationsPerStudent = max;
        }
    }
    
    /**
     * Sets the number of days a reservation is held after a book becomes available
     */
    public void setReservationExpiryDays(int days) {
        if (days > 0) {
            this.reservationExpiryDays = days;
        }
    }
    
    /**
     * Reserves a book for a student
     * @param bookId The book ID
     * @param studentId The student ID
     * @return Reservation ID if successful, -1 if failed, -2 if limit reached
     */
    public int reserveBook(int bookId, int studentId) {
        try {
            // Check if student already has this book reserved
            PreparedStatement checkStmt = connection.prepareStatement(
                "SELECT id FROM reservations " +
                "WHERE book_id = ? AND student_id = ? AND status IN ('WAITING', 'READY')"
            );
            
            checkStmt.setInt(1, bookId);
            checkStmt.setInt(2, studentId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                // Already reserved
                int reservationId = rs.getInt("id");
                rs.close();
                checkStmt.close();
                return reservationId;
            }
            
            rs.close();
            
            // Check if student has reached reservation limit
            checkStmt = connection.prepareStatement(
                "SELECT COUNT(*) as count FROM reservations " +
                "WHERE student_id = ? AND status IN ('WAITING', 'READY')"
            );
            
            checkStmt.setInt(1, studentId);
            rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt("count") >= maxReservationsPerStudent) {
                // Limit reached
                rs.close();
                checkStmt.close();
                return -2;
            }
            
            rs.close();
            checkStmt.close();
            
            // Check if book is available
            PreparedStatement bookStmt = connection.prepareStatement(
                "SELECT is_available FROM books WHERE id = ?"
            );
            
            bookStmt.setInt(1, bookId);
            rs = bookStmt.executeQuery();
            
            boolean isAvailable = false;
            if (rs.next()) {
                isAvailable = rs.getBoolean("is_available");
            }
            
            rs.close();
            bookStmt.close();
            
            // Determine status and queue position
            String status = isAvailable ? "READY" : "WAITING";
            int queuePosition = 0;
            
            if (!isAvailable) {
                // Get current max queue position for this book
                PreparedStatement queueStmt = connection.prepareStatement(
                    "SELECT MAX(queue_position) as max_pos FROM reservations " +
                    "WHERE book_id = ? AND status = 'WAITING'"
                );
                
                queueStmt.setInt(1, bookId);
                rs = queueStmt.executeQuery();
                
                if (rs.next()) {
                    queuePosition = rs.getInt("max_pos") + 1;
                } else {
                    queuePosition = 1;
                }
                
                rs.close();
                queueStmt.close();
            }
            
            // Create reservation
            PreparedStatement insertStmt = connection.prepareStatement(
                "INSERT INTO reservations (book_id, student_id, reservation_date, status, queue_position, expiry_date) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            
            LocalDateTime now = LocalDateTime.now();
            insertStmt.setInt(1, bookId);
            insertStmt.setInt(2, studentId);
            insertStmt.setString(3, now.toString());
            insertStmt.setString(4, status);
            insertStmt.setInt(5, queuePosition);
            
            // Set expiry date if book is available
            if (isAvailable) {
                insertStmt.setString(6, LocalDate.now().plusDays(reservationExpiryDays).toString());
            } else {
                insertStmt.setNull(6, Types.VARCHAR);
            }
            
            int rowsAffected = insertStmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Get the generated ID
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                int reservationId = -1;
                
                if (generatedKeys.next()) {
                    reservationId = generatedKeys.getInt(1);
                    
                    // If book is available, mark it as unavailable
                    if (isAvailable) {
                        PreparedStatement updateBookStmt = connection.prepareStatement(
                            "UPDATE books SET is_available = 0 WHERE id = ?"
                        );
                        
                        updateBookStmt.setInt(1, bookId);
                        updateBookStmt.executeUpdate();
                        updateBookStmt.close();
                        
                        // Create a notification
                        addNotification(reservationId, studentId, 
                                        "The book you reserved '" + getBookTitle(bookId) + "' is available for pickup. " +
                                        "Please collect it within " + reservationExpiryDays + " days.",
                                        "READY");
                    }
                }
                
                generatedKeys.close();
                insertStmt.close();
                return reservationId;
            }
            
            insertStmt.close();
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error reserving book: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Gets the title of a book by ID
     */
    private String getBookTitle(int bookId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT title FROM books WHERE id = ?"
            );
            
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            
            String title = "Unknown Book";
            if (rs.next()) {
                title = rs.getString("title");
            }
            
            rs.close();
            stmt.close();
            return title;
        } catch (SQLException e) {
            return "Unknown Book";
        }
    }
    
    /**
     * Processes a book return and updates reservations accordingly
     * @param bookId The book ID
     * @return ID of the reservation that now has the book available, or -1 if none
     */
    public int processReturn(int bookId) {
        try {
            // Find the next reservation in the queue
            PreparedStatement findStmt = connection.prepareStatement(
                "SELECT id, student_id FROM reservations " +
                "WHERE book_id = ? AND status = 'WAITING' " +
                "ORDER BY queue_position ASC LIMIT 1"
            );
            
            findStmt.setInt(1, bookId);
            ResultSet rs = findStmt.executeQuery();
            
            if (rs.next()) {
                int reservationId = rs.getInt("id");
                int studentId = rs.getInt("student_id");
                
                // Update the reservation
                PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE reservations SET status = 'READY', " +
                    "expiry_date = ?, notification_sent = 0 " +
                    "WHERE id = ?"
                );
                
                updateStmt.setString(1, LocalDate.now().plusDays(reservationExpiryDays).toString());
                updateStmt.setInt(2, reservationId);
                updateStmt.executeUpdate();
                updateStmt.close();
                
                // Update queue positions for remaining reservations
                PreparedStatement queueStmt = connection.prepareStatement(
                    "UPDATE reservations SET queue_position = queue_position - 1 " +
                    "WHERE book_id = ? AND status = 'WAITING' AND queue_position > 1"
                );
                
                queueStmt.setInt(1, bookId);
                queueStmt.executeUpdate();
                queueStmt.close();
                
                // Create a notification
                addNotification(reservationId, studentId, 
                               "The book you reserved '" + getBookTitle(bookId) + "' is now available for pickup. " +
                               "Please collect it within " + reservationExpiryDays + " days.",
                               "READY");
                
                rs.close();
                findStmt.close();
                return reservationId;
            }
            
            rs.close();
            findStmt.close();
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error processing return: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Cancels a reservation
     * @param reservationId The reservation ID
     * @return true if successful
     */
    public boolean cancelReservation(int reservationId) {
        try {
            // Get reservation details
            PreparedStatement getStmt = connection.prepareStatement(
                "SELECT book_id, status, queue_position FROM reservations WHERE id = ?"
            );
            
            getStmt.setInt(1, reservationId);
            ResultSet rs = getStmt.executeQuery();
            
            if (!rs.next()) {
                rs.close();
                getStmt.close();
                return false;
            }
            
            int bookId = rs.getInt("book_id");
            String status = rs.getString("status");
            int queuePosition = rs.getInt("queue_position");
            
            rs.close();
            getStmt.close();
            
            // Update reservation status
            PreparedStatement updateStmt = connection.prepareStatement(
                "UPDATE reservations SET status = 'CANCELLED' WHERE id = ?"
            );
            
            updateStmt.setInt(1, reservationId);
            updateStmt.executeUpdate();
            updateStmt.close();
            
            // If the reservation was for a waiting book, update queue positions
            if ("WAITING".equals(status)) {
                PreparedStatement queueStmt = connection.prepareStatement(
                    "UPDATE reservations SET queue_position = queue_position - 1 " +
                    "WHERE book_id = ? AND status = 'WAITING' AND queue_position > ?"
                );
                
                queueStmt.setInt(1, bookId);
                queueStmt.setInt(2, queuePosition);
                queueStmt.executeUpdate();
                queueStmt.close();
            } else if ("READY".equals(status)) {
                // If the book was reserved and ready, process the next reservation
                processReturn(bookId);
            }
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Error cancelling reservation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Fulfills a reservation (book is borrowed by the student)
     * @param reservationId The reservation ID
     * @return true if successful
     */
    public boolean fulfillReservation(int reservationId) {
        try {
            PreparedStatement updateStmt = connection.prepareStatement(
                "UPDATE reservations SET status = 'FULFILLED', fulfillment_date = ? WHERE id = ?"
            );
            
            updateStmt.setString(1, LocalDate.now().toString());
            updateStmt.setInt(2, reservationId);
            int result = updateStmt.executeUpdate();
            updateStmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            System.err.println("Error fulfilling reservation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Processes expired reservations
     * @return Number of reservations expired
     */
    public int processExpiredReservations() {
        int count = 0;
        
        try {
            // Find expired READY reservations
            PreparedStatement findStmt = connection.prepareStatement(
                "SELECT id, book_id, student_id FROM reservations " +
                "WHERE status = 'READY' AND expiry_date < ?"
            );
            
            findStmt.setString(1, LocalDate.now().toString());
            ResultSet rs = findStmt.executeQuery();
            
            List<Integer> bookIds = new ArrayList<>();
            
            while (rs.next()) {
                int reservationId = rs.getInt("id");
                int bookId = rs.getInt("book_id");
                int studentId = rs.getInt("student_id");
                
                // Update reservation status
                PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE reservations SET status = 'EXPIRED' WHERE id = ?"
                );
                
                updateStmt.setInt(1, reservationId);
                updateStmt.executeUpdate();
                updateStmt.close();
                
                // Create a notification
                addNotification(reservationId, studentId, 
                               "Your reservation for '" + getBookTitle(bookId) + "' has expired.",
                               "EXPIRY");
                
                bookIds.add(bookId);
                count++;
            }
            
            rs.close();
            findStmt.close();
            
            // Process the next reservation for each book
            for (int bookId : bookIds) {
                processReturn(bookId);
            }
            
        } catch (SQLException e) {
            System.err.println("Error processing expired reservations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return count;
    }
    
    /**
     * Gets active reservations for a student
     * @param studentId The student ID
     * @return List of reservations
     */
    public List<Reservation> getReservationsForStudent(int studentId) {
        List<Reservation> reservations = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM active_reservations WHERE student_id = ?"
            );
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                reservations.add(createReservationFromResultSet(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting reservations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reservations;
    }
    
    /**
     * Gets all active reservations for a book
     * @param bookId The book ID
     * @return List of reservations
     */
    public List<Reservation> getReservationsForBook(int bookId) {
        List<Reservation> reservations = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM active_reservations WHERE book_id = ? ORDER BY queue_position ASC"
            );
            
            stmt.setInt(1, bookId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                reservations.add(createReservationFromResultSet(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting reservations for book: " + e.getMessage());
            e.printStackTrace();
        }
        
        return reservations;
    }
    
    /**
     * Gets all books with waiting lists
     * @return List of books with their waiting list counts
     */
    public List<WaitingListBook> getBooksWithWaitingLists() {
        List<WaitingListBook> books = new ArrayList<>();
        
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT b.id, b.title, b.isbn, b.author, COUNT(r.id) as waiting_count " +
                "FROM books b " +
                "JOIN reservations r ON b.id = r.book_id " +
                "WHERE r.status = 'WAITING' " +
                "GROUP BY b.id " +
                "ORDER BY waiting_count DESC"
            );
            
            while (rs.next()) {
                WaitingListBook book = new WaitingListBook(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("isbn"),
                    rs.getString("author"),
                    rs.getInt("waiting_count")
                );
                books.add(book);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting books with waiting lists: " + e.getMessage());
            e.printStackTrace();
        }
        
        return books;
    }
    
    /**
     * Creates a notification for a student
     */
    private void addNotification(int reservationId, int studentId, String message, String type) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO notification_log (reservation_id, student_id, message, type, sent_date) " +
                "VALUES (?, ?, ?, ?, ?)"
            );
            
            stmt.setInt(1, reservationId);
            stmt.setInt(2, studentId);
            stmt.setString(3, message);
            stmt.setString(4, type);
            stmt.setString(5, LocalDateTime.now().toString());
            
            stmt.executeUpdate();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error adding notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gets all unread notifications for a student
     * @param studentId The student ID
     * @return List of notifications
     */
    public List<Notification> getUnreadNotifications(int studentId) {
        List<Notification> notifications = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM notification_log " +
                "WHERE student_id = ? AND read_status = 0 " +
                "ORDER BY sent_date DESC"
            );
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(new Notification(
                    rs.getInt("id"),
                    rs.getInt("reservation_id"),
                    rs.getInt("student_id"),
                    rs.getString("message"),
                    rs.getString("type"),
                    rs.getString("sent_date")
                ));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting notifications: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notifications;
    }
    
    /**
     * Marks a notification as read
     * @param notificationId The notification ID
     * @return true if successful
     */
    public boolean markNotificationAsRead(int notificationId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE notification_log SET read_status = 1 WHERE id = ?"
            );
            
            stmt.setInt(1, notificationId);
            int result = stmt.executeUpdate();
            stmt.close();
            
            return result > 0;
            
        } catch (SQLException e) {
            System.err.println("Error marking notification as read: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Creates a Reservation object from a ResultSet
     */
    private Reservation createReservationFromResultSet(ResultSet rs) throws SQLException {
        return new Reservation(
            rs.getInt("reservation_id"),
            rs.getInt("book_id"),
            rs.getString("title"),
            rs.getString("isbn"),
            rs.getInt("student_id"),
            rs.getString("uid"),
            rs.getString("first_name") + " " + rs.getString("last_name"),
            rs.getString("status"),
            rs.getString("reservation_date"),
            rs.getString("expiry_date"),
            rs.getInt("queue_position"),
            rs.getBoolean("is_available")
        );
    }
    
    /**
     * Represents a book reservation
     */
    public static class Reservation {
        private int id;
        private int bookId;
        private String bookTitle;
        private String bookIsbn;
        private int studentId;
        private String studentUid;
        private String studentName;
        private String status;
        private String reservationDate;
        private String expiryDate;
        private int queuePosition;
        private boolean isBookAvailable;
        
        public Reservation(int id, int bookId, String bookTitle, String bookIsbn,
                         int studentId, String studentUid, String studentName,
                         String status, String reservationDate, String expiryDate,
                         int queuePosition, boolean isBookAvailable) {
            this.id = id;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.bookIsbn = bookIsbn;
            this.studentId = studentId;
            this.studentUid = studentUid;
            this.studentName = studentName;
            this.status = status;
            this.reservationDate = reservationDate;
            this.expiryDate = expiryDate;
            this.queuePosition = queuePosition;
            this.isBookAvailable = isBookAvailable;
        }
        
        // Getters
        public int getId() { return id; }
        public int getBookId() { return bookId; }
        public String getBookTitle() { return bookTitle; }
        public String getBookIsbn() { return bookIsbn; }
        public int getStudentId() { return studentId; }
        public String getStudentUid() { return studentUid; }
        public String getStudentName() { return studentName; }
        public String getStatus() { return status; }
        public String getReservationDate() { return reservationDate; }
        public String getExpiryDate() { return expiryDate; }
        public int getQueuePosition() { return queuePosition; }
        public boolean isBookAvailable() { return isBookAvailable; }
        
        @Override
        public String toString() {
            if ("WAITING".equals(status)) {
                return String.format("Reservation #%d: %s is waiting for '%s' - Queue Position: %d",
                    id, studentName, bookTitle, queuePosition);
            } else {
                return String.format("Reservation #%d: %s can pickup '%s' until %s",
                    id, studentName, bookTitle, expiryDate);
            }
        }
    }
    
    /**
     * Represents a book with waiting list
     */
    public static class WaitingListBook {
        private int id;
        private String title;
        private String isbn;
        private String author;
        private int waitingCount;
        
        public WaitingListBook(int id, String title, String isbn, String author, int waitingCount) {
            this.id = id;
            this.title = title;
            this.isbn = isbn;
            this.author = author;
            this.waitingCount = waitingCount;
        }
        
        // Getters
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getIsbn() { return isbn; }
        public String getAuthor() { return author; }
        public int getWaitingCount() { return waitingCount; }
        
        @Override
        public String toString() {
            return String.format("'%s' by %s - %d students waiting", title, author, waitingCount);
        }
    }
    
    /**
     * Represents a notification
     */
    public static class Notification {
        private int id;
        private int reservationId;
        private int studentId;
        private String message;
        private String type;
        private String sentDate;
        
        public Notification(int id, int reservationId, int studentId, String message, String type, String sentDate) {
            this.id = id;
            this.reservationId = reservationId;
            this.studentId = studentId;
            this.message = message;
            this.type = type;
            this.sentDate = sentDate;
        }
        
        // Getters
        public int getId() { return id; }
        public int getReservationId() { return reservationId; }
        public int getStudentId() { return studentId; }
        public String getMessage() { return message; }
        public String getType() { return type; }
        public String getSentDate() { return sentDate; }
        
        @Override
        public String toString() {
            return String.format("[%s] %s - %s", type, message, sentDate);
        }
    }
} 