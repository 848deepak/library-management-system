import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

public class RecommendationPanel extends JPanel {
    private JTable recommendationsTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> categoryComboBox;
    private JSlider interestSlider;
    private final RecommendationSystem recommendationSystem;
    private final String studentId;
    private BookTableView bookTableView;

    public RecommendationPanel(Connection connection, String studentId) {
        this.recommendationSystem = new RecommendationSystem(connection);
        this.studentId = studentId;
        this.bookTableView = new BookTableView();
        
        setLayout(new BorderLayout());
        
        // Initialize the UI components
        initializeComponents();
        
        // Load recommendations
        updateRecommendations();
    }
    
    private void initializeComponents() {
        // Create the recommendations table
        String[] columns = {"Title", "Author", "Category", "Year", "Rating", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        recommendationsTable = new JTable(tableModel);
        recommendationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recommendationsTable.getTableHeader().setReorderingAllowed(false);
        
        // Set column widths
        recommendationsTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Title
        recommendationsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Author
        recommendationsTable.getColumnModel().getColumn(2).setPreferredWidth(100); // Category
        recommendationsTable.getColumnModel().getColumn(3).setPreferredWidth(60);  // Year
        recommendationsTable.getColumnModel().getColumn(4).setPreferredWidth(60);  // Rating
        recommendationsTable.getColumnModel().getColumn(5).setPreferredWidth(80);  // Status
        
        JScrollPane scrollPane = new JScrollPane(recommendationsTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create control panel for setting interests
        JPanel interestsPanel = createInterestsPanel();
        add(interestsPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createInterestsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Update Your Interests"));
        
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // Category selection dropdown
        String[] categories = {"Fiction", "Non-Fiction", "Science", "History", "Biography", 
                              "Technology", "Philosophy", "Art", "Psychology", "Business"};
        categoryComboBox = new JComboBox<>(categories);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(categoryComboBox);
        
        // Interest level slider
        inputPanel.add(new JLabel("Interest Level:"));
        interestSlider = new JSlider(JSlider.HORIZONTAL, 1, 5, 3);
        interestSlider.setMajorTickSpacing(1);
        interestSlider.setPaintTicks(true);
        interestSlider.setPaintLabels(true);
        interestSlider.setSnapToTicks(true);
        inputPanel.add(interestSlider);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton updateButton = new JButton("Update Interest");
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateInterest();
            }
        });
        buttonPanel.add(updateButton);
        
        JButton analyzeButton = new JButton("Analyze My History");
        analyzeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                analyzeHistory();
            }
        });
        buttonPanel.add(analyzeButton);
        
        JButton refreshButton = new JButton("Refresh Recommendations");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateRecommendations();
            }
        });
        buttonPanel.add(refreshButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateInterest() {
        String category = (String) categoryComboBox.getSelectedItem();
        int interestLevel = interestSlider.getValue();
        
        try {
            recommendationSystem.updateStudentInterest(studentId, category, interestLevel);
            JOptionPane.showMessageDialog(this, 
                "Interest updated: " + category + " (Level " + interestLevel + ")",
                "Interest Updated", JOptionPane.INFORMATION_MESSAGE);
            
            // Refresh recommendations
            updateRecommendations();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error updating interest: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void analyzeHistory() {
        try {
            recommendationSystem.analyzeStudentBorrowingHistory(studentId);
            
            // Display current interests after analysis
            Map<String, Integer> interests = recommendationSystem.getStudentInterests(studentId);
            StringBuilder message = new StringBuilder("Your interests based on borrowing history:\n");
            
            for (Map.Entry<String, Integer> entry : interests.entrySet()) {
                message.append(entry.getKey()).append(": ")
                       .append("★".repeat(entry.getValue()))
                       .append("\n");
            }
            
            JOptionPane.showMessageDialog(this, message.toString(),
                "Interest Analysis Complete", JOptionPane.INFORMATION_MESSAGE);
            
            // Refresh recommendations
            updateRecommendations();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error analyzing history: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateRecommendations() {
        // Clear the table
        tableModel.setRowCount(0);
        
        // Get recommendations
        List<Book> recommendations = recommendationSystem.getRecommendedBooks(studentId);
        
        // Add books to the table
        for (Book book : recommendations) {
            Object[] row = {
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getYear(),
                formatRating(book.getRating()),
                book.getStatus()
            };
            tableModel.addRow(row);
        }
        
        if (recommendations.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No recommendations found. Try updating your interests or analyzing your borrowing history.",
                "No Recommendations", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private String formatRating(double rating) {
        int fullStars = (int) rating;
        boolean halfStar = rating - fullStars >= 0.5;
        
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < fullStars; i++) {
            stars.append("★");
        }
        
        if (halfStar) {
            stars.append("½");
        }
        
        return stars.toString();
    }
} 