import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * BookTableView provides utility methods for displaying books in a table format
 */
public class BookTableView {
    /**
     * Creates a JTable component configured for displaying books
     * @return A configured JTable for displaying books
     */
    public static JTable createBookTable() {
        String[] columnNames = {"Title", "Author", "Year", "Category", "ISBN", "Rating", "Status"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        JTable bookTable = new JTable(tableModel);
        bookTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bookTable.setRowHeight(25);
        bookTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        bookTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Title
        bookTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Author
        bookTable.getColumnModel().getColumn(2).setPreferredWidth(60);  // Year
        bookTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Category
        bookTable.getColumnModel().getColumn(4).setPreferredWidth(120); // ISBN
        bookTable.getColumnModel().getColumn(5).setPreferredWidth(100); // Rating
        bookTable.getColumnModel().getColumn(6).setPreferredWidth(180); // Status
        
        return bookTable;
    }
    
    /**
     * Updates the book table with the given list of books
     * @param books The list of books to display
     * @param bookTable The table to update
     */
    public static void updateBookTable(List<Book> books, JTable bookTable) {
        DefaultTableModel model = (DefaultTableModel) bookTable.getModel();
        model.setRowCount(0); // Clear existing rows
        
        if (books.isEmpty()) {
            return;
        }
        
        for (Book book : books) {
            // Create status string
            String status;
            if (book.isAvailable()) {
                status = "Available";
            } else {
                status = "Borrowed by: " + book.getBorrowerName();
                if (book.getDueDate() != null) {
                    String formattedDate = book.getDueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
                    status += " (Due: " + formattedDate + ")";
                    if (book.isOverdue()) {
                        status += " - OVERDUE";
                    }
                }
            }
            
            // Create rating string
            String ratingStr;
            if (book.getRatingCount() > 0) {
                ratingStr = String.format("%.1f/5.0 (%d)", book.getRating(), book.getRatingCount());
            } else {
                ratingStr = "No ratings";
            }
            
            // Add a row to the table
            model.addRow(new Object[] {
                book.getTitle(),
                book.getAuthor(),
                book.getPublicationYear() > 0 ? book.getPublicationYear() : "",
                book.getCategory(),
                book.getIsbn(),
                ratingStr,
                status
            });
        }
    }
    
    /**
     * Gets the ISBN of the selected book in the table
     * @param bookTable The table containing books
     * @return The ISBN of the selected book, or null if no book is selected
     */
    public static String getSelectedBookISBN(JTable bookTable) {
        int selectedRow = bookTable.getSelectedRow();
        if (selectedRow == -1) {
            return null;
        }
        
        // ISBN is in column 4
        return (String) bookTable.getValueAt(selectedRow, 4);
    }
} 