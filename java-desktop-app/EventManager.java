import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages library events such as book clubs, author readings, and workshops
 */
public class EventManager {
    private Connection connection;
    
    /**
     * Creates a new EventManager with database connection
     */
    public EventManager(Connection connection) {
        this.connection = connection;
        createEventTables();
    }
    
    /**
     * Creates the necessary tables for event management
     */
    private void createEventTables() {
        try {
            Statement stmt = connection.createStatement();
            
            // Create events table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS events (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT NOT NULL, " +
                "description TEXT, " +
                "event_type TEXT NOT NULL, " + // 'BOOK_CLUB', 'AUTHOR_EVENT', 'WORKSHOP', 'STUDY_GROUP'
                "start_date TEXT NOT NULL, " +
                "end_date TEXT NOT NULL, " +
                "location TEXT, " +
                "max_participants INTEGER, " +
                "organizer_id INTEGER, " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY (organizer_id) REFERENCES students(id)" +
                ")"
            );
            
            // Create event participants table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS event_participants (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "event_id INTEGER NOT NULL, " +
                "student_id INTEGER NOT NULL, " +
                "registration_date TEXT NOT NULL, " +
                "attendance_status TEXT DEFAULT 'REGISTERED', " + // 'REGISTERED', 'ATTENDED', 'CANCELLED'
                "FOREIGN KEY (event_id) REFERENCES events(id), " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "UNIQUE(event_id, student_id)" +
                ")"
            );
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error creating event tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a new event
     * @param title Event title
     * @param description Event description
     * @param eventType Type of event
     * @param startDate Start date and time
     * @param endDate End date and time
     * @param location Event location
     * @param maxParticipants Maximum number of participants (0 for unlimited)
     * @param organizerId ID of the event organizer
     * @return ID of the created event, or -1 if failed
     */
    public int createEvent(String title, String description, String eventType, 
                          LocalDateTime startDate, LocalDateTime endDate, 
                          String location, int maxParticipants, int organizerId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO events (title, description, event_type, start_date, end_date, " +
                "location, max_participants, organizer_id, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setString(3, eventType);
            stmt.setString(4, startDate.toString());
            stmt.setString(5, endDate.toString());
            stmt.setString(6, location);
            stmt.setInt(7, maxParticipants);
            stmt.setInt(8, organizerId);
            stmt.setString(9, LocalDateTime.now().toString());
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    int eventId = rs.getInt(1);
                    rs.close();
                    stmt.close();
                    return eventId;
                }
            }
            
