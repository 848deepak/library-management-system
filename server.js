const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const cors = require('cors');
const bodyParser = require('body-parser');
const path = require('path');
const bcrypt = require('bcrypt');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Serve static files from client build directory
app.use(express.static(path.join(__dirname, 'client/build')));

// Initialize SQLite database
const db = new sqlite3.Database('./library.db', (err) => {
    if (err) {
        console.error('Error opening database:', err);
    } else {
        console.log('Connected to SQLite database');
        initializeDatabase();
    }
});

// Initialize database tables
function initializeDatabase() {
    // Books table
    db.run(`CREATE TABLE IF NOT EXISTS books (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        title TEXT NOT NULL,
        author TEXT NOT NULL,
        isbn TEXT UNIQUE,
        year INTEGER,
        category TEXT,
        available INTEGER DEFAULT 1,
        borrowed_by TEXT,
        borrowed_date TEXT,
        due_date TEXT,
        rating REAL DEFAULT 0,
        rating_count INTEGER DEFAULT 0
    )`, (err) => {
        if (err) {
            console.error('Error creating books table:', err);
            return;
        }
        console.log('Books table created or already exists');
    });

    // Students table
    db.run(`CREATE TABLE IF NOT EXISTS students (
        uid TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        department TEXT,
        year INTEGER,
        points INTEGER DEFAULT 0,
        books_borrowed INTEGER DEFAULT 0,
        books_returned INTEGER DEFAULT 0
    )`, (err) => {
        if (err) {
            console.error('Error creating students table:', err);
            return;
        }
        console.log('Students table created or already exists');
    });

    // Borrowing history table
    db.run(`CREATE TABLE IF NOT EXISTS borrowing_history (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_uid TEXT,
        book_id INTEGER,
        borrowed_date TEXT,
        returned_date TEXT,
        status TEXT DEFAULT 'borrowed',
        FOREIGN KEY(student_uid) REFERENCES students(uid),
        FOREIGN KEY(book_id) REFERENCES books(id)
    )`, (err) => {
        if (err) {
            console.error('Error creating borrowing_history table:', err);
            return;
        }
        console.log('Borrowing history table created or already exists');
    });

    // Achievements table
    db.run(`CREATE TABLE IF NOT EXISTS achievements (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        student_uid TEXT,
        achievement_name TEXT,
        achievement_description TEXT,
        points_earned INTEGER,
        earned_date TEXT,
        FOREIGN KEY(student_uid) REFERENCES students(uid)
    )`, (err) => {
        if (err) {
            console.error('Error creating achievements table:', err);
            return;
        }
        console.log('Achievements table created or already exists');
        
        // Add sample data after all tables are created
        setTimeout(addSampleData, 1000);
    });
}

