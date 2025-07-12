import java.sql.*;
import java.io.File;

/**
 * Simple utility to check the state of the database and troubleshoot issues
 */
public class DatabaseChecker {
    public static void main(String[] args) {
        System.out.println("Database Checker Utility");
        System.out.println("=======================");
        
        // Check if database file exists
        File dbFile = new File("library.db");
        if (!dbFile.exists()) {
            System.out.println("Database file does not exist.");
            return;
        }
        
        System.out.println("Database file exists: " + dbFile.getAbsolutePath());
        System.out.println("Database file size: " + dbFile.length() + " bytes");
        
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded successfully.");
            
            // Connect to database
            Connection conn = DriverManager.getConnection("jdbc:sqlite:library.db");
            System.out.println("Connected to database successfully.");
            
            // Check tables
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet tables = metaData.getTables(null, null, "%", new String[] {"TABLE"});
            
            System.out.println("\nTables in database:");
            boolean hasBookTable = false;
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("- " + tableName);
                if (tableName.equals("books")) {
                    hasBookTable = true;
                }
            }
            
            if (!hasBookTable) {
                System.out.println("WARNING: 'books' table does not exist!");
                return;
            }
            
            // Check book count
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books");
            
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("\nBook count: " + count);
                
                if (count > 0) {
                    // Show sample books
                    ResultSet books = stmt.executeQuery("SELECT title, author, isbn FROM books LIMIT 5");
                    System.out.println("\nSample books:");
                    while (books.next()) {
                        System.out.println("- " + books.getString("title") + " by " + 
                                           books.getString("author") + " (ISBN: " + 
                                           books.getString("isbn") + ")");
                    }
                    books.close();
                } else {
                    System.out.println("WARNING: No books in database.");
                }
            }
            
            // Close connections
            rs.close();
            stmt.close();
            conn.close();
            System.out.println("\nDatabase check completed.");
            
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 