            stmt.close();
            return -1;
            
        } catch (SQLException e) {
            System.err.println("Error creating event: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    /**
     * Updates an existing event
     * @param eventId Event ID
     * @param title Event title
     * @param description Event description
     * @param eventType Type of event
     * @param startDate Start date and time
     * @param endDate End date and time
     * @param location Event location
     * @param maxParticipants Maximum number of participants
     * @return true if successful
     */
    public boolean updateEvent(int eventId, String title, String description, String eventType,
                             LocalDateTime startDate, LocalDateTime endDate,
                             String location, int maxParticipants) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE events SET title = ?, description = ?, event_type = ?, " +
                "start_date = ?, end_date = ?, location = ?, max_participants = ? " +
                "WHERE id = ?"
            );
            
            stmt.setString(1, title);
            stmt.setString(2, description);
            stmt.setString(3, eventType);
            stmt.setString(4, startDate.toString());
            stmt.setString(5, endDate.toString());
            stmt.setString(6, location);
            stmt.setInt(7, maxParticipants);
            stmt.setInt(8, eventId);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Deletes an event and all its registrations
     * @param eventId Event ID
     * @return true if successful
     */
    public boolean deleteEvent(int eventId) {
        try {
            // First delete all registrations
            PreparedStatement stmt1 = connection.prepareStatement(
                "DELETE FROM event_participants WHERE event_id = ?"
            );
            
            stmt1.setInt(1, eventId);
            stmt1.executeUpdate();
            stmt1.close();
            
            // Then delete the event
            PreparedStatement stmt2 = connection.prepareStatement(
                "DELETE FROM events WHERE id = ?"
            );
            
            stmt2.setInt(1, eventId);
            int rowsAffected = stmt2.executeUpdate();
            stmt2.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Registers a student for an event
     * @param eventId Event ID
     * @param studentId Student ID
     * @return true if registration successful, false if failed
     */
    public boolean registerForEvent(int eventId, int studentId) {
        try {
            // Check if event is full
            PreparedStatement checkStmt = connection.prepareStatement(
                "SELECT count(*) as participant_count, max_participants " +
                "FROM event_participants ep " +
                "JOIN events e ON ep.event_id = e.id " +
                "WHERE event_id = ? AND attendance_status != 'CANCELLED'"
            );
            
            checkStmt.setInt(1, eventId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                int participantCount = rs.getInt("participant_count");
                int maxParticipants = rs.getInt("max_participants");
                
                if (maxParticipants > 0 && participantCount >= maxParticipants) {
                    rs.close();
                    checkStmt.close();
                    return false; // Event is full
                }
            }
            
            rs.close();
            checkStmt.close();
            
            // Register the student
            PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO event_participants " +
                "(event_id, student_id, registration_date, attendance_status) " +
                "VALUES (?, ?, ?, 'REGISTERED')"
            );
            
            stmt.setInt(1, eventId);
            stmt.setInt(2, studentId);
            stmt.setString(3, LocalDateTime.now().toString());
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error registering for event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Cancels a student's registration for an event
     * @param eventId Event ID
     * @param studentId Student ID
     * @return true if cancellation successful
     */
    public boolean cancelRegistration(int eventId, int studentId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE event_participants SET attendance_status = 'CANCELLED' " +
                "WHERE event_id = ? AND student_id = ?"
            );
            
            stmt.setInt(1, eventId);
            stmt.setInt(2, studentId);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error cancelling registration: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Records attendance for a student at an event
     * @param eventId Event ID
     * @param studentId Student ID
     * @return true if attendance recorded successfully
     */
    public boolean recordAttendance(int eventId, int studentId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "UPDATE event_participants SET attendance_status = 'ATTENDED' " +
                "WHERE event_id = ? AND student_id = ?"
            );
            
            stmt.setInt(1, eventId);
            stmt.setInt(2, studentId);
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Error recording attendance: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Gets all upcoming events
     * @return List of upcoming events
     */
    public List<Event> getUpcomingEvents() {
        List<Event> events = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT e.*, s.first_name || ' ' || s.last_name as organizer_name, " +
                "(SELECT COUNT(*) FROM event_participants ep WHERE ep.event_id = e.id AND ep.attendance_status != 'CANCELLED') as participant_count " +
                "FROM events e " +
                "LEFT JOIN students s ON e.organizer_id = s.id " +
                "WHERE e.start_date > ? " +
                "ORDER BY e.start_date ASC"
            );
            
            stmt.setString(1, LocalDateTime.now().toString());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                events.add(createEventFromResultSet(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting upcoming events: " + e.getMessage());
            e.printStackTrace();
        }
        
        return events;
    }
    
    /**
     * Gets all events registered by a student
     * @param studentId Student ID
     * @return List of events
     */
    public List<Event> getStudentEvents(int studentId) {
        List<Event> events = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT e.*, s.first_name || ' ' || s.last_name as organizer_name, " +
                "(SELECT COUNT(*) FROM event_participants ep WHERE ep.event_id = e.id AND ep.attendance_status != 'CANCELLED') as participant_count " +
                "FROM events e " +
                "LEFT JOIN students s ON e.organizer_id = s.id " +
                "JOIN event_participants ep ON e.id = ep.event_id " +
                "WHERE ep.student_id = ? AND ep.attendance_status != 'CANCELLED' " +
                "ORDER BY e.start_date ASC"
            );
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                events.add(createEventFromResultSet(rs));
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting student events: " + e.getMessage());
            e.printStackTrace();
        }
        
        return events;
    }
    
    /**
     * Gets event details
     * @param eventId Event ID
     * @return Event object or null if not found
     */
    public Event getEvent(int eventId) {
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT e.*, s.first_name || ' ' || s.last_name as organizer_name, " +
                "(SELECT COUNT(*) FROM event_participants ep WHERE ep.event_id = e.id AND ep.attendance_status != 'CANCELLED') as participant_count " +
                "FROM events e " +
                "LEFT JOIN students s ON e.organizer_id = s.id " +
                "WHERE e.id = ?"
            );
            
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Event event = createEventFromResultSet(rs);
                rs.close();
                stmt.close();
                return event;
            }
            
            rs.close();
            stmt.close();
            return null;
            
        } catch (SQLException e) {
            System.err.println("Error getting event: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Gets registered participants for an event
     * @param eventId Event ID
     * @return List of participants
     */
    public List<EventParticipant> getEventParticipants(int eventId) {
        List<EventParticipant> participants = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT ep.*, s.uid, s.first_name, s.last_name " +
                "FROM event_participants ep " +
                "JOIN students s ON ep.student_id = s.id " +
                "WHERE ep.event_id = ? AND ep.attendance_status != 'CANCELLED' " +
                "ORDER BY ep.registration_date ASC"
            );
            
            stmt.setInt(1, eventId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                EventParticipant participant = new EventParticipant(
                    rs.getInt("id"),
                    rs.getInt("event_id"),
                    rs.getInt("student_id"),
                    rs.getString("uid"),
                    rs.getString("first_name") + " " + rs.getString("last_name"),
                    LocalDateTime.parse(rs.getString("registration_date")),
                    rs.getString("attendance_status")
                );
                participants.add(participant);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting event participants: " + e.getMessage());
            e.printStackTrace();
        }
        
        return participants;
    }
    
    /**
     * Creates an Event object from a ResultSet
     */
    private Event createEventFromResultSet(ResultSet rs) throws SQLException {
        return new Event(
            rs.getInt("id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getString("event_type"),
            LocalDateTime.parse(rs.getString("start_date")),
            LocalDateTime.parse(rs.getString("end_date")),
            rs.getString("location"),
            rs.getInt("max_participants"),
            rs.getInt("organizer_id"),
            rs.getString("organizer_name"),
            rs.getInt("participant_count"),
            LocalDateTime.parse(rs.getString("created_at"))
        );
    }
    
    /**
     * Represents a library event
     */
    public static class Event {
        private int id;
        private String title;
        private String description;
        private String eventType;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String location;
        private int maxParticipants;
        private int organizerId;
        private String organizerName;
        private int participantCount;
        private LocalDateTime createdAt;
        
        public Event(int id, String title, String description, String eventType,
                    LocalDateTime startDate, LocalDateTime endDate, String location,
                    int maxParticipants, int organizerId, String organizerName,
                    int participantCount, LocalDateTime createdAt) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.eventType = eventType;
            this.startDate = startDate;
            this.endDate = endDate;
            this.location = location;
            this.maxParticipants = maxParticipants;
            this.organizerId = organizerId;
            this.organizerName = organizerName;
            this.participantCount = participantCount;
            this.createdAt = createdAt;
        }
        
        // Getters
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getEventType() { return eventType; }
        public LocalDateTime getStartDate() { return startDate; }
        public LocalDateTime getEndDate() { return endDate; }
        public String getLocation() { return location; }
        public int getMaxParticipants() { return maxParticipants; }
        public int getOrganizerId() { return organizerId; }
        public String getOrganizerName() { return organizerName; }
        public int getParticipantCount() { return participantCount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        
        public boolean isFull() {
            return maxParticipants > 0 && participantCount >= maxParticipants;
        }
        
        public String getFormattedStartDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
            return startDate.format(formatter);
        }
        
        public String getFormattedEndDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
            return endDate.format(formatter);
        }
        
        public boolean isUpcoming() {
            return startDate.isAfter(LocalDateTime.now());
        }
        
        @Override
        public String toString() {
            return title + " (" + eventType + ") - " + getFormattedStartDate();
        }
    }
    
    /**
     * Represents an event participant
     */
    public static class EventParticipant {
        private int id;
        private int eventId;
        private int studentId;
        private String studentUid;
        private String studentName;
        private LocalDateTime registrationDate;
        private String attendanceStatus;
        
        public EventParticipant(int id, int eventId, int studentId, String studentUid,
                              String studentName, LocalDateTime registrationDate,
                              String attendanceStatus) {
            this.id = id;
            this.eventId = eventId;
            this.studentId = studentId;
            this.studentUid = studentUid;
            this.studentName = studentName;
            this.registrationDate = registrationDate;
            this.attendanceStatus = attendanceStatus;
        }
        
        // Getters
        public int getId() { return id; }
        public int getEventId() { return eventId; }
        public int getStudentId() { return studentId; }
        public String getStudentUid() { return studentUid; }
        public String getStudentName() { return studentName; }
        public LocalDateTime getRegistrationDate() { return registrationDate; }
        public String getAttendanceStatus() { return attendanceStatus; }
        
        public String getFormattedRegistrationDate() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");
            return registrationDate.format(formatter);
        }
    }
} 