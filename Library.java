import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Library class manages the collection of books and provides operations
 * for adding, removing, searching, and managing book borrowing.
 */
public class Library {
    // List to store all books in the library
    private List<Book> books;
    private int loanPeriodDays = 14; // Default loan period is 14 days
    private DatabaseManager dbManager;
    private boolean useDatabase = false;

    /**
     * Constructor initializes the library and attempts to connect to the database
     */
    public Library() {
        this.books = new ArrayList<>();
        
        // Try to initialize database
        try {
            this.dbManager = new DatabaseManager();
            this.useDatabase = true;
            
            // Check if books table exists and has books
            if (dbManager.getBookCount() > 0) {
                // Load books from database
                this.books = dbManager.getAllBooks();
                System.out.println("Loaded " + books.size() + " books from database.");
            } else {
                System.out.println("Database is empty or books table doesn't exist. Using in-memory storage for now.");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize database. Falling back to in-memory storage.");
            e.printStackTrace();
            this.useDatabase = false;
        }
    }

    /**
     * Adds a new book to the library
     * @param book The book to be added
     */
    public void addBook(Book book) {
        if (useDatabase) {
            if (dbManager.addBook(book)) {
                books.add(book);
            }
        } else {
            books.add(book);
        }
    }

    /**
     * Removes a book from the library by its ISBN
     * @param isbn The ISBN of the book to remove
     * @return true if book was found and removed, false otherwise
     */
    public boolean removeBook(String isbn) {
        if (useDatabase) {
            if (dbManager.removeBook(isbn)) {
                return books.removeIf(book -> book.getIsbn().equals(isbn));
            }
            return false;
        } else {
            return books.removeIf(book -> book.getIsbn().equals(isbn));
        }
    }

    /**
     * Finds a book by its ISBN
     * @param isbn The ISBN to search for
     * @return The book if found, null otherwise
     */
    public Book findBook(String isbn) {
        return books.stream()
                .filter(book -> book.getIsbn().equals(isbn))
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns a copy of all books in the library
     * @return List of all books
     */
    public List<Book> getAllBooks() {
        return new ArrayList<>(books);
    }

    /**
     * Returns a list of all available books
     * @return List of available books
     */
    public List<Book> getAvailableBooks() {
        return books.stream()
                .filter(Book::isAvailable)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * Sets the loan period in days
     * @param days The number of days for the loan period
     */
    public void setLoanPeriodDays(int days) {
        if (days > 0) {
            this.loanPeriodDays = days;
        }
    }
    
    /**
     * Gets the current loan period in days
     * @return The loan period in days
     */
    public int getLoanPeriodDays() {
        return loanPeriodDays;
    }
    
    /**
     * Borrows a book to a student with the default loan period
     * @param isbn The ISBN of the book to borrow
     * @param borrowerName The name of the person borrowing the book
     * @param borrowerUID The UID of the student
     * @return true if book was successfully borrowed, false otherwise
     */
    public boolean borrowBook(String isbn, String borrowerName, String borrowerUID) {
        return borrowBook(isbn, borrowerName, borrowerUID, LocalDate.now().plusDays(loanPeriodDays));
    }
    
    /**
     * Borrows a book to a student with a specific due date
     * @param isbn The ISBN of the book to borrow
     * @param borrowerName The name of the person borrowing the book
     * @param borrowerUID The UID of the student
     * @param dueDate The due date for the book
     * @return true if book was successfully borrowed, false otherwise
     */
    public boolean borrowBook(String isbn, String borrowerName, String borrowerUID, LocalDate dueDate) {
        if (!StudentAuth.validateUID(borrowerUID)) {
            return false;
        }
        
        Book book = findBook(isbn);
        if (book != null && book.isAvailable()) {
            book.setAvailable(false);
            book.setBorrowerName(borrowerName);
            book.setDueDate(dueDate);
            
            if (useDatabase) {
                // Register or update student in database
                dbManager.registerStudent(borrowerUID, borrowerName);
                
                // Update book borrowing status
                dbManager.updateBookBorrowStatus(isbn, false, borrowerName, borrowerUID, dueDate);
            }
            
            return true;
        }
        return false;
    }

    /**
     * Legacy method for backward compatibility
     */
    public boolean borrowBook(String isbn, String borrowerName) {
        return borrowBook(isbn, borrowerName, LocalDate.now().plusDays(loanPeriodDays));
    }

    /**
     * Legacy method for backward compatibility
     */
    public boolean borrowBook(String isbn, String borrowerName, LocalDate dueDate) {
        Book book = findBook(isbn);
        if (book != null && book.isAvailable()) {
            book.setAvailable(false);
            book.setBorrowerName(borrowerName);
            book.setDueDate(dueDate);
            
            if (useDatabase) {
                dbManager.updateBookBorrowStatus(isbn, false, borrowerName, null, dueDate);
            }
            
            return true;
        }
        return false;
    }

    /**
     * Returns a borrowed book
     * @param isbn The ISBN of the book to return
     * @param borrowerUID The UID of the student returning the book
     * @return true if book was successfully returned, false otherwise
     */
    public boolean returnBook(String isbn, String borrowerUID) {
        Book book = findBook(isbn);
        if (book != null && !book.isAvailable()) {
            book.setAvailable(true);
            book.setBorrowerName(null);
            book.setDueDate(null);
            
            if (useDatabase) {
                dbManager.updateBookBorrowStatus(isbn, true, null, borrowerUID, null);
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public boolean returnBook(String isbn) {
        return returnBook(isbn, null);
    }

    /**
     * Searches for books by title (case-insensitive, partial match)
     * @param title The title to search for
     * @return List of matching books
     */
    public List<Book> searchByTitle(String title) {
        String searchTerm = title.toLowerCase();
        return books.stream()
                .filter(book -> book.getTitle().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
    }
    
    /**
     * Searches for books by author (case-insensitive, partial match)
     * @param author The author to search for
     * @return List of matching books
     */
    public List<Book> searchByAuthor(String author) {
        String searchTerm = author.toLowerCase();
        return books.stream()
                .filter(book -> book.getAuthor().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
    }
    
    /**
     * Searches for books by ISBN (case-insensitive, partial match)
     * @param isbn The ISBN to search for
     * @return List of matching books
     */
    public List<Book> searchByIsbn(String isbn) {
        String searchTerm = isbn.toLowerCase();
        return books.stream()
                .filter(book -> book.getIsbn().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
    }
    
    /**
     * Searches for books by borrower name (case-insensitive, partial match)
     * @param borrowerName The name of the borrower to search for
     * @return List of books borrowed by the specified person
     */
    public List<Book> searchByBorrower(String borrowerName) {
        String searchTerm = borrowerName.toLowerCase();
        return books.stream()
                .filter(book -> !book.isAvailable() && 
                        book.getBorrowerName().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets the total number of books in the library
     * @return Total number of books
     */
    public int getTotalBooks() {
        return books.size();
    }
    
    /**
     * Gets the number of available books in the library
     * @return Number of available books
     */
    public int getAvailableBooksCount() {
        return (int) books.stream().filter(Book::isAvailable).count();
    }

    /**
     * Searches for books by publication year
     * @param year The publication year to search for
     * @return List of books published in the specified year
     */
    public List<Book> searchByYear(int year) {
        return books.stream()
                .filter(book -> book.getPublicationYear() == year)
                .collect(Collectors.toList());
    }
    
    /**
     * Searches for books published after a specified year
     * @param year The starting year
     * @return List of books published after the specified year
     */
    public List<Book> searchByYearAfter(int year) {
        return books.stream()
                .filter(book -> book.getPublicationYear() > year && book.getPublicationYear() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Searches for books within a range of years
     * @param startYear The starting year (inclusive)
     * @param endYear The ending year (inclusive)
     * @return List of books published within the specified range
     */
    public List<Book> searchByYearRange(int startYear, int endYear) {
        return books.stream()
                .filter(book -> {
                    int year = book.getPublicationYear();
                    return year >= startYear && year <= endYear && year > 0;
                })
                .collect(Collectors.toList());
    }

    /**
     * Searches for books by category (case-insensitive, partial match)
     * @param category The category to search for
     * @return List of books in the specified category
     */
    public List<Book> searchByCategory(String category) {
        String searchTerm = category.toLowerCase();
        return books.stream()
                .filter(book -> book.getCategory().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
    }

    /**
     * Rates a book by ISBN
     * @param isbn The ISBN of the book to rate
     * @param rating The rating value (0-5)
     * @param raterUID The UID of the student rating the book
     * @return true if the book was successfully rated, false otherwise
     */
    public boolean rateBook(String isbn, double rating, String raterUID) {
        if (rating < 0 || rating > 5) {
            return false;
        }
        
        Book book = findBook(isbn);
        if (book != null) {
            book.addRating(rating);
            
            if (useDatabase) {
                return dbManager.updateBookRating(isbn, rating, raterUID);
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public boolean rateBook(String isbn, double rating) {
        return rateBook(isbn, rating, null);
    }

    /**
     * Searches for books with minimum rating
     * @param minRating The minimum rating to search for
     * @return List of books with rating greater than or equal to minRating
     */
    public List<Book> searchByMinRating(double minRating) {
        return books.stream()
                .filter(book -> book.getRating() >= minRating && book.getRatingCount() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Get top-rated books (with at least one rating)
     * @param limit The maximum number of books to return
     * @return List of top-rated books, sorted by rating (descending)
     */
    public List<Book> getTopRatedBooks(int limit) {
        return books.stream()
                .filter(book -> book.getRatingCount() > 0)
                .sorted((b1, b2) -> Double.compare(b2.getRating(), b1.getRating()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of overdue books
     * @return List of books that are currently overdue
     */
    public List<Book> getOverdueBooks() {
        return books.stream()
                .filter(Book::isOverdue)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets a list of books due within specified days
     * @param days Number of days from today
     * @return List of books due within the specified number of days
     */
    public List<Book> getBooksDueSoon(int days) {
        LocalDate cutoffDate = LocalDate.now().plusDays(days);
        return books.stream()
                .filter(book -> !book.isAvailable() && 
                        book.getDueDate() != null && 
                        !book.getDueDate().isAfter(cutoffDate) &&
                        !book.isOverdue())
                .collect(Collectors.toList());
    }

    /**
     * Rate a book in the library by title
     * @param title The title of the book to rate
     * @param rating The rating to give (0-5 scale)
     * @return true if book was found and rated, false otherwise
     */
    public boolean rateBookByTitle(String title, double rating) {
        Book book = findBookByTitle(title);
        if (book != null) {
            boolean success = book.addRating(rating);
            
            if (success && useDatabase) {
                dbManager.updateBookRating(book.getIsbn(), rating);
            }
            
            return success;
        }
        return false;
    }

    /**
     * Find a book by title
     * @param title The title of the book to find
     * @return The book if found, null otherwise
     */
    public Book findBookByTitle(String title) {
        for (Book book : books) {
            if (book.getTitle().equalsIgnoreCase(title)) {
                return book;
            }
        }
        return null;
    }

    /**
     * Borrows a book to a student by book title
     * @param title The title of the book to borrow
     * @param borrowerName The name of the person borrowing the book
     * @param borrowerUID The UID of the student
     * @return true if book was successfully borrowed, false otherwise
     */
    public boolean borrowBookByTitle(String title, String borrowerName, String borrowerUID) {
        // Find the book by title
        Book book = findBookByTitle(title);
        if (book == null) {
            return false;
        }
        // Borrow the book by ISBN
        return borrowBook(book.getIsbn(), borrowerName, borrowerUID);
    }

    /**
     * Gets a list of books borrowed by a specific student
     * @param uid The student UID
     * @return List of books borrowed by the student
     */
    public List<Book> getBooksBorrowedByStudent(String uid) {
        if (useDatabase) {
            try {
                return dbManager.getBooksBorrowedByStudent(uid);
            } catch (Exception e) {
                System.err.println("Error retrieving borrowed books from database: " + e.getMessage());
                // Fall back to in-memory check if database query fails
            }
        }
        
        // Use in-memory check if database is not available or query failed
        return books.stream()
                .filter(book -> !book.isAvailable() && book.getBorrowerName() != null)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a student with the given UID exists
     * @param uid The student UID to check
     * @return true if the UID is valid and exists, false otherwise
     */
    public boolean isValidStudent(String uid) {
        return StudentAuth.validateUID(uid);
    }

    /**
     * Closes the database connection when the library is no longer needed
     */
    public void close() {
        if (useDatabase && dbManager != null) {
            dbManager.closeConnection();
        }
    }
    
    /**
     * Gets the database manager instance
     * @return The database manager instance
     */
    public DatabaseManager getDBManager() {
        return dbManager;
    }
    
    /**
     * Gets the number of books in the database
     */
    public int getDatabaseBookCount() {
        if (useDatabase) {
            return dbManager.getBookCount();
        }
        return 0;
    }
} 