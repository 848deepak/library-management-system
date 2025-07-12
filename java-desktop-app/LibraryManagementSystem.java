import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.io.File;
import java.sql.ResultSet;
import java.util.Map;

/**
 * LibraryManagementSystem is the main GUI application class that provides
 * the user interface for both admin and student operations.
 */
public class LibraryManagementSystem extends JFrame {
    // Core components
    private Library library;                    // The library instance that manages books
    private JPanel mainPanel;                   // Main panel containing all views
    private CardLayout cardLayout;              // Layout manager for switching between views
    
    // Admin panel components
    private JTable adminBookTable;              // Table to display books in admin view
    private JTextField titleField;              // Field for book title
    private JTextField authorField;             // Field for book author
    private JTextField isbnField;               // Field for book ISBN
    private JTextField yearField;               // Field for publication year
    private JTextField categoryField;           // Field for book category
    private JTextField adminSearchField;        // Search field for admin
    private JComboBox<String> adminSearchTypeCombo;  // Search type selector for admin
    private JLabel adminStatsLabel;             // Statistics label for admin view
    
    // Student panel components
    private JTable studentBookTable;            // Table to display books in student view
    private JTextField studentSearchField;      // Search field for student
    private JComboBox<String> studentSearchTypeCombo;  // Search type selector for student
    private JLabel studentStatsLabel;           // Statistics label for student view
    private JLabel studentInfoLabel;            // Label to display student information
    private String currentStudentName;          // Name of the currently logged in student
    private String currentStudentUID;           // UID of the currently logged in student

