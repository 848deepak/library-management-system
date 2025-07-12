import java.util.*;

/**
 * Engine that provides book recommendations based on student interests.
 */
public class RecommendationEngine {
    private DatabaseManager dbManager;
    private Map<String, StudentInterest> studentInterests;
    
    /**
     * Constructor initializes the recommendation engine with a database manager.
     * 
     * @param dbManager The database manager to use for book data
     */
    public RecommendationEngine(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.studentInterests = new HashMap<>();
    }
    
    /**
     * Gets or creates the interest profile for a student.
     * 
     * @param studentUID The student's UID
     * @return The student's interest profile
     */
    public StudentInterest getStudentInterest(String studentUID) {
        if (!studentInterests.containsKey(studentUID)) {
            studentInterests.put(studentUID, new StudentInterest(studentUID));
        }
        return studentInterests.get(studentUID);
    }
    
    /**
     * Updates a student's interest based on borrowing activity.
     * 
     * @param studentUID The student's UID
     * @param book The book being borrowed
     */
    public void updateInterestFromBorrowing(String studentUID, Book book) {
        StudentInterest interest = getStudentInterest(studentUID);
        String category = book.getCategory();
        
        // Increase interest in this category
        int currentInterest = interest.getInterest(category);
        interest.setInterest(category, Math.min(currentInterest + 1, 5));
    }
    
    /**
     * Updates a student's interest based on rating activity.
     * 
     * @param studentUID The student's UID
     * @param book The book being rated
     * @param rating The rating given (1-5)
     */
    public void updateInterestFromRating(String studentUID, Book book, int rating) {
        StudentInterest interest = getStudentInterest(studentUID);
        String category = book.getCategory();
        
        // If highly rated (4-5), increase interest
        if (rating >= 4) {
            int currentInterest = interest.getInterest(category);
            interest.setInterest(category, Math.min(currentInterest + 1, 5));
        }
        // If poorly rated (1-2), decrease interest
        else if (rating <= 2) {
            int currentInterest = interest.getInterest(category);
            interest.setInterest(category, Math.max(currentInterest - 1, 1));
        }
    }
    
    /**
     * Gets recommended books for a student based on their interests.
     * 
     * @param studentUID The student's UID
     * @param limit Maximum number of recommendations to return
     * @return List of recommended books
     */
    public List<Book> getRecommendedBooks(String studentUID, int limit) {
        StudentInterest interest = getStudentInterest(studentUID);
        
        if (!interest.hasInterests()) {
            // If no interests recorded, return highest rated books
            return getTopRatedBooks(limit);
        }
        
        // Get all available books
        List<Book> allBooks = dbManager.getAllBooks();
        
        // Calculate a score for each book based on interest matching and rating
        Map<Book, Double> bookScores = new HashMap<>();
        
        for (Book book : allBooks) {
            // Skip already borrowed books by this student
            if (book.getStatus().equals("Borrowed") && 
                dbManager.getBorrowerUID(book.getISBN()).equals(studentUID)) {
                continue;
            }
            
            double score = calculateRecommendationScore(book, interest);
            bookScores.put(book, score);
        }
        
        // Sort books by score
        List<Book> sortedBooks = new ArrayList<>(bookScores.keySet());
        sortedBooks.sort((b1, b2) -> Double.compare(bookScores.get(b2), bookScores.get(b1)));
        
        // Return top N books
        return sortedBooks.subList(0, Math.min(limit, sortedBooks.size()));
    }
    
    /**
     * Calculate a recommendation score for a book based on student interests.
     * 
     * @param book The book to score
     * @param interest The student's interest profile
     * @return A recommendation score
     */
    private double calculateRecommendationScore(Book book, StudentInterest interest) {
        String category = book.getCategory();
        int interestLevel = interest.getInterest(category);
        double rating = book.getRating();
        
        // Combine interest level (0-5) and rating (0-5) with interest weighted higher
        return (interestLevel * 0.7) + (rating * 0.3);
    }
    
    /**
     * Gets the top rated available books.
     * 
     * @param limit Maximum number of books to return
     * @return List of top rated books
     */
    private List<Book> getTopRatedBooks(int limit) {
        List<Book> allBooks = dbManager.getAllBooks();
        
        // Filter to only available books
        List<Book> availableBooks = new ArrayList<>();
        for (Book book : allBooks) {
            if (book.getStatus().equals("Available")) {
                availableBooks.add(book);
            }
        }
        
        // Sort by rating
        availableBooks.sort((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()));
        
        // Return top N books
        return availableBooks.subList(0, Math.min(limit, availableBooks.size()));
    }
} 