function addSampleData() {
    // Check if books exist
    db.get("SELECT COUNT(*) as count FROM books", (err, row) => {
        if (err) {
            console.error(err);
            return;
        }
        
        if (row.count === 0) {
            console.log("Adding comprehensive sample library...");
            const sampleBooks = [
                // Classic Literature
                ["The Great Gatsby", "F. Scott Fitzgerald", "978-0-7432-7356-5", 1925, "Classic Literature"],
                ["To Kill a Mockingbird", "Harper Lee", "978-0-06-112008-4", 1960, "Classic Literature"],
                ["1984", "George Orwell", "978-0-452-28423-4", 1949, "Classic Literature"],
                ["Pride and Prejudice", "Jane Austen", "978-0-14-143951-8", 1813, "Classic Literature"],
                ["The Catcher in the Rye", "J.D. Salinger", "978-0-316-76948-0", 1951, "Classic Literature"],
                ["Lord of the Flies", "William Golding", "978-0-571-05686-2", 1954, "Classic Literature"],
                ["Animal Farm", "George Orwell", "978-0-452-28424-1", 1945, "Classic Literature"],
                ["Brave New World", "Aldous Huxley", "978-0-06-085052-4", 1932, "Classic Literature"],
                
                // Computer Science & Programming
                ["Introduction to Algorithms", "Thomas H. Cormen", "978-0-262-03384-8", 2009, "Computer Science"],
                ["Clean Code", "Robert C. Martin", "978-0-13-235088-4", 2008, "Computer Science"],
                ["Design Patterns", "Gang of Four", "978-0-201-63361-0", 1994, "Computer Science"],
                ["The Pragmatic Programmer", "David Thomas", "978-0-201-61622-4", 1999, "Computer Science"],
                ["Code Complete", "Steve McConnell", "978-0-7356-1967-8", 2004, "Computer Science"],
                ["JavaScript: The Good Parts", "Douglas Crockford", "978-0-596-51774-8", 2008, "Computer Science"],
                ["Python Crash Course", "Eric Matthes", "978-1-59327-928-8", 2019, "Computer Science"],
                ["You Don't Know JS", "Kyle Simpson", "978-1-4919-0415-0", 2015, "Computer Science"],
                ["Effective Java", "Joshua Bloch", "978-0-13-468599-1", 2017, "Computer Science"],
                ["Structure and Interpretation of Computer Programs", "Harold Abelson", "978-0-262-01153-2", 1996, "Computer Science"],
                
                // Science & Mathematics
                ["A Brief History of Time", "Stephen Hawking", "978-0-553-38016-3", 1988, "Science"],
                ["The Elegant Universe", "Brian Greene", "978-0-375-70811-4", 2003, "Science"],
                ["Cosmos", "Carl Sagan", "978-0-345-33135-9", 1980, "Science"],
                ["The Gene", "Siddhartha Mukherjee", "978-1-4767-3350-5", 2016, "Science"],
                ["Calculus Made Easy", "Silvanus P. Thompson", "978-0-312-18548-0", 1998, "Mathematics"],
                ["The Art of Problem Solving", "Richard Rusczyk", "978-0-9773045-1-8", 2006, "Mathematics"],
                ["Linear Algebra Done Right", "Sheldon Axler", "978-3-319-11079-0", 2015, "Mathematics"],
                
                // Business & Economics
                ["Think and Grow Rich", "Napoleon Hill", "978-1-58542-433-7", 1937, "Business"],
                ["The Lean Startup", "Eric Ries", "978-0-307-88789-4", 2011, "Business"],
                ["Good to Great", "Jim Collins", "978-0-06-662099-2", 2001, "Business"],
                ["The Intelligent Investor", "Benjamin Graham", "978-0-06-055566-5", 1949, "Business"],
                ["Freakonomics", "Steven Levitt", "978-0-06-073132-6", 2005, "Economics"],
                ["The Wealth of Nations", "Adam Smith", "978-0-14-043208-6", 1776, "Economics"],
                
                // Psychology & Self-Help
                ["Atomic Habits", "James Clear", "978-0-7352-1129-2", 2018, "Psychology"],
                ["The 7 Habits of Highly Effective People", "Stephen Covey", "978-1-982137-27-7", 1989, "Self-Help"],
                ["How to Win Friends and Influence People", "Dale Carnegie", "978-0-671-72765-5", 1936, "Self-Help"],
                ["Mindset", "Carol Dweck", "978-0-345-47232-8", 2006, "Psychology"],
                ["The Power of Now", "Eckhart Tolle", "978-1-57731-152-2", 1997, "Psychology"],
                ["Thinking, Fast and Slow", "Daniel Kahneman", "978-0-374-53355-7", 2011, "Psychology"],
                
                // History & Biography
                ["Sapiens", "Yuval Noah Harari", "978-0-06-231609-7", 2014, "History"],
                ["The Diary of a Young Girl", "Anne Frank", "978-0-553-29698-1", 1947, "Biography"],
                ["Steve Jobs", "Walter Isaacson", "978-1-4516-4853-9", 2011, "Biography"],
                ["Long Walk to Freedom", "Nelson Mandela", "978-0-316-54818-3", 1994, "Biography"],
                ["The Wright Brothers", "David McCullough", "978-1-4767-2874-7", 2015, "History"],
                ["Guns, Germs, and Steel", "Jared Diamond", "978-0-393-31755-8", 1997, "History"],
                
                // Fiction & Fantasy
                ["Harry Potter and the Philosopher's Stone", "J.K. Rowling", "978-0-7475-3269-9", 1997, "Fantasy"],
                ["The Lord of the Rings", "J.R.R. Tolkien", "978-0-544-00341-5", 1954, "Fantasy"],
                ["Dune", "Frank Herbert", "978-0-441-17271-9", 1965, "Science Fiction"],
                ["The Hitchhiker's Guide to the Galaxy", "Douglas Adams", "978-0-345-39180-3", 1979, "Science Fiction"],
                ["The Kite Runner", "Khaled Hosseini", "978-1-59448-000-3", 2003, "Contemporary Fiction"],
                ["One Hundred Years of Solitude", "Gabriel García Márquez", "978-0-06-088328-7", 1967, "Magical Realism"],
                
                // Engineering & Technology
                ["The Innovators", "Walter Isaacson", "978-1-4767-0869-0", 2014, "Technology"],
                ["Zero to One", "Peter Thiel", "978-0-8041-9684-3", 2014, "Technology"],
                ["The Design of Everyday Things", "Don Norman", "978-0-465-05065-9", 2013, "Design"],
                ["Fundamentals of Electric Circuits", "Charles Alexander", "978-0-07-338057-5", 2016, "Engineering"],
                ["Introduction to Robotics", "John Craig", "978-0-201-54361-4", 2004, "Engineering"],
                
                // Arts & Philosophy
                ["The Art of War", "Sun Tzu", "978-1-59030-963-7", -500, "Philosophy"],
                ["Meditations", "Marcus Aurelius", "978-0-14-044933-6", 180, "Philosophy"],
                ["The Republic", "Plato", "978-0-14-044914-5", -380, "Philosophy"],
                ["Ways of Seeing", "John Berger", "978-0-14-013515-0", 1972, "Art"],
                ["The Story of Art", "Ernst Gombrich", "978-0-7148-3355-2", 1950, "Art"],
                
                // Health & Medicine
                ["Gray's Anatomy", "Henry Gray", "978-0-7020-5230-9", 1858, "Medicine"],
                ["The Immortal Life of Henrietta Lacks", "Rebecca Skloot", "978-1-4000-5217-2", 2010, "Medicine"],
                ["Being Mortal", "Atul Gawande", "978-0-8050-9515-9", 2014, "Medicine"],
                ["The Body Keeps the Score", "Bessel van der Kolk", "978-0-670-78593-9", 2014, "Psychology"],
                
                // Reference & Textbooks
                ["Oxford English Dictionary", "Oxford University Press", "978-0-19-861186-8", 1989, "Reference"],
                ["Merriam-Webster Dictionary", "Merriam-Webster", "978-0-87779-296-4", 2016, "Reference"],
                ["Encyclopedia Britannica", "Britannica", "978-1-59339-292-5", 2010, "Reference"],
                ["The Chicago Manual of Style", "University of Chicago Press", "978-0-226-10420-1", 2017, "Reference"],
                
                // Environmental Science
                ["Silent Spring", "Rachel Carson", "978-0-618-24906-0", 1962, "Environmental Science"],
                ["The Sixth Extinction", "Elizabeth Kolbert", "978-0-8050-9979-9", 2014, "Environmental Science"],
                ["This Changes Everything", "Naomi Klein", "978-1-4516-9738-4", 2014, "Environmental Science"]
            ];

            console.log(`Adding ${sampleBooks.length} books to the library...`);
            const stmt = db.prepare("INSERT INTO books (title, author, isbn, year, category) VALUES (?, ?, ?, ?, ?)");
            sampleBooks.forEach(book => {
                stmt.run(book);
            });
            stmt.finalize();
            console.log("Library initialization complete!");
        }
    });
}

