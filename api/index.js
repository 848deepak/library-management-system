const express = require('express');
const sqlite3 = require('sqlite3').verbose();
const cors = require('cors');
const bodyParser = require('body-parser');
const path = require('path');

const app = express();

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Initialize SQLite database
let db;

function initializeDatabase() {
    return new Promise((resolve, reject) => {
        db = new sqlite3.Database(':memory:', (err) => {
            if (err) {
                console.error('Error opening database:', err);
                reject(err);
                return;
            }
            console.log('Connected to SQLite database (in-memory for demo)');
            
            // Create tables and add sample data
            createTables().then(() => {
                addSampleData().then(() => {
                    resolve();
                });
            });
        });
    });
}

function createTables() {
    return new Promise((resolve) => {
        db.serialize(() => {
            // Books table
            db.run(`CREATE TABLE IF NOT EXISTS books (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                author TEXT NOT NULL,
                isbn TEXT UNIQUE,
                year INTEGER,
                category TEXT,
                is_available INTEGER DEFAULT 1,
                borrowed_by TEXT,
                borrowed_date TEXT,
                due_date TEXT,
                rating REAL DEFAULT 0
            )`);

            // Students table
            db.run(`CREATE TABLE IF NOT EXISTS students (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                uid TEXT UNIQUE NOT NULL,
                name TEXT NOT NULL,
                password TEXT DEFAULT 'password',
                points INTEGER DEFAULT 0,
                books_borrowed INTEGER DEFAULT 0
            )`);

            // Borrowing history table
            db.run(`CREATE TABLE IF NOT EXISTS borrowing_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_uid TEXT,
                book_id INTEGER,
                borrowed_date TEXT,
                returned_date TEXT,
                status TEXT DEFAULT 'borrowed'
            )`);

            // Achievements table
            db.run(`CREATE TABLE IF NOT EXISTS achievements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_uid TEXT,
                achievement_type TEXT,
                points_earned INTEGER,
                date_earned TEXT
            )`, () => {
                resolve();
            });
        });
    });
}

function addSampleData() {
    return new Promise((resolve) => {
        const sampleBooks = [
            { title: "The Great Gatsby", author: "F. Scott Fitzgerald", isbn: "978-0-7432-7356-5", year: 1925, category: "Fiction", rating: 4.2 },
            { title: "To Kill a Mockingbird", author: "Harper Lee", isbn: "978-0-06-112008-4", year: 1960, category: "Fiction", rating: 4.5 },
            { title: "1984", author: "George Orwell", isbn: "978-0-452-28423-4", year: 1949, category: "Dystopian", rating: 4.7 },
            { title: "Pride and Prejudice", author: "Jane Austen", isbn: "978-0-14-143951-8", year: 1813, category: "Romance", rating: 4.3 },
            { title: "The Catcher in the Rye", author: "J.D. Salinger", isbn: "978-0-316-76948-0", year: 1951, category: "Fiction", rating: 3.9 },
            { title: "Introduction to Algorithms", author: "Thomas H. Cormen", isbn: "978-0-262-03384-8", year: 2009, category: "Computer Science", rating: 4.8 },
            { title: "Clean Code", author: "Robert C. Martin", isbn: "978-0-13-235088-4", year: 2008, category: "Programming", rating: 4.6 },
            { title: "Design Patterns", author: "Gang of Four", isbn: "978-0-201-63361-0", year: 1994, category: "Programming", rating: 4.4 },
            { title: "The Art of Computer Programming", author: "Donald E. Knuth", isbn: "978-0-201-89683-1", year: 1968, category: "Computer Science", rating: 4.9 },
            { title: "Harry Potter and the Sorcerer's Stone", author: "J.K. Rowling", isbn: "978-0-439-70818-8", year: 1997, category: "Fantasy", rating: 4.7 }
        ];

        const sampleStudents = [
            { uid: "23BCS12345", name: "Student Demo", points: 85, books_borrowed: 3 },
            { uid: "22BCE10001", name: "Another Student", points: 120, books_borrowed: 5 },
            { uid: "21BBA10002", name: "Third Student", points: 45, books_borrowed: 2 }
        ];

        db.serialize(() => {
            const bookStmt = db.prepare("INSERT OR IGNORE INTO books (title, author, isbn, year, category, rating) VALUES (?, ?, ?, ?, ?, ?)");
            sampleBooks.forEach(book => {
                bookStmt.run(book.title, book.author, book.isbn, book.year, book.category, book.rating);
            });
            bookStmt.finalize();

            const studentStmt = db.prepare("INSERT OR IGNORE INTO students (uid, name, points, books_borrowed) VALUES (?, ?, ?, ?)");
            sampleStudents.forEach(student => {
                studentStmt.run(student.uid, student.name, student.points, student.books_borrowed);
            });
            studentStmt.finalize(() => {
                resolve();
            });
        });
    });
}

