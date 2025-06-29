import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages gamification features like achievements, points, and badges
 */
public class GamificationSystem {
    private Connection connection;
    
    // Achievement types
    public static final String ACHIEVEMENT_BORROW = "BORROW";
    public static final String ACHIEVEMENT_RETURN = "RETURN";
    public static final String ACHIEVEMENT_REVIEW = "REVIEW";
    public static final String ACHIEVEMENT_READING_LIST = "READING_LIST";
    public static final String ACHIEVEMENT_DISCUSSION = "DISCUSSION";
    public static final String ACHIEVEMENT_ONTIME = "ONTIME";
    
    /**
     * Creates a new GamificationSystem with database connection
     */
    public GamificationSystem(Connection connection) {
        this.connection = connection;
        createGamificationTables();
        initializeAchievements();
    }
    
    /**
     * Creates necessary tables for gamification
     */
    private void createGamificationTables() {
        try {
            Statement stmt = connection.createStatement();
            
            // Create achievements table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS achievements (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "type TEXT NOT NULL, " + // Type of activity: BORROW, RETURN, REVIEW, etc.
                "threshold INTEGER NOT NULL, " + // How many actions needed to earn
                "points INTEGER NOT NULL, " + // Points awarded
                "badge_icon TEXT, " + // Icon filename
                "badge_color TEXT" + // Badge color
                ")"
            );
            
            // Create student achievements table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS student_achievements (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "achievement_id INTEGER NOT NULL, " +
                "date_earned TEXT NOT NULL, " +
                "FOREIGN KEY (student_id) REFERENCES students(id), " +
                "FOREIGN KEY (achievement_id) REFERENCES achievements(id), " +
                "UNIQUE(student_id, achievement_id)" +
                ")"
            );
            