// API Routes

// Get all books
app.get('/api/books', (req, res) => {
    const query = `
        SELECT b.*, s.name as borrowed_by_name 
        FROM books b 
        LEFT JOIN students s ON b.borrowed_by = s.uid
    `;
    
    db.all(query, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json(rows);
    });
});

// Search books
app.get('/api/books/search', (req, res) => {
    const { q, type = 'title' } = req.query;
    
    let query;
    let params;
    
    switch (type) {
        case 'author':
            query = "SELECT * FROM books WHERE author LIKE ? ORDER BY title";
            params = [`%${q}%`];
            break;
        case 'category':
            query = "SELECT * FROM books WHERE category LIKE ? ORDER BY title";
            params = [`%${q}%`];
            break;
        case 'isbn':
            query = "SELECT * FROM books WHERE isbn LIKE ? ORDER BY title";
            params = [`%${q}%`];
            break;
        default:
            query = "SELECT * FROM books WHERE title LIKE ? ORDER BY title";
            params = [`%${q}%`];
    }
    
    db.all(query, params, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json(rows);
    });
});

// Add new book (admin only)
app.post('/api/books', (req, res) => {
    const { title, author, isbn, year, category } = req.body;
    
    const query = "INSERT INTO books (title, author, isbn, year, category) VALUES (?, ?, ?, ?, ?)";
    db.run(query, [title, author, isbn, year, category], function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json({ id: this.lastID, message: 'Book added successfully' });
    });
});

