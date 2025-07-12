import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Dialog for student login with UID format validation
 */
public class StudentLoginDialog extends JDialog {
    private JTextField nameField;
    private JTextField uidField;
    private JLabel errorLabel;
    private JButton loginButton;
    private JButton cancelButton;
    private boolean loginSuccessful = false;
    private String studentName;
    private String studentUID;

    /**
     * Constructor creates and displays the login dialog
     * @param parent The parent frame
     */
    public StudentLoginDialog(JFrame parent) {
        super(parent, "Student Login", true);
        
        // Create components
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Name field
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        nameField = new JTextField(20);
        panel.add(nameField, gbc);
        
        // UID field
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        panel.add(new JLabel("UID:"), gbc);
        
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        uidField = new JTextField(20);
        panel.add(uidField, gbc);
        
        // Format hint
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        panel.add(new JLabel("Format: YYDEPTXXXXX (e.g., 23BCS12345)"), gbc);
        
        // Error message label
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        panel.add(errorLabel, gbc);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        loginButton = new JButton("Login");
        cancelButton = new JButton("Cancel");
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        panel.add(buttonPanel, gbc);
        
        // Add action listeners
        loginButton.addActionListener(e -> {
            if (validateInput()) {
                loginSuccessful = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        // Handle ENTER key in UID field
        uidField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        });
        
        // Set up dialog
        getContentPane().add(panel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
        setLocationRelativeTo(parent);
        
        // Focus on name field when dialog opens
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                nameField.requestFocus();
            }
        });
    }
    
    /**
     * Validates input and shows appropriate error messages
     */
    private boolean validateInput() {
        studentName = nameField.getText().trim();
        studentUID = uidField.getText().trim();
        
        if (studentName.isEmpty()) {
            errorLabel.setText("Please enter your name");
            nameField.requestFocus();
            return false;
        }
        
        if (studentUID.isEmpty()) {
            errorLabel.setText("Please enter your UID");
            uidField.requestFocus();
            return false;
        }
        
        if (!StudentAuth.validateUID(studentUID)) {
            errorLabel.setText("Invalid UID format. Example: 23BCS12345");
            uidField.requestFocus();
            return false;
        }
        
        return true;
    }
    
    /**
     * Shows the dialog and returns true if login was successful
     */
    public boolean showDialog() {
        setVisible(true);
        return loginSuccessful;
    }
    
    /**
     * Gets the student name entered in the dialog
     */
    public String getStudentName() {
        return studentName;
    }
    
    /**
     * Gets the student UID entered in the dialog
     */
    public String getStudentUID() {
        return studentUID;
    }
} 