            // Create student points table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS student_points (" +
                "student_id INTEGER PRIMARY KEY, " +
                "total_points INTEGER DEFAULT 0, " +
                "level INTEGER DEFAULT 1, " +
                "FOREIGN KEY (student_id) REFERENCES students(id)" +
                ")"
            );
            
            // Create point transactions table
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS point_transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "student_id INTEGER NOT NULL, " +
                "points INTEGER NOT NULL, " + // Can be positive or negative
                "reason TEXT NOT NULL, " +
                "transaction_date TEXT NOT NULL, " +
                "FOREIGN KEY (student_id) REFERENCES students(id)" +
                ")"
            );
            
            stmt.close();
        } catch (SQLException e) {
            System.err.println("Error creating gamification tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize default achievements if not already present
     */
    private void initializeAchievements() {
        try {
            Statement checkStmt = connection.createStatement();
            ResultSet rs = checkStmt.executeQuery("SELECT COUNT(*) as count FROM achievements");
            
            if (rs.next() && rs.getInt("count") == 0) {
                // No achievements yet, add default ones
                PreparedStatement stmt = connection.prepareStatement(
                    "INSERT INTO achievements (name, description, type, threshold, points, badge_icon, badge_color) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)"
                );
                
                // Borrowing achievements
                addAchievement(stmt, "First Book", "Borrow your first book", ACHIEVEMENT_BORROW, 1, 10, "book.png", "bronze");
                addAchievement(stmt, "Bookworm", "Borrow 10 books", ACHIEVEMENT_BORROW, 10, 50, "bookworm.png", "silver");
                addAchievement(stmt, "Bibliophile", "Borrow 50 books", ACHIEVEMENT_BORROW, 50, 200, "bibliophile.png", "gold");
                
                // Return achievements
                addAchievement(stmt, "Responsible Reader", "Return 5 books on time", ACHIEVEMENT_ONTIME, 5, 25, "clock.png", "bronze");
                addAchievement(stmt, "Punctual Patron", "Return 20 books on time", ACHIEVEMENT_ONTIME, 20, 100, "calendar.png", "silver");
                
                // Review achievements
                addAchievement(stmt, "Critic", "Write your first book review", ACHIEVEMENT_REVIEW, 1, 15, "pen.png", "bronze");
                addAchievement(stmt, "Literary Critic", "Write 10 book reviews", ACHIEVEMENT_REVIEW, 10, 75, "star.png", "silver");
                
                // Reading list achievements
                addAchievement(stmt, "Collector", "Create your first reading list", ACHIEVEMENT_READING_LIST, 1, 10, "list.png", "bronze");
                addAchievement(stmt, "Curator", "Add 20 books to reading lists", ACHIEVEMENT_READING_LIST, 20, 50, "collection.png", "silver");
                
                // Discussion achievements
                addAchievement(stmt, "Commentator", "Participate in a book discussion", ACHIEVEMENT_DISCUSSION, 1, 10, "comment.png", "bronze");
                addAchievement(stmt, "Thought Leader", "Start 5 book discussions", ACHIEVEMENT_DISCUSSION, 5, 50, "chat.png", "silver");
                
                stmt.close();
                System.out.println("Default achievements initialized.");
            }
            
            rs.close();
            checkStmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error initializing achievements: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Helper method to add an achievement
     */
    private void addAchievement(PreparedStatement stmt, String name, String description, 
                              String type, int threshold, int points, String icon, String color) 
                              throws SQLException {
        stmt.setString(1, name);
        stmt.setString(2, description);
        stmt.setString(3, type);
        stmt.setInt(4, threshold);
        stmt.setInt(5, points);
        stmt.setString(6, icon);
        stmt.setString(7, color);
        stmt.executeUpdate();
    }
    
    /**
     * Tracks an activity for a student and awards achievements if thresholds are met
     * @param studentId Student ID
     * @param activityType Type of activity
     * @return List of newly earned achievements
     */
    public List<Achievement> trackActivity(int studentId, String activityType) {
        List<Achievement> newAchievements = new ArrayList<>();
        
        try {
            // Create or update student points record
            ensureStudentPointsRecord(studentId);
            
            // Count student activities of this type
            PreparedStatement countStmt = connection.prepareStatement(
                "SELECT COUNT(*) as count FROM point_transactions " +
                "WHERE student_id = ? AND reason LIKE ?"
            );
            
            countStmt.setInt(1, studentId);
            countStmt.setString(2, "%" + activityType + "%");
            ResultSet countRs = countStmt.executeQuery();
            
            int activityCount = 0;
            if (countRs.next()) {
                activityCount = countRs.getInt("count") + 1; // Add 1 for current activity
            }
            countRs.close();
            countStmt.close();
            
            // Add points transaction for this activity
            int basePoints = getBasePointsForActivity(activityType);
            addPointsTransaction(studentId, basePoints, "Activity: " + activityType);
            
            // Check for achievements
            PreparedStatement achievementStmt = connection.prepareStatement(
                "SELECT * FROM achievements " +
                "WHERE type = ? AND threshold <= ? AND id NOT IN " +
                "(SELECT achievement_id FROM student_achievements WHERE student_id = ?)"
            );
            
            achievementStmt.setString(1, activityType);
            achievementStmt.setInt(2, activityCount);
            achievementStmt.setInt(3, studentId);
            
            ResultSet achievementRs = achievementStmt.executeQuery();
            
            while (achievementRs.next()) {
                // Award achievement
                int achievementId = achievementRs.getInt("id");
                String name = achievementRs.getString("name");
                String description = achievementRs.getString("description");
                int points = achievementRs.getInt("points");
                String icon = achievementRs.getString("badge_icon");
                String color = achievementRs.getString("badge_color");
                
                // Add to student achievements
                PreparedStatement awardStmt = connection.prepareStatement(
                    "INSERT INTO student_achievements (student_id, achievement_id, date_earned) " +
                    "VALUES (?, ?, ?)"
                );
                
                awardStmt.setInt(1, studentId);
                awardStmt.setInt(2, achievementId);
                awardStmt.setString(3, LocalDateTime.now().toString());
                awardStmt.executeUpdate();
                awardStmt.close();
                
                // Add points for achievement
                addPointsTransaction(studentId, points, "Achievement: " + name);
                
                // Add to result list
                Achievement achievement = new Achievement(
                    achievementId, name, description, activityType, 
                    achievementRs.getInt("threshold"), points, icon, color
                );
                newAchievements.add(achievement);
            }
            
            achievementRs.close();
            achievementStmt.close();
            
            // Update student level
            updateStudentLevel(studentId);
            
        } catch (SQLException e) {
            System.err.println("Error tracking activity: " + e.getMessage());
            e.printStackTrace();
        }
        
        return newAchievements;
    }
    
    /**
     * Gets base points for different activity types
     */
    private int getBasePointsForActivity(String activityType) {
        switch (activityType) {
            case ACHIEVEMENT_BORROW: return 5;
            case ACHIEVEMENT_RETURN: return 3;
            case ACHIEVEMENT_ONTIME: return 8;
            case ACHIEVEMENT_REVIEW: return 10;
            case ACHIEVEMENT_READING_LIST: return 5;
            case ACHIEVEMENT_DISCUSSION: return 7;
            default: return 1;
        }
    }
    
    /**
     * Ensures a student has a points record
     */
    private void ensureStudentPointsRecord(int studentId) throws SQLException {
        PreparedStatement checkStmt = connection.prepareStatement(
            "SELECT student_id FROM student_points WHERE student_id = ?"
        );
        
        checkStmt.setInt(1, studentId);
        ResultSet rs = checkStmt.executeQuery();
        
        if (!rs.next()) {
            // Create record
            PreparedStatement insertStmt = connection.prepareStatement(
                "INSERT INTO student_points (student_id, total_points, level) VALUES (?, 0, 1)"
            );
            
            insertStmt.setInt(1, studentId);
            insertStmt.executeUpdate();
            insertStmt.close();
        }
        
        rs.close();
        checkStmt.close();
    }
    
    /**
     * Adds a points transaction for a student
     */
    private void addPointsTransaction(int studentId, int points, String reason) throws SQLException {
        // Add transaction
        PreparedStatement transStmt = connection.prepareStatement(
            "INSERT INTO point_transactions (student_id, points, reason, transaction_date) " +
            "VALUES (?, ?, ?, ?)"
        );
        
        transStmt.setInt(1, studentId);
        transStmt.setInt(2, points);
        transStmt.setString(3, reason);
        transStmt.setString(4, LocalDateTime.now().toString());
        transStmt.executeUpdate();
        transStmt.close();
        
        // Update total points
        PreparedStatement updateStmt = connection.prepareStatement(
            "UPDATE student_points SET total_points = total_points + ? WHERE student_id = ?"
        );
        
        updateStmt.setInt(1, points);
        updateStmt.setInt(2, studentId);
        updateStmt.executeUpdate();
        updateStmt.close();
    }
    
    /**
     * Updates a student's level based on total points
     */
    private void updateStudentLevel(int studentId) throws SQLException {
        PreparedStatement pointsStmt = connection.prepareStatement(
            "SELECT total_points FROM student_points WHERE student_id = ?"
        );
        
        pointsStmt.setInt(1, studentId);
        ResultSet rs = pointsStmt.executeQuery();
        
        if (rs.next()) {
            int totalPoints = rs.getInt("total_points");
            int newLevel = calculateLevel(totalPoints);
            
            PreparedStatement updateStmt = connection.prepareStatement(
                "UPDATE student_points SET level = ? WHERE student_id = ?"
            );
            
            updateStmt.setInt(1, newLevel);
            updateStmt.setInt(2, studentId);
            updateStmt.executeUpdate();
            updateStmt.close();
        }
        
        rs.close();
        pointsStmt.close();
    }
    
    /**
     * Calculates level based on points
     */
    private int calculateLevel(int points) {
        // Level formula: 1 + sqrt(points / 100)
        return 1 + (int)Math.sqrt(points / 100.0);
    }
    
    /**
     * Gets a student's total points and level
     * @param studentId Student ID
     * @return Map with points and level information
     */
    public Map<String, Object> getStudentProgress(int studentId) {
        Map<String, Object> progress = new HashMap<>();
        progress.put("points", 0);
        progress.put("level", 1);
        progress.put("nextLevelPoints", 100);
        
        try {
            ensureStudentPointsRecord(studentId);
            
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT total_points, level FROM student_points WHERE student_id = ?"
            );
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                int points = rs.getInt("total_points");
                int level = rs.getInt("level");
                int pointsForNextLevel = (level * level + 1) * 100;
                
                progress.put("points", points);
                progress.put("level", level);
                progress.put("nextLevelPoints", pointsForNextLevel);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting student progress: " + e.getMessage());
            e.printStackTrace();
        }
        
        return progress;
    }
    
    /**
     * Gets all achievements earned by a student
     * @param studentId Student ID
     * @return List of earned achievements
     */
    public List<Achievement> getStudentAchievements(int studentId) {
        List<Achievement> achievements = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT a.*, sa.date_earned FROM achievements a " +
                "JOIN student_achievements sa ON a.id = sa.achievement_id " +
                "WHERE sa.student_id = ? " +
                "ORDER BY sa.date_earned DESC"
            );
            
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Achievement achievement = new Achievement(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("type"),
                    rs.getInt("threshold"),
                    rs.getInt("points"),
                    rs.getString("badge_icon"),
                    rs.getString("badge_color"),
                    LocalDateTime.parse(rs.getString("date_earned"))
                );
                achievements.add(achievement);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting student achievements: " + e.getMessage());
            e.printStackTrace();
        }
        
        return achievements;
    }
    
    /**
     * Gets leaderboard of top students by points
     * @param limit Maximum number of students to return
     * @return List of students with their points and level
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        List<Map<String, Object>> leaderboard = new ArrayList<>();
        
        try {
            PreparedStatement stmt = connection.prepareStatement(
                "SELECT sp.student_id, sp.total_points, sp.level, " +
                "s.first_name || ' ' || s.last_name as student_name, s.uid " +
                "FROM student_points sp " +
                "JOIN students s ON sp.student_id = s.id " +
                "ORDER BY sp.total_points DESC LIMIT ?"
            );
            
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("studentId", rs.getInt("student_id"));
                entry.put("studentName", rs.getString("student_name"));
                entry.put("studentUid", rs.getString("uid"));
                entry.put("points", rs.getInt("total_points"));
                entry.put("level", rs.getInt("level"));
                leaderboard.add(entry);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            System.err.println("Error getting leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
        
        return leaderboard;
    }
    
    /**
     * Represents an achievement
     */
    public static class Achievement {
        private int id;
        private String name;
        private String description;
        private String type;
        private int threshold;
        private int points;
        private String badgeIcon;
        private String badgeColor;
        private LocalDateTime dateEarned;
        
        public Achievement(int id, String name, String description, String type,
                         int threshold, int points, String badgeIcon, String badgeColor) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.threshold = threshold;
            this.points = points;
            this.badgeIcon = badgeIcon;
            this.badgeColor = badgeColor;
            this.dateEarned = null;
        }
        
        public Achievement(int id, String name, String description, String type,
                         int threshold, int points, String badgeIcon, String badgeColor,
                         LocalDateTime dateEarned) {
            this(id, name, description, type, threshold, points, badgeIcon, badgeColor);
            this.dateEarned = dateEarned;
        }
        
        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getType() { return type; }
        public int getThreshold() { return threshold; }
        public int getPoints() { return points; }
        public String getBadgeIcon() { return badgeIcon; }
        public String getBadgeColor() { return badgeColor; }
        public LocalDateTime getDateEarned() { return dateEarned; }
        public boolean isEarned() { return dateEarned != null; }
        
        @Override
        public String toString() {
            return name + " (" + points + " points) - " + description;
        }
    }
} 