// Delete book (admin only)
app.delete('/api/books/:id', (req, res) => {
    const query = "DELETE FROM books WHERE id = ?";
    db.run(query, [req.params.id], function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json({ message: 'Book deleted successfully' });
    });
});

// Student authentication
app.post('/api/auth/student', (req, res) => {
    const { uid } = req.body;
    
    // Validate UID format (e.g., 23BCS12345)
    const uidPattern = /^(\d{2})(BCS|BCE|BBA|BME|BEC|BIT)(\d{5})$/;
    const match = uid.match(uidPattern);
    
    if (!match) {
        return res.status(400).json({ error: 'Invalid UID format' });
    }
    
    const [, year, dept, number] = match;
    const studentName = `Student ${number}`;
    
    // Check if student exists, if not create
    db.get("SELECT * FROM students WHERE uid = ?", [uid], (err, row) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        
        if (row) {
            res.json({ uid: row.uid, name: row.name, department: row.department, points: row.points });
        } else {
            // Create new student
            const query = "INSERT INTO students (uid, name, department, year) VALUES (?, ?, ?, ?)";
            db.run(query, [uid, studentName, dept, 20 + parseInt(year)], function(err) {
                if (err) {
                    res.status(500).json({ error: err.message });
                    return;
                }
                res.json({ uid, name: studentName, department: dept, points: 0 });
            });
        }
    });
});

// Admin authentication
app.post('/api/auth/admin', (req, res) => {
    const { password } = req.body;
    
    if (password === 'admin123') {
        res.json({ success: true, role: 'admin' });
    } else {
        res.status(401).json({ error: 'Invalid admin password' });
    }
});

// Borrow book
app.post('/api/books/:id/borrow', (req, res) => {
    const { studentUid } = req.body;
    const bookId = req.params.id;
    
    const borrowDate = new Date().toISOString().split('T')[0];
    const dueDate = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0]; // 14 days from now
    
    // Check if book is available
    db.get("SELECT * FROM books WHERE id = ? AND available = 1", [bookId], (err, book) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        
        if (!book) {
            res.status(400).json({ error: 'Book not available' });
            return;
        }
        
        // Update book as borrowed
        const updateBookQuery = "UPDATE books SET available = 0, borrowed_by = ?, borrowed_date = ?, due_date = ? WHERE id = ?";
        db.run(updateBookQuery, [studentUid, borrowDate, dueDate, bookId], (err) => {
            if (err) {
                res.status(500).json({ error: err.message });
                return;
            }
            
            // Add to borrowing history
            const historyQuery = "INSERT INTO borrowing_history (student_uid, book_id, borrowed_date, status) VALUES (?, ?, ?, 'borrowed')";
            db.run(historyQuery, [studentUid, bookId, borrowDate], (err) => {
                if (err) {
                    console.error('Error adding to history:', err);
                }
            });
            
            // Update student stats
            db.run("UPDATE students SET books_borrowed = books_borrowed + 1, points = points + 10 WHERE uid = ?", [studentUid]);
            
            res.json({ message: 'Book borrowed successfully', dueDate });
        });
    });
});

