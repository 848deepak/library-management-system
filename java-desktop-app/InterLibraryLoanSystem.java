import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages inter-library loans with partner libraries
 */
public class InterLibraryLoanSystem {
    private Connection connection;
    private int defaultLoanPeriodDays = 30; // Longer than regular loans
    
    /**
     * Creates a new InterLibraryLoanSystem with database connection
     */
    public InterLibraryLoanSystem(Connection connection) {
        this.connection = connection;
        createILLTables();
    }
    
    /**
     * Creates necessary tables for the ILL system
     */
    private void createILLTables() {
        try {
            Statement stmt = connection.createStatement();
            
            // Create partner libraries table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS partner_libraries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "address TEXT, " +
                "contact_email TEXT, " +
                "contact_phone TEXT, " +
                "active BOOLEAN DEFAULT 1" +
                ")"
            );
            
            // Create ILL requests table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS ill_requests (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "author TEXT, " +
                "isbn TEXT, " +
                "request_date TEXT NOT NULL, " +
                "status TEXT NOT NULL, " + // 'PENDING', 'APPROVED', 'DENIED', 'CANCELLED', 'RECEIVED', 'RETURNED'
                "partner_library_id INTEGER, " +
                "due_date TEXT, " +
                "received_date TEXT, " +
                "returned_date TEXT, " +
                "notes TEXT, " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "FOREIGN KEY (partner_library_id) REFERENCES partner_libraries(id)" +
                ")"
            );
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error creating ILL tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Adds a new partner library
     * @return ID of the new library, or -1 if failed
     */
    public int addPartnerLibrary(String name, String address, String email, String phone) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO partner_libraries (name, address, contact_email, contact_phone) " +
                "VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            
            stmt.setString(1, name);
            stmt.setString(2, address);
            stmt.setString(3, email);
            stmt.setString(4, phone);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    rs.close();
                    stmt.close();
                    return id;
                }
            }
            
            stmt.close();
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error adding partner library: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Creates a new ILL request
     * @return ID of the new request, or -1 if failed
     */
    public int createILLRequest(int studentId, String title, String author, String isbn) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO ill_requests (student_id, title, author, isbn, request_date, status) " +
                "VALUES (?, ?, ?, ?, ?, 'PENDING')",
                Statement.RETURN_GENERATED_KEYS
            );
            
            stmt.setInt(1, studentId);
            stmt.setString(2, title);
            stmt.setString(3, author);
            stmt.setString(4, isbn);
            stmt.setString(5, LocalDate.now().toString());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int id = rs.getInt(1);
                    rs.close();
                    stmt.close();
                    return id;
                }
            }
            
            stmt.close();
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error creating ILL request: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Updates the status of an ILL request
     */
    public boolean updateILLRequestStatus(int requestId, String status, String notes) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE ill_requests SET status = ?, notes = ? WHERE id = ?"
            );
            
            stmt.setString(1, status);
            stmt.setString(2, notes);
            stmt.setInt(3, requestId);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating ILL request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Approves an ILL request with a partner library
     */
    public boolean approveILLRequest(int requestId, int partnerLibraryId, LocalDate dueDate) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE ill_requests SET status = 'APPROVED', partner_library_id = ?, " +
                "due_date = ? WHERE id = ?"
            );
            
            stmt.setInt(1, partnerLibraryId);
            stmt.setString(2, dueDate.toString());
            stmt.setInt(3, requestId);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error approving ILL request: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Records that an ILL book has been received
     */
    public boolean receiveILLBook(int requestId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE ill_requests SET status = 'RECEIVED', received_date = ? WHERE id = ?"
            );
            
            stmt.setString(1, LocalDate.now().toString());
            stmt.setInt(2, requestId);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error receiving ILL book: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Returns an ILL book to its owner library
     */
    public boolean returnILLBook(int requestId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE ill_requests SET status = 'RETURNED', returned_date = ? WHERE id = ?"
            );
            
            stmt.setString(1, LocalDate.now().toString());
            stmt.setInt(2, requestId);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error returning ILL book: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets all ILL requests for a student
     */
    public List<ILLRequest> getStudentILLRequests(int studentId) {
        List<ILLRequest> requests = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT r.*, l.name as library_name " +
                "FROM ill_requests r " +
                "LEFT JOIN partner_libraries l ON r.partner_library_id = l.id " +
                "WHERE r.student_id = ? " +
                "ORDER BY r.request_date DESC"
            );
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                requests.add(createILLRequestFromResultSet(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting student ILL requests: " + e.getMessage());
            e.printStackTrace();
        }
        
        return requests;
    }
    
    /**
     * Gets all pending ILL requests
     */
    public List<ILLRequest> getPendingILLRequests() {
        List<ILLRequest> requests = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT r.*, l.name as library_name, " +
                "s.first_name || ' ' || s.last_name as student_name, s.uid as student_uid " +
                "FROM ill_requests r " +
                "LEFT JOIN partner_libraries l ON r.partner_library_id = l.id " +
                "JOIN students s ON r.student_id = s.id " +
                "WHERE r.status = 'PENDING' " +
                "ORDER BY r.request_date ASC"
            );
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                requests.add(createILLRequestFromResultSet(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting pending ILL requests: " + e.getMessage());
            e.printStackTrace();
        }
        
        return requests;
    }
    
    /**
     * Gets all active partner libraries
     */
    public List<PartnerLibrary> getActivePartnerLibraries() {
        List<PartnerLibrary> libraries = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM partner_libraries WHERE active = 1 ORDER BY name"
            );
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                libraries.add(new PartnerLibrary(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("address"),
                    rs.getString("contact_email"),
                    rs.getString("contact_phone"),
                    rs.getBoolean("active")
                ));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting partner libraries: " + e.getMessage());
            e.printStackTrace();
        }
        
        return libraries;
    }
    
    /**
     * Creates an ILLRequest object from a ResultSet
     */
    private ILLRequest createILLRequestFromResultSet(ResultSet rs) throws SQLException {
        LocalDate dueDate = null;
        if (rs.getString("due_date") != null) {
            dueDate = LocalDate.parse(rs.getString("due_date"));
        }
        
        LocalDate receivedDate = null;
        if (rs.getString("received_date") != null) {
            receivedDate = LocalDate.parse(rs.getString("received_date"));
        }
        
        LocalDate returnedDate = null;
        if (rs.getString("returned_date") != null) {
            returnedDate = LocalDate.parse(rs.getString("returned_date"));
        }
        
        return new ILLRequest(
            rs.getInt("id"),
            rs.getInt("student_id"),
            rs.getString("student_name"),
            rs.getString("student_uid"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("isbn"),
            LocalDate.parse(rs.getString("request_date")),
            rs.getString("status"),
            rs.getInt("partner_library_id"),
            rs.getString("library_name"),
            dueDate,
            receivedDate,
            returnedDate,
            rs.getString("notes")
        );
    }
    
    /**
     * Represents a partner library
     */
    public static class PartnerLibrary {
        private int id;
        private String name;
        private String address;
        private String contactEmail;
        private String contactPhone;
        private boolean active;
        
        public PartnerLibrary(int id, String name, String address, 
                             String contactEmail, String contactPhone, boolean active) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.contactEmail = contactEmail;
            this.contactPhone = contactPhone;
            this.active = active;
        }
        
        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getAddress() { return address; }
        public String getContactEmail() { return contactEmail; }
        public String getContactPhone() { return contactPhone; }
        public boolean isActive() { return active; }
    }
    
    /**
     * Represents an inter-library loan request
     */
    public static class ILLRequest {
        private int id;
        private int studentId;
        private String studentName;
        private String studentUid;
        private String title;
        private String author;
        private String isbn;
        private LocalDate requestDate;
        private String status;
        private int partnerLibraryId;
        private String libraryName;
        private LocalDate dueDate;
        private LocalDate receivedDate;
        private LocalDate returnedDate;
        private String notes;
        
        public ILLRequest(int id, int studentId, String studentName, String studentUid,
                        String title, String author, String isbn, LocalDate requestDate,
                        String status, int partnerLibraryId, String libraryName,
                        LocalDate dueDate, LocalDate receivedDate, LocalDate returnedDate,
                        String notes) {
            this.id = id;
            this.studentId = studentId;
            this.studentName = studentName;
            this.studentUid = studentUid;
            this.title = title;
            this.author = author;
            this.isbn = isbn;
            this.requestDate = requestDate;
            this.status = status;
            this.partnerLibraryId = partnerLibraryId;
            this.libraryName = libraryName;
            this.dueDate = dueDate;
            this.receivedDate = receivedDate;
            this.returnedDate = returnedDate;
            this.notes = notes;
        }
        
        // Getters
        public int getId() { return id; }
        public int getStudentId() { return studentId; }
        public String getStudentName() { return studentName; }
        public String getStudentUid() { return studentUid; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getIsbn() { return isbn; }
        public LocalDate getRequestDate() { return requestDate; }
        public String getStatus() { return status; }
        public int getPartnerLibraryId() { return partnerLibraryId; }
        public String getLibraryName() { return libraryName; }
        public LocalDate getDueDate() { return dueDate; }
        public LocalDate getReceivedDate() { return receivedDate; }
        public LocalDate getReturnedDate() { return returnedDate; }
        public String getNotes() { return notes; }
        
        public String getFormattedRequestDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            return requestDate.format(formatter);
        }
        
        public String getFormattedDueDate() {
            if (dueDate == null) return "N/A";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
            return dueDate.format(formatter);
        }
        
        public boolean isOverdue() {
            return status.equals("RECEIVED") && dueDate != null && 
                   LocalDate.now().isAfter(dueDate);
        }
    }
} 