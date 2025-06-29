import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Handles student authentication in the library system
 */
public class StudentAuth {
    // Regular expression for student UID validation
    // Format: YYDEPTXXXXX where YY=year, DEPT=department code, XXXXX=unique ID
    private static final String UID_PATTERN = "^\\d{2}[a-zA-Z]{2,4}\\d{5}$";
    private static final Pattern pattern = Pattern.compile(UID_PATTERN);
    
    /**
     * Validates student UID format
     * @param uid The student UID to validate
     * @return true if format is valid, false otherwise
     */
    public static boolean validateUID(String uid) {
        if (uid == null || uid.trim().isEmpty()) {
            return false;
        }
        
        Matcher matcher = pattern.matcher(uid);
        return matcher.matches();
    }
    
    /**
     * Extracts the enrollment year from a valid UID
     * @param uid The student UID
     * @return The enrollment year as 20YY or null if invalid format
     */
    public static String getEnrollmentYear(String uid) {
        if (!validateUID(uid)) {
            return null;
        }
        
        String yearCode = uid.substring(0, 2);
        return "20" + yearCode; // Assuming 21st century
    }
    
    /**
     * Extracts the department code from a valid UID
     * @param uid The student UID
     * @return The department code or null if invalid format
     */
    public static String getDepartmentCode(String uid) {
        if (!validateUID(uid)) {
            return null;
        }
        
        // Find department code by extracting letters between digits
        Matcher m = Pattern.compile("\\d{2}([a-zA-Z]{2,4})\\d{5}").matcher(uid);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        
        return null;
    }
    
    /**
     * Formats the department code to a readable name
     * @param deptCode The department code
     * @return The readable department name
     */
    public static String getDepartmentName(String deptCode) {
        if (deptCode == null) {
            return "Unknown";
        }
        
        switch (deptCode.toUpperCase()) {
            case "BCS":
                return "B.E. Computer Science";
            case "BCE":
                return "B.E. Civil Engineering";
            case "BME":
                return "B.E. Mechanical Engineering";
            case "BEC":
                return "B.E. Electronics & Communication";
            case "BEE":
                return "B.E. Electrical Engineering";
            case "BAR":
                return "B.Arch";
            case "MCS":
                return "M.Tech Computer Science";
            case "MBA":
                return "Master of Business Administration";
            default:
                return deptCode;
        }
    }
} 