// Return book
app.post('/api/books/:id/return', (req, res) => {
    const { studentUid } = req.body;
    const bookId = req.params.id;
    const returnDate = new Date().toISOString().split('T')[0];
    
    // Update book as available
    const updateBookQuery = "UPDATE books SET available = 1, borrowed_by = NULL, borrowed_date = NULL, due_date = NULL WHERE id = ? AND borrowed_by = ?";
    db.run(updateBookQuery, [bookId, studentUid], function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        
        if (this.changes === 0) {
            res.status(400).json({ error: 'Book not borrowed by this student' });
            return;
        }
        
        // Update borrowing history
        const historyQuery = "UPDATE borrowing_history SET returned_date = ?, status = 'returned' WHERE student_uid = ? AND book_id = ? AND status = 'borrowed'";
        db.run(historyQuery, [returnDate, studentUid, bookId]);
        
        // Update student stats
        db.run("UPDATE students SET books_returned = books_returned + 1, points = points + 15 WHERE uid = ?", [studentUid]);
        
        res.json({ message: 'Book returned successfully' });
    });
});

// Rate book
app.post('/api/books/:id/rate', (req, res) => {
    const { rating } = req.body;
    const bookId = req.params.id;
    
    if (rating < 1 || rating > 5) {
        return res.status(400).json({ error: 'Rating must be between 1 and 5' });
    }
    
    // Update book rating
    db.get("SELECT rating, rating_count FROM books WHERE id = ?", [bookId], (err, book) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        
        const newRatingCount = book.rating_count + 1;
        const newRating = ((book.rating * book.rating_count) + rating) / newRatingCount;
        
        db.run("UPDATE books SET rating = ?, rating_count = ? WHERE id = ?", [newRating, newRatingCount, bookId], (err) => {
            if (err) {
                res.status(500).json({ error: err.message });
                return;
            }
            res.json({ message: 'Book rated successfully', newRating: newRating.toFixed(1) });
        });
    });
});

// Get student profile
app.get('/api/students/:uid', (req, res) => {
    const uid = req.params.uid;
    
    db.get("SELECT * FROM students WHERE uid = ?", [uid], (err, student) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        
        if (!student) {
            res.status(404).json({ error: 'Student not found' });
            return;
        }
        
        res.json(student);
    });
});

// Get leaderboard
app.get('/api/leaderboard', (req, res) => {
    const query = `
        SELECT uid, name, department, points, books_borrowed, books_returned
        FROM students 
        WHERE points > 0 
        ORDER BY points DESC 
        LIMIT 10
    `;
    
    db.all(query, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        res.json(rows);
    });
});

// Get library statistics
app.get('/api/stats', (req, res) => {
    const stats = {};
    
    // Get total books
    db.get("SELECT COUNT(*) as total, SUM(available) as available FROM books", (err, bookStats) => {
        if (err) {
            res.status(500).json({ error: err.message });
            return;
        }
        
        stats.totalBooks = bookStats.total;
        stats.availableBooks = bookStats.available;
        stats.borrowedBooks = bookStats.total - bookStats.available;
        
        // Get total students
        db.get("SELECT COUNT(*) as total FROM students", (err, studentStats) => {
            if (err) {
                res.status(500).json({ error: err.message });
                return;
            }
            
            stats.totalStudents = studentStats.total;
            res.json(stats);
        });
    });
});

// Serve React app for all other routes
app.get('*', (req, res) => {
    res.sendFile(path.join(__dirname, 'client/build', 'index.html'));
});

// Start server
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
    console.log(`Access the application at: http://localhost:${PORT}`);
});

// Graceful shutdown
process.on('SIGINT', () => {
    console.log('\nShutting down server...');
    db.close((err) => {
        if (err) {
            console.error(err.message);
        } else {
            console.log('Database connection closed.');
        }
        process.exit(0);
    });
});