// API Routes

// Authentication
app.post('/api/auth/student', (req, res) => {
    const { uid } = req.body;
    
    const uidPattern = /^\d{2}[A-Z]{3}\d{5}$/;
    if (!uidPattern.test(uid)) {
        return res.status(400).json({ success: false, message: 'Invalid UID format' });
    }

    db.get("SELECT * FROM students WHERE uid = ?", [uid], (err, row) => {
        if (err) {
            return res.status(500).json({ success: false, message: err.message });
        }

        if (row) {
            res.json({ 
                success: true, 
                user: { 
                    type: 'student', 
                    uid: row.uid, 
                    name: row.name,
                    points: row.points,
                    books_borrowed: row.books_borrowed
                } 
            });
        } else {
            // Create new student
            const studentName = `Student ${uid}`;
            db.run(
                "INSERT INTO students (uid, name, points, books_borrowed) VALUES (?, ?, ?, ?)",
                [uid, studentName, 0, 0],
                function(err) {
                    if (err) {
                        return res.status(500).json({ success: false, message: err.message });
                    }
                    res.json({ 
                        success: true, 
                        user: { 
                            type: 'student', 
                            uid: uid, 
                            name: studentName,
                            points: 0,
                            books_borrowed: 0
                        } 
                    });
                }
            );
        }
    });
});

app.post('/api/auth/admin', (req, res) => {
    const { password } = req.body;
    
    if (password === 'admin123') {
        res.json({ success: true, user: { type: 'admin', name: 'Administrator' } });
    } else {
        res.status(401).json({ success: false, message: 'Invalid admin password' });
    }
});

// Books API
app.get('/api/books', (req, res) => {
    const { q, type } = req.query;
    
    let query = `
        SELECT b.*, s.name as borrowed_by_name 
        FROM books b 
        LEFT JOIN students s ON b.borrowed_by = s.uid
        ORDER BY b.title
    `;
    let params = [];

    if (q && q.trim()) {
        const searchField = type === 'author' ? 'b.author' : 
                          type === 'category' ? 'b.category' : 
                          type === 'isbn' ? 'b.isbn' : 'b.title';
        
        query = `
            SELECT b.*, s.name as borrowed_by_name 
            FROM books b 
            LEFT JOIN students s ON b.borrowed_by = s.uid
            WHERE ${searchField} LIKE ? 
            ORDER BY b.title
        `;
        params = [`%${q}%`];
    }
    
    db.all(query, params, (err, rows) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        res.json(rows);
    });
});

app.post('/api/books', (req, res) => {
    const { title, author, isbn, year, category } = req.body;
    
    const query = "INSERT INTO books (title, author, isbn, year, category) VALUES (?, ?, ?, ?, ?)";
    db.run(query, [title, author, isbn, year, category], function(err) {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        res.json({ id: this.lastID, message: 'Book added successfully' });
    });
});

app.delete('/api/books/:id', (req, res) => {
    const bookId = req.params.id;
    const query = "DELETE FROM books WHERE id = ?";
    db.run(query, [bookId], function(err) {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        res.json({ message: 'Book deleted successfully' });
    });
});

