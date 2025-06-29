import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Book class represents a book in the library system.
 * It contains all the basic information about a book and its current status.
 */
public class Book {
    // Basic book information
    private String title;      // Title of the book
    private String author;     // Author of the book
    private String isbn;       // ISBN number (unique identifier)
    private boolean isAvailable;  // Whether the book is available for borrowing
    private String borrowerName;  // Name of the person who borrowed the book (null if available)
    private String borrowerUID;   // UID of the student who borrowed the book (null if available)
    private int publicationYear;  // Year the book was published
    private String category;      // Category/genre of the book
    private double rating;        // Rating of the book (0-5 scale)
    private int ratingCount;      // Number of ratings
    private LocalDate dueDate;    // Due date for borrowed books

    /**
     * Constructor to create a new book
     * @param title The title of the book
     * @param author The author of the book
     * @param isbn The ISBN number of the book
     */
    public Book(String title, String author, String isbn) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.isAvailable = true;    // New books are always available
        this.borrowerName = null;   // No borrower initially
        this.borrowerUID = null;    // No borrower UID initially
        this.publicationYear = 0;   // Default value (unknown)
        this.category = "Uncategorized";  // Default category
        this.rating = 0.0;          // No ratings initially
        this.ratingCount = 0;       // No ratings initially
    }
    
    /**
     * Constructor with publication year
     * @param title The title of the book
     * @param author The author of the book
     * @param isbn The ISBN number of the book
     * @param publicationYear The year the book was published
     */
    public Book(String title, String author, String isbn, int publicationYear) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.isAvailable = true;
        this.borrowerName = null;
        this.borrowerUID = null;
        this.publicationYear = publicationYear;
        this.category = "Uncategorized";
        this.rating = 0.0;
        this.ratingCount = 0;
    }
    
    /**
     * Full constructor with all fields
     * @param title The title of the book
     * @param author The author of the book
     * @param isbn The ISBN number of the book
     * @param publicationYear The year the book was published
     * @param category The category/genre of the book
     */
    public Book(String title, String author, String isbn, int publicationYear, String category) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.isAvailable = true;
        this.borrowerName = null;
        this.borrowerUID = null;
        this.publicationYear = publicationYear;
        this.category = category;
        this.rating = 0.0;
        this.ratingCount = 0;
    }

    /**
     * Constructor with title, author, isbn, category parameters
     * @param title The title of the book
     * @param author The author of the book
     * @param category The category/genre of the book
     * @param isbn The ISBN number of the book
     */
    public Book(String title, String author, String category, String isbn) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.category = category;
        this.isAvailable = true;
        this.borrowerName = null;
        this.borrowerUID = null;
        this.publicationYear = 2023; // Default to current year for new books
        this.rating = 0.0;
        this.ratingCount = 0;
    }

    // Getter methods to access private fields
    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }
    
    public String getBorrowerName() {
        return borrowerName;
    }
    
    public void setBorrowerName(String name) {
        this.borrowerName = name;
    }
    
    /**
     * Gets the borrower UID for the book
     * @return The UID of the student who borrowed the book, or null if not borrowed
     */
    public String getBorrowerUID() {
        return borrowerUID;
    }
    
    /**
     * Sets the borrower UID for the book
     * @param uid The UID of the student who is borrowing the book
     */
    public void setBorrowerUID(String uid) {
        this.borrowerUID = uid;
    }
    
    public int getPublicationYear() {
        return publicationYear;
    }
    
    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Adds a rating to the book
     * @param newRating The rating to add (0-5 scale)
     * @return true if rating was added successfully, false otherwise
     */
    public boolean addRating(double newRating) {
        if (newRating < 0 || newRating > 5) {
            return false;
        }
        
        double totalRating = (rating * ratingCount) + newRating;
        ratingCount++;
        rating = totalRating / ratingCount;
        return true;
    }
    
    /**
     * Gets the current rating of the book
     * @return The rating (0-5 scale)
     */
    public double getRating() {
        return rating;
    }
    
    /**
     * Gets the number of ratings for the book
     * @return The number of ratings
     */
    public int getRatingCount() {
        return ratingCount;
    }

    /**
     * Gets the due date for the book
     * @return The due date or null if not borrowed
     */
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    /**
     * Sets the due date for the book
     * @param dueDate The due date to set
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    /**
     * Checks if the book is overdue
     * @return true if the book is overdue, false otherwise
     */
    public boolean isOverdue() {
        return !isAvailable && dueDate != null && LocalDate.now().isAfter(dueDate);
    }

    /**
     * Override toString method to provide a formatted string representation of the book
     * @return String containing book details and current status
     */
    @Override
    public String toString() {
        // Create status string based on availability
        String status;
        if (isAvailable) {
            status = "Available";
        } else {
            status = "Borrowed by: " + borrowerName;
            if (dueDate != null) {
                String formattedDate = dueDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
                status += " (Due: " + formattedDate + ")";
                if (isOverdue()) {
                    status += " - OVERDUE";
                }
            }
        }
        
        String yearInfo = publicationYear > 0 ? ", Year: " + publicationYear : "";
        String ratingInfo = ratingCount > 0 ? String.format(", Rating: %.1f/5.0 (%d ratings)", rating, ratingCount) : ", No ratings yet";
        
        return "Title: " + title + ", Author: " + author + yearInfo + 
               ", Category: " + category + ratingInfo + ", ISBN: " + isbn + 
               ", Status: " + status;
    }
} 