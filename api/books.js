const sqlite3 = require('sqlite3').verbose();
const path = require('path');

// Initialize database
const dbPath = path.join(process.cwd(), 'library.db');
const db = new sqlite3.Database(dbPath);

export default function handler(req, res) {
  // Enable CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  if (req.method === 'GET') {
    // Search functionality
    const { q, type } = req.query;
    
    let query = `
      SELECT b.*, s.name as borrowed_by_name 
      FROM books b 
      LEFT JOIN students s ON b.borrowed_by = s.uid
      ORDER BY b.title
    `;
    let params = [];

    if (q && q.trim()) {
      if (type === 'author') {
        query = `
          SELECT b.*, s.name as borrowed_by_name 
          FROM books b 
          LEFT JOIN students s ON b.borrowed_by = s.uid
          WHERE b.author LIKE ? 
          ORDER BY b.title
        `;
        params = [`%${q}%`];
      } else if (type === 'category') {
        query = `
          SELECT b.*, s.name as borrowed_by_name 
          FROM books b 
          LEFT JOIN students s ON b.borrowed_by = s.uid
          WHERE b.category LIKE ? 
          ORDER BY b.title
        `;
        params = [`%${q}%`];
      } else if (type === 'isbn') {
        query = `
          SELECT b.*, s.name as borrowed_by_name 
          FROM books b 
          LEFT JOIN students s ON b.borrowed_by = s.uid
          WHERE b.isbn LIKE ? 
          ORDER BY b.title
        `;
        params = [`%${q}%`];
      } else {
        // Default to title search
        query = `
          SELECT b.*, s.name as borrowed_by_name 
          FROM books b 
          LEFT JOIN students s ON b.borrowed_by = s.uid
          WHERE b.title LIKE ? 
          ORDER BY b.title
        `;
        params = [`%${q}%`];
      }
    }
    
    db.all(query, params, (err, rows) => {
      if (err) {
        res.status(500).json({ error: err.message });
        return;
      }
      res.json(rows);
    });
  } else if (req.method === 'POST') {
    // Add new book
    const { title, author, isbn, year, category } = req.body;
    
    const query = "INSERT INTO books (title, author, isbn, year, category) VALUES (?, ?, ?, ?, ?)";
    db.run(query, [title, author, isbn, year, category], function(err) {
      if (err) {
        res.status(500).json({ error: err.message });
        return;
      }
      res.json({ id: this.lastID, message: 'Book added successfully' });
    });
  } else if (req.method === 'DELETE') {
    // Delete book
    const bookId = req.query.id;
    const query = "DELETE FROM books WHERE id = ?";
    db.run(query, [bookId], function(err) {
      if (err) {
        res.status(500).json({ error: err.message });
        return;
      }
      res.json({ message: 'Book deleted successfully' });
    });
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