// Borrow/Return books
app.post('/api/books/:id/borrow', (req, res) => {
    const { studentUid } = req.body;
    const bookId = req.params.id;
    const borrowDate = new Date().toISOString().split('T')[0];
    const dueDate = new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    
    db.get("SELECT * FROM books WHERE id = ? AND is_available = 1", [bookId], (err, book) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        if (!book) {
            return res.status(400).json({ error: 'Book not available' });
        }
        
        const updateBookQuery = "UPDATE books SET is_available = 0, borrowed_by = ?, borrowed_date = ?, due_date = ? WHERE id = ?";
        db.run(updateBookQuery, [studentUid, borrowDate, dueDate, bookId], (err) => {
            if (err) {
                return res.status(500).json({ error: err.message });
            }
            
            // Add to borrowing history
            const historyQuery = "INSERT INTO borrowing_history (student_uid, book_id, borrowed_date, status) VALUES (?, ?, ?, 'borrowed')";
            db.run(historyQuery, [studentUid, bookId, borrowDate]);
            
            // Update student stats
            db.run("UPDATE students SET books_borrowed = books_borrowed + 1, points = points + 10 WHERE uid = ?", [studentUid]);
            
            res.json({ message: 'Book borrowed successfully', dueDate });
        });
    });
});

app.post('/api/books/:id/return', (req, res) => {
    const { studentUid } = req.body;
    const bookId = req.params.id;
    const returnDate = new Date().toISOString().split('T')[0];
    
    const updateBookQuery = "UPDATE books SET is_available = 1, borrowed_by = NULL, borrowed_date = NULL, due_date = NULL WHERE id = ? AND borrowed_by = ?";
    db.run(updateBookQuery, [bookId, studentUid], function(err) {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        if (this.changes === 0) {
            return res.status(400).json({ error: 'Book not found or not borrowed by this student' });
        }
        
        // Update borrowing history
        db.run("UPDATE borrowing_history SET returned_date = ?, status = 'returned' WHERE student_uid = ? AND book_id = ? AND status = 'borrowed'", 
               [returnDate, studentUid, bookId]);
        
        // Update student points
        db.run("UPDATE students SET points = points + 15 WHERE uid = ?", [studentUid]);
        
        res.json({ message: 'Book returned successfully' });
    });
});

// Rate book
app.post('/api/books/:id/rate', (req, res) => {
    const { rating } = req.body;
    const bookId = req.params.id;
    
    const query = "UPDATE books SET rating = ? WHERE id = ?";
    db.run(query, [rating, bookId], function(err) {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        res.json({ message: 'Book rated successfully' });
    });
});

// Student profile
app.get('/api/students/:uid', (req, res) => {
    const uid = req.params.uid;
    
    db.get("SELECT * FROM students WHERE uid = ?", [uid], (err, row) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        if (!row) {
            return res.status(404).json({ error: 'Student not found' });
        }
        res.json(row);
    });
});

// Leaderboard
app.get('/api/leaderboard', (req, res) => {
    const query = "SELECT uid, name, points, books_borrowed FROM students ORDER BY points DESC LIMIT 10";
    db.all(query, [], (err, rows) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        res.json(rows);
    });
});

// Statistics
app.get('/api/stats', (req, res) => {
    const stats = {};
    
    db.get("SELECT COUNT(*) as total_books FROM books", (err, row) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        }
        stats.total_books = row.total_books;
        
        db.get("SELECT COUNT(*) as available_books FROM books WHERE is_available = 1", (err, row) => {
            if (err) {
                return res.status(500).json({ error: err.message });
            }
            stats.available_books = row.available_books;
            stats.borrowed_books = stats.total_books - stats.available_books;
            
            db.get("SELECT COUNT(*) as total_students FROM students", (err, row) => {
                if (err) {
                    return res.status(500).json({ error: err.message });
                }
                stats.total_students = row.total_students;
                res.json(stats);
            });
        });
    });
});

// Export for Vercel
module.exports = async (req, res) => {
    if (!db) {
        await initializeDatabase();
    }
    return app(req, res);
};