    /**
     * Constructor initializes the library system and sets up the GUI
     */
    public LibraryManagementSystem() {
        library = new Library();
        
        // Check if the database is empty and initialize if needed
        int bookCount = library.getDatabaseBookCount();
        if (bookCount == 0) {
            System.out.println("Database is empty. Populating with sample books...");
            // Delete existing database file to ensure fresh start
            try {
                library.close();
                File dbFile = new File("library.db");
                if (dbFile.exists()) {
                    if (dbFile.delete()) {
                        System.out.println("Empty database file deleted.");
                    }
                }
                // Use DatabasePopulator to populate the database with 150+ books
                DatabasePopulator.populateDatabase();
                
                // Recreate library object to connect to the newly populated database
                library = new Library();
                System.out.println("Database has been populated with a large collection of books.");
            } catch (Exception e) {
                System.err.println("Error populating database: " + e.getMessage());
            }
        } else {
            System.out.println("Database contains " + bookCount + " books.");
        }
        
        setTitle("Library Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        // Initialize layout and main panel
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create and add different views
        mainPanel.add(createAdminPanel(), "ADMIN");
        mainPanel.add(createStudentPanel(), "STUDENT");
        mainPanel.add(createLoginPanel(), "LOGIN");

        add(mainPanel);
        cardLayout.show(mainPanel, "LOGIN");
        
        // Load admin view with all books in alphabetical order
        loadBooksInAlphabeticalOrder();
        
        // Add window listener to close database connection when app exits
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (library != null) {
                    library.close();
                }
            }
        });
    }
    
    /**
     * Loads all books in alphabetical order by title (admin view only)
     */
    private void loadBooksInAlphabeticalOrder() {
        List<Book> allBooks = library.getAllBooks();
        // Sort books by title alphabetically
        allBooks.sort(Comparator.comparing(Book::getTitle));
        
        // Update only admin table
        BookTableView.updateBookTable(allBooks, adminBookTable);
        
        // Update admin stats
        updateStats(adminStatsLabel);
    }

    /**
     * Creates the login panel with admin and student options
     */
    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Create login buttons
        JButton adminButton = new JButton("Admin Login");
        JButton studentButton = new JButton("Student Login");

        // Add action listeners
        adminButton.addActionListener(e -> {
            // Refresh books and stats before showing admin panel
            loadBooksInAlphabeticalOrder();
            updateStats(adminStatsLabel);
            cardLayout.show(mainPanel, "ADMIN");
        });
        studentButton.addActionListener(e -> showStudentLogin());

        // Add buttons to panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(adminButton, gbc);

        gbc.gridy = 1;
        panel.add(studentButton, gbc);

        return panel;
    }

    /**
     * Shows the student login dialog and processes the login
     */
    private void showStudentLogin() {
        StudentLoginDialog loginDialog = new StudentLoginDialog(this);
        if (loginDialog.showDialog()) {
            // Login successful
            currentStudentName = loginDialog.getStudentName();
            currentStudentUID = loginDialog.getStudentUID();
            
            // Update the student info label
            String deptName = StudentAuth.getDepartmentName(StudentAuth.getDepartmentCode(currentStudentUID));
            String enrollYear = StudentAuth.getEnrollmentYear(currentStudentUID);
            studentInfoLabel.setText("Logged in: " + currentStudentName + 
                                     " | UID: " + currentStudentUID + 
                                     " | Dept: " + deptName + 
                                     " | Enrolled: " + enrollYear);
            
            // Show the student panel
            cardLayout.show(mainPanel, "STUDENT");
            
            // First check if student has any borrowed books
            List<Book> borrowedBooks = library.getBooksBorrowedByStudent(currentStudentUID);
            if (!borrowedBooks.isEmpty()) {
                // Show the student's borrowed books
                BookTableView.updateBookTable(borrowedBooks, studentBookTable);
                JOptionPane.showMessageDialog(this, 
                    "Welcome back! You have " + borrowedBooks.size() + " book(s) currently borrowed.");
            } else {
                // No borrowed books, show available books
                viewAvailableBooks(studentBookTable);
            }
        }
    }

    /**
     * Creates the admin panel with book management features
     */
    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create form panel for adding books
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        titleField = new JTextField();
        authorField = new JTextField();
        isbnField = new JTextField();
        yearField = new JTextField();
        categoryField = new JTextField();

        // Add form fields
        formPanel.add(new JLabel("Title:"));
        formPanel.add(titleField);
        formPanel.add(new JLabel("Author:"));
        formPanel.add(authorField);
        formPanel.add(new JLabel("ISBN:"));
        formPanel.add(isbnField);
        formPanel.add(new JLabel("Publication Year:"));
        formPanel.add(yearField);
        formPanel.add(new JLabel("Category:"));
        formPanel.add(categoryField);

        // Add book button
        JButton addButton = new JButton("Add Book");
        addButton.addActionListener(e -> addBook());
        formPanel.add(addButton);

        // Create search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        adminSearchField = new JTextField(20);
        adminSearchTypeCombo = new JComboBox<>(new String[]{"Title", "Author", "ISBN", "Borrower", "Year", "Category"});
        JButton searchButton = new JButton("Search");
        
        searchButton.addActionListener(e -> searchBooks(adminSearchField, adminSearchTypeCombo, adminBookTable));
        
        // Add search components
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(adminSearchField);
        searchPanel.add(new JLabel("By: "));
        searchPanel.add(adminSearchTypeCombo);
        searchPanel.add(searchButton);
        
        // Create stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        adminStatsLabel = new JLabel("Total Books: 0 | Available Books: 0");
        updateStats(adminStatsLabel);
        statsPanel.add(adminStatsLabel);

        // Create book table
        adminBookTable = BookTableView.createBookTable();
        JScrollPane scrollPane = new JScrollPane(adminBookTable);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton viewButton = new JButton("View All Books");
        JButton removeButton = new JButton("Remove Book");
        JButton overdueButton = new JButton("View Overdue Books");
        JButton dueSoonButton = new JButton("View Books Due Soon");
        JButton backButton = new JButton("Back to Login");

        // Add button listeners
        viewButton.addActionListener(e -> viewAllBooks(adminBookTable));
        removeButton.addActionListener(e -> removeBook());
        overdueButton.addActionListener(e -> viewOverdueBooks(adminBookTable));
        dueSoonButton.addActionListener(e -> viewBooksDueSoon());
        backButton.addActionListener(e -> cardLayout.show(mainPanel, "LOGIN"));

        // Add buttons to panel
        buttonPanel.add(viewButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(overdueButton);
        buttonPanel.add(dueSoonButton);
        buttonPanel.add(backButton);

        // Organize panels
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(statsPanel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the student panel with borrowing features
     */
    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create student info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        studentInfoLabel = new JLabel("Not logged in");
        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> {
            currentStudentName = null;
            currentStudentUID = null;
            cardLayout.show(mainPanel, "LOGIN");
        });
        infoPanel.add(studentInfoLabel, BorderLayout.CENTER);
        infoPanel.add(logoutButton, BorderLayout.EAST);

        // Create search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        studentSearchField = new JTextField(20);
        studentSearchTypeCombo = new JComboBox<>(new String[]{"Title", "Author", "ISBN", "Year", "Category", "Min Rating"});
        JButton searchButton = new JButton("Search");
        
        searchButton.addActionListener(e -> searchBooks(studentSearchField, studentSearchTypeCombo, studentBookTable));
        
        // Add search components
        searchPanel.add(new JLabel("Search: "));
        searchPanel.add(studentSearchField);
        searchPanel.add(new JLabel("By: "));
        searchPanel.add(studentSearchTypeCombo);
        searchPanel.add(searchButton);
        
        // Create stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        studentStatsLabel = new JLabel("Total Books: 0 | Available Books: 0");
        updateStats(studentStatsLabel);
        statsPanel.add(studentStatsLabel);

        // Create book table
        studentBookTable = BookTableView.createBookTable();
        JScrollPane scrollPane = new JScrollPane(studentBookTable);

        // Create button panel
        JPanel buttonPanel = new JPanel();
        JButton viewButton = new JButton("View Available Books");
        JButton myBooksButton = new JButton("My Borrowed Books");
        JButton borrowButton = new JButton("Borrow Book");
        JButton returnButton = new JButton("Return Book");
        JButton rateButton = new JButton("Rate Book");
        JButton topRatedButton = new JButton("Top Rated Books");
        
        // Create gamification buttons
        JButton achievementsButton = new JButton("My Achievements");
        JButton leaderboardButton = new JButton("Leaderboard");

        // Add button listeners
        viewButton.addActionListener(e -> viewAvailableBooks(studentBookTable));
        myBooksButton.addActionListener(e -> viewMyBorrowedBooks());
        borrowButton.addActionListener(e -> borrowBook());
        returnButton.addActionListener(e -> returnBook());
        rateButton.addActionListener(e -> rateBook());
        topRatedButton.addActionListener(e -> viewTopRatedBooks());
        achievementsButton.addActionListener(e -> displayStudentAchievements());
        leaderboardButton.addActionListener(e -> displayLeaderboard());

        // Add buttons to panel
        buttonPanel.add(viewButton);
        buttonPanel.add(myBooksButton);
        buttonPanel.add(borrowButton);
        buttonPanel.add(returnButton);
        buttonPanel.add(rateButton);
        buttonPanel.add(topRatedButton);
        
        // Add gamification buttons to a separate panel
        JPanel gamificationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        gamificationPanel.setBorder(BorderFactory.createTitledBorder("Gamification"));
        gamificationPanel.add(achievementsButton);
        gamificationPanel.add(leaderboardButton);

        // Organize panels
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(infoPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.add(statsPanel, BorderLayout.SOUTH);
        
        // Add main buttons and gamification buttons to the bottom
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(gamificationPanel, BorderLayout.SOUTH);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Don't load any books in student view initially - will be loaded after login
        
        return panel;
    }

    /**
     * Displays books borrowed by the current student
     */
    private void viewMyBorrowedBooks() {
        if (currentStudentUID == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to view your books.");
            return;
        }
        
        List<Book> myBooks = library.getBooksBorrowedByStudent(currentStudentUID);
        
        if (myBooks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no borrowed books.");
            viewAvailableBooks(studentBookTable); // Show available books instead
            return;
        }
        
        BookTableView.updateBookTable(myBooks, studentBookTable);
        JOptionPane.showMessageDialog(this, "Showing " + myBooks.size() + " borrowed book(s).");
    }

    /**
     * Adds a new book to the library based on the form input
     */
    private void addBook() {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();
        String yearStr = yearField.getText().trim();
        String category = categoryField.getText().trim();
        
        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title, Author and ISBN are required fields.");
            return;
        }
        
        Book book;
        int year = 0;
        
        if (!yearStr.isEmpty()) {
            try {
                year = Integer.parseInt(yearStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Year must be a valid number.");
                return;
            }
        }
        
        if (category.isEmpty()) {
            if (year > 0) {
                book = new Book(title, author, isbn, year);
            } else {
                book = new Book(title, author, isbn);
            }
        } else {
            if (year > 0) {
                book = new Book(title, author, isbn, year, category);
            } else {
                book = new Book(title, author, isbn);
                book.setCategory(category);
            }
        }
        
        addBookToDatabase(book);
        JOptionPane.showMessageDialog(this, "Book added successfully.");
        clearFields();
        viewAllBooks(adminBookTable);
        updateStats(adminStatsLabel);
        updateStats(studentStatsLabel);
    }

    /**
     * Removes a book from the library
     */
    private void removeBook() {
        // Try to get selected ISBN from table first
        String isbn = null;
        
        if (adminBookTable.getSelectedRow() != -1) {
            isbn = (String) adminBookTable.getValueAt(adminBookTable.getSelectedRow(), 4);
        }
        
        // If no selection, ask user for ISBN
        if (isbn == null) {
            isbn = JOptionPane.showInputDialog(this, "Enter ISBN of book to remove:");
        }
        
        if (isbn != null && !isbn.trim().isEmpty()) {
            if (library.removeBook(isbn.trim())) {
                JOptionPane.showMessageDialog(this, "Book removed successfully");
                updateStats(adminStatsLabel);
                updateStats(studentStatsLabel);
                viewAllBooks(adminBookTable);
            } else {
                JOptionPane.showMessageDialog(this, "Book not found");
            }
        }
    }

    /**
     * Displays all books in the library
     */
    private void viewAllBooks(JTable bookTable) {
        List<Book> allBooks = library.getAllBooks();
        // Sort books by title alphabetically
        allBooks.sort(Comparator.comparing(Book::getTitle));
        BookTableView.updateBookTable(allBooks, bookTable);
    }

    /**
     * Displays only available books
     */
    private void viewAvailableBooks(JTable bookTable) {
        List<Book> availableBooks = library.getAvailableBooks();
        // Sort books by title alphabetically
        availableBooks.sort(Comparator.comparing(Book::getTitle));
        BookTableView.updateBookTable(availableBooks, bookTable);
    }

    /**
     * Handles the book borrowing process
     */
    private void borrowBook() {
        if (currentStudentName == null || currentStudentUID == null) {
            JOptionPane.showMessageDialog(this, "You must be logged in to borrow books.");
            return;
        }

        // Ask if user wants to borrow by title or ISBN
        String[] options = {"By Title", "By ISBN"};
        int choice = JOptionPane.showOptionDialog(this,
                "How would you like to borrow a book?",
                "Borrow Book",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        String identifier = null;
        Book book = null;
        boolean isByTitle = (choice == 0); // 0 = By Title, 1 = By ISBN
        
        // Try to get selected info from table first
        if (studentBookTable.getSelectedRow() != -1) {
            if (isByTitle) {
                identifier = (String) studentBookTable.getValueAt(studentBookTable.getSelectedRow(), 0); // Title column
            } else {
                identifier = (String) studentBookTable.getValueAt(studentBookTable.getSelectedRow(), 4); // ISBN column
            }
        }
        
        // If no selection, ask user for title/ISBN
        if (identifier == null) {
            String prompt = isByTitle ? "Enter title of book to borrow:" : "Enter ISBN of book to borrow:";
            identifier = JOptionPane.showInputDialog(this, prompt);
        }
        
        if (identifier == null || identifier.trim().isEmpty()) {
            return;
        }

        // Find the book
        if (isByTitle) {
            book = library.findBookByTitle(identifier.trim());
            if (book == null) {
                JOptionPane.showMessageDialog(this, "Book with this title not found.");
                return;
            }
        } else {
            book = library.findBook(identifier.trim());
            if (book == null) {
                JOptionPane.showMessageDialog(this, "Book with this ISBN not found.");
                return;
            }
        }

        if (!book.isAvailable()) {
            JOptionPane.showMessageDialog(this, "Book is not available.");
            return;
        }

        // Ask for custom due date
        Object[] dateOptions = {"Default (2 weeks)", "Custom Date"};
        int dateChoice = JOptionPane.showOptionDialog(this,
                "Choose loan period:",
                "Borrow Book",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                dateOptions,
                dateOptions[0]);

        boolean success;
        if (dateChoice == 1) { // Custom date selected
            String dueDateStr = JOptionPane.showInputDialog(this, 
                    "Enter due date (yyyy-MM-dd):", 
                    LocalDate.now().plusDays(library.getLoanPeriodDays()).toString());
            
            if (dueDateStr == null || dueDateStr.trim().isEmpty()) {
                return;
            }
            
            try {
                LocalDate dueDate = LocalDate.parse(dueDateStr.trim());
                if (dueDate.isBefore(LocalDate.now())) {
                    JOptionPane.showMessageDialog(this, "Due date cannot be in the past.");
                    return;
                }
                
                if (isByTitle) {
                    success = library.borrowBookByTitle(identifier.trim(), currentStudentName, currentStudentUID);
                } else {
                    success = library.borrowBook(identifier.trim(), currentStudentName, currentStudentUID, dueDate);
                }
            } catch (DateTimeParseException e) {
                JOptionPane.showMessageDialog(this, "Invalid date format. Use yyyy-MM-dd.");
                return;
            }
        } else {
            // Use default due date
            if (isByTitle) {
                success = library.borrowBookByTitle(identifier.trim(), currentStudentName, currentStudentUID);
            } else {
                success = library.borrowBook(identifier.trim(), currentStudentName, currentStudentUID);
            }
        }

        if (success) {
            JOptionPane.showMessageDialog(this, "Book borrowed successfully.");
            updateStats(adminStatsLabel);
            updateStats(studentStatsLabel);
            // Show the user's borrowed books after successful borrow
            List<Book> borrowedBooks = library.getBooksBorrowedByStudent(currentStudentUID);
            BookTableView.updateBookTable(borrowedBooks, studentBookTable);
        } else {
            JOptionPane.showMessageDialog(this, "Error borrowing book.");
        }
    }

    /**
     * Handles the book return process
     */
    private void returnBook() {
        if (currentStudentUID == null) {
            JOptionPane.showMessageDialog(this, "Please log in first");
            return;
        }
        
        // Get books borrowed by this student
        List<Book> borrowedBooks = library.getBooksBorrowedByStudent(currentStudentUID);
        
        if (borrowedBooks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no borrowed books to return");
            return;
        }
        
        // Check if a book is selected in the table
        String selectedIsbn = null;
        if (studentBookTable.getSelectedRow() != -1) {
            selectedIsbn = BookTableView.getSelectedBookISBN(studentBookTable);
            
            // Verify this book was borrowed by the student
            final String isbn = selectedIsbn; // Create a final copy for the lambda
            boolean borrowedByStudent = borrowedBooks.stream()
                    .anyMatch(book -> book.getIsbn().equals(isbn));
            
            if (!borrowedByStudent) {
                JOptionPane.showMessageDialog(this, "This book was not borrowed by you");
                selectedIsbn = null;
            }
        }
        
        // If no valid selection, ask user to select from their borrowed books
        String isbn = selectedIsbn;
        if (isbn == null) {
            String[] options = new String[borrowedBooks.size()];
            for (int i = 0; i < borrowedBooks.size(); i++) {
                Book book = borrowedBooks.get(i);
                options[i] = book.getTitle() + " (" + book.getIsbn() + ")";
            }
            
            String selection = (String) JOptionPane.showInputDialog(
                this,
                "Select a book to return:",
                "Return Book",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
            );
            
            if (selection == null) {
                return;  // User cancelled
            }
            
            // Extract ISBN from selection
            int isbnStart = selection.lastIndexOf("(") + 1;
            int isbnEnd = selection.lastIndexOf(")");
            isbn = selection.substring(isbnStart, isbnEnd);
        }
        
        // Return the book with student UID for gamification tracking
        if (library.returnBook(isbn, currentStudentUID)) {
            JOptionPane.showMessageDialog(this, "Book returned successfully");
            
            // Refresh the book table
            viewMyBorrowedBooks();
            updateStats(studentStatsLabel);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to return book");
        }
    }
    
    /**
     * Searches for books based on the search criteria
     */
    private void searchBooks(JTextField searchField, JComboBox<String> searchTypeCombo, JTable bookTable) {
        String searchText = searchField.getText().trim();
        String searchType = (String) searchTypeCombo.getSelectedItem();
        
        if (searchText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term.");
            return;
        }
        
        List<Book> results;
        
        // Perform search based on selected criteria
        switch (searchType) {
            case "Title":
                results = library.searchByTitle(searchText);
                break;
            case "Author":
                results = library.searchByAuthor(searchText);
                break;
            case "ISBN":
                results = library.searchByIsbn(searchText);
                break;
            case "Borrower":
                results = library.searchByBorrower(searchText);
                break;
            case "Year":
                try {
                    int year = Integer.parseInt(searchText);
                    results = library.searchByYear(year);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Year must be a valid number.");
                    return;
                }
                break;
            case "Category":
                results = library.searchByCategory(searchText);
                break;
            case "Min Rating":
                try {
                    double minRating = Double.parseDouble(searchText);
                    results = library.searchByMinRating(minRating);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Rating must be a valid number.");
                    return;
                }
                break;
            default:
                results = library.getAllBooks();
        }
        
        BookTableView.updateBookTable(results, bookTable);
    }
    
    /**
     * Updates the statistics display
     */
    private void updateStats(JLabel statsLabel) {
        // Force a refresh of book counts from library
        int totalBooks = library.getAllBooks().size();
        int availableBooks = library.getAvailableBooks().size();
        
        statsLabel.setText("Total Books: " + totalBooks + " | Available Books: " + availableBooks);
        
        // Update both admin and student stats labels to ensure consistency
        if (statsLabel == adminStatsLabel && studentStatsLabel != null) {
            studentStatsLabel.setText("Total Books: " + totalBooks + " | Available Books: " + availableBooks);
        } else if (statsLabel == studentStatsLabel && adminStatsLabel != null) {
            adminStatsLabel.setText("Total Books: " + totalBooks + " | Available Books: " + availableBooks);
        }
    }

    /**
     * Clears all input fields
     */
    private void clearFields() {
        titleField.setText("");
        authorField.setText("");
        isbnField.setText("");
        yearField.setText("");
        categoryField.setText("");
    }

    /**
     * Displays the top 5 rated books
     */
    private void viewTopRatedBooks() {
        List<Book> topBooks = library.getTopRatedBooks(5);
        if (topBooks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No rated books found.");
            return;
        }
        
        BookTableView.updateBookTable(topBooks, studentBookTable);
    }

    /**
     * Displays books that are currently overdue
     */
    private void viewOverdueBooks(JTable bookTable) {
        List<Book> overdueBooks = library.getOverdueBooks();
        if (overdueBooks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No overdue books found.");
        } else {
            BookTableView.updateBookTable(overdueBooks, bookTable);
        }
    }

    /**
     * Displays books that are due within the next few days
     */
    private void viewBooksDueSoon() {
        String daysStr = JOptionPane.showInputDialog(this, 
                "Enter number of days to check:", "7");
        
        if (daysStr == null || daysStr.trim().isEmpty()) {
            return;
        }
        
        try {
            int days = Integer.parseInt(daysStr.trim());
            if (days <= 0) {
                JOptionPane.showMessageDialog(this, "Please enter a positive number.");
                return;
            }
            
            List<Book> dueSoonBooks = library.getBooksDueSoon(days);
            if (dueSoonBooks.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No books due within " + days + " days.");
            } else {
                BookTableView.updateBookTable(dueSoonBooks, adminBookTable);
                JOptionPane.showMessageDialog(this, 
                        dueSoonBooks.size() + " books due within " + days + " days.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number.");
        }
    }

    /**
     * Allows the user to rate a book
     */
    private void rateBook() {
        if (currentStudentUID == null) {
            JOptionPane.showMessageDialog(this, "Please log in first.");
            return;
        }

        String bookId = null;
        double rating = 0;
        
        // Check if a book is selected in the table
        if (studentBookTable.getSelectedRow() != -1) {
            bookId = BookTableView.getSelectedBookISBN(studentBookTable);
        }
        
        // If no selection, ask user for title/ISBN
        if (bookId == null) {
            String[] options = {"By Title", "By ISBN"};
            int choice = JOptionPane.showOptionDialog(
                this, 
                "How would you like to identify the book?", 
                "Rate Book", 
                JOptionPane.DEFAULT_OPTION, 
                JOptionPane.QUESTION_MESSAGE, 
                null, 
                options, 
                options[0]
            );
            
            if (choice == 0) {  // By Title
                String title = JOptionPane.showInputDialog(this, "Enter book title:");
                if (title == null || title.trim().isEmpty()) {
                    return;
                }
                
                Book book = library.findBookByTitle(title.trim());
                if (book == null) {
                    JOptionPane.showMessageDialog(this, "Book not found.");
                    return;
                }
                bookId = book.getIsbn();
            } else if (choice == 1) {  // By ISBN
                bookId = JOptionPane.showInputDialog(this, "Enter book ISBN:");
                if (bookId == null || bookId.trim().isEmpty()) {
                    return;
                }
                
                Book book = library.findBook(bookId.trim());
                if (book == null) {
                    JOptionPane.showMessageDialog(this, "Book not found.");
                    return;
                }
                bookId = book.getIsbn();  // Make sure we have the correct ISBN
            } else {
                return;  // User cancelled
            }
        }
        
        // Ask for rating
        String[] ratingOptions = {"1", "2", "3", "4", "5"};
        int ratingChoice = JOptionPane.showOptionDialog(
            this, 
            "Rate this book:", 
            "Book Rating", 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.QUESTION_MESSAGE, 
            null, 
            ratingOptions, 
            ratingOptions[2]
        );
        
        if (ratingChoice >= 0) {
            rating = ratingChoice + 1;  // Convert to 1-5 scale
            
            // Rate the book with the student's UID for gamification tracking
            if (library.rateBook(bookId, rating, currentStudentUID)) {
                JOptionPane.showMessageDialog(this, "Thank you for rating this book!");
                
                // Refresh view to show updated rating
                searchBooks(studentSearchField, studentSearchTypeCombo, studentBookTable);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to rate the book.");
            }
        }
    }

    /**
     * Helper method to add a book to the database with error handling
     */
    private void addBookToDatabase(Book book) {
        try {
            System.out.println("Adding book: " + book.getTitle() + " by " + book.getAuthor() + ", ISBN: " + book.getIsbn());
            library.addBook(book);
            System.out.println("Successfully added to database.");
        } catch (Exception e) {
            System.err.println("Failed to add book '" + book.getTitle() + "': " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Display student achievements and points
     */
    private void displayStudentAchievements() {
        if (currentStudentUID == null) {
            JOptionPane.showMessageDialog(this, "Please log in as a student first.");
            return;
        }
        
        try {
            // Get student information
            DatabaseManager dbManager = library.getDBManager();
            GamificationSystem gamificationSystem = dbManager.getGamificationSystem();
            
            if (gamificationSystem == null) {
                JOptionPane.showMessageDialog(this, "Gamification system is not available.");
                return;
            }
            
            // Get student ID from UID
            ResultSet studentInfo = dbManager.getStudentInfo(currentStudentUID);
            if (!studentInfo.next()) {
                JOptionPane.showMessageDialog(this, "Student information not found.");
                studentInfo.close();
                return;
            }
            
            int studentRowId = studentInfo.getInt("rowid");
            String studentName = studentInfo.getString("name");
            studentInfo.close();
            
            // Get achievements and progress
            List<GamificationSystem.Achievement> achievements = gamificationSystem.getStudentAchievements(studentRowId);
            Map<String, Object> progress = gamificationSystem.getStudentProgress(studentRowId);
            
            // Create achievement display
            JDialog achievementDialog = new JDialog(this, "Student Achievements", true);
            achievementDialog.setLayout(new BorderLayout());
            achievementDialog.setSize(500, 400);
            achievementDialog.setLocationRelativeTo(this);
            
            // Create progress panel
            JPanel progressPanel = new JPanel(new BorderLayout());
            JLabel progressLabel = new JLabel(String.format(
                "<html><h2>%s</h2><p>Level: %d | Points: %d | Next Level: %d points needed</p></html>",
                studentName,
                progress.get("level"),
                progress.get("points"),
                progress.get("nextLevelPoints")
            ));
            progressLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            progressPanel.add(progressLabel, BorderLayout.CENTER);
            
            // Create achievements panel
            String[] columnNames = {"Achievement", "Description", "Points", "Date Earned"};
            Object[][] data = new Object[achievements.size()][4];
            
            for (int i = 0; i < achievements.size(); i++) {
                GamificationSystem.Achievement a = achievements.get(i);
                data[i][0] = a.getName();
                data[i][1] = a.getDescription();
                data[i][2] = a.getPoints();
                data[i][3] = a.getDateEarned().toLocalDate().toString();
            }
            
            JTable achievementTable = new JTable(data, columnNames);
            achievementTable.setEnabled(false);
            JScrollPane scrollPane = new JScrollPane(achievementTable);
            
            // Add panels to dialog
            achievementDialog.add(progressPanel, BorderLayout.NORTH);
            achievementDialog.add(scrollPane, BorderLayout.CENTER);
            
            // Add close button
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> achievementDialog.dispose());
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);
            achievementDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            // Show dialog
            achievementDialog.setVisible(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error displaying achievements: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Display leaderboard of top students
     */
    private void displayLeaderboard() {
        try {
            // Get database manager
            DatabaseManager dbManager = library.getDBManager();
            GamificationSystem gamificationSystem = dbManager.getGamificationSystem();
            
            if (gamificationSystem == null) {
                JOptionPane.showMessageDialog(this, "Gamification system is not available.");
                return;
            }
            
            // Get leaderboard data (top 10 students)
            List<Map<String, Object>> leaderboard = gamificationSystem.getLeaderboard(10);
            
            // Create leaderboard dialog
            JDialog leaderboardDialog = new JDialog(this, "Student Leaderboard", true);
            leaderboardDialog.setLayout(new BorderLayout());
            leaderboardDialog.setSize(400, 300);
            leaderboardDialog.setLocationRelativeTo(this);
            
            // Create table
            String[] columnNames = {"Rank", "Student", "Level", "Points"};
            Object[][] data = new Object[leaderboard.size()][4];
            
            for (int i = 0; i < leaderboard.size(); i++) {
                Map<String, Object> entry = leaderboard.get(i);
                data[i][0] = i + 1;  // Rank
                data[i][1] = entry.get("studentName");
                data[i][2] = entry.get("level");
                data[i][3] = entry.get("points");
            }
            
            JTable leaderboardTable = new JTable(data, columnNames);
            leaderboardTable.setEnabled(false);
            JScrollPane scrollPane = new JScrollPane(leaderboardTable);
            
            // Add table to dialog
            leaderboardDialog.add(scrollPane, BorderLayout.CENTER);
            
            // Add close button
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> leaderboardDialog.dispose());
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);
            leaderboardDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            // Show dialog
            leaderboardDialog.setVisible(true);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error displaying leaderboard: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main method to start the application
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LibraryManagementSystem().setVisible(true);
        });
    }
} 