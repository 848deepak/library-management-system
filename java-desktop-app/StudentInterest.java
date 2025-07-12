import java.util.HashMap;
import java.util.Map;

/**
 * Represents a student's interests in different book categories.
 * Used for generating personalized book recommendations.
 */
public class StudentInterest {
    private String studentUID;
    private Map<String, Integer> categoryInterests;
    
    /**
     * Constructor creates a new StudentInterest object for a student.
     * 
     * @param studentUID The unique ID of the student
     */
    public StudentInterest(String studentUID) {
        this.studentUID = studentUID;
        this.categoryInterests = new HashMap<>();
    }
    
    /**
     * Gets the student's UID.
     * 
     * @return The student's UID
     */
    public String getStudentUID() {
        return studentUID;
    }
    
    /**
     * Sets the interest level for a specific category.
     * 
     * @param category The book category
     * @param interestLevel The interest level (1-5)
     */
    public void setInterest(String category, int interestLevel) {
        if (interestLevel < 1) interestLevel = 1;
        if (interestLevel > 5) interestLevel = 5;
        categoryInterests.put(category, interestLevel);
    }
    
    /**
     * Gets the interest level for a specific category.
     * 
     * @param category The book category
     * @return The interest level (1-5), or 0 if not set
     */
    public int getInterest(String category) {
        return categoryInterests.getOrDefault(category, 0);
    }
    
    /**
     * Gets all category interests.
     * 
     * @return Map of category to interest level
     */
    public Map<String, Integer> getAllInterests() {
        return new HashMap<>(categoryInterests);
    }
    
    /**
     * Checks if there are any interests set.
     * 
     * @return true if there are interests, false otherwise
     */
    public boolean hasInterests() {
        return !categoryInterests.isEmpty();
    }
    
    /**
     * Gets the most interested category (highest interest level).
     * 
     * @return The most interested category, or null if no interests
     */
    public String getMostInterestedCategory() {
        if (categoryInterests.isEmpty()) {
            return null;
        }
        
        String bestCategory = null;
        int maxInterest = 0;
        
        for (Map.Entry<String, Integer> entry : categoryInterests.entrySet()) {
            if (entry.getValue() > maxInterest) {
                maxInterest = entry.getValue();
                bestCategory = entry.getKey();
            }
        }
        
        return bestCategory;
    }
} 