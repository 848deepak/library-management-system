const sqlite3 = require('sqlite3').verbose();
const path = require('path');

const dbPath = path.join(process.cwd(), 'library.db');
const db = new sqlite3.Database(dbPath);

export default function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  if (req.method === 'POST') {
    const { type, username, password, uid } = req.body;

    if (type === 'admin') {
      // Simple admin authentication
      if (password === 'admin123') {
        res.json({ success: true, user: { type: 'admin', name: 'Administrator' } });
      } else {
        res.status(401).json({ success: false, message: 'Invalid admin password' });
      }
    } else if (type === 'student') {
      // Validate UID format and authenticate
      const uidPattern = /^\d{2}[A-Z]{3}\d{5}$/;
      if (!uidPattern.test(uid)) {
        res.status(400).json({ success: false, message: 'Invalid UID format. Use format: YYDEPTnnnnn (e.g., 23BCS12345)' });
        return;
      }

      // Check if student exists, if not create them
      db.get("SELECT * FROM students WHERE uid = ?", [uid], (err, row) => {
        if (err) {
          res.status(500).json({ success: false, message: err.message });
          return;
        }

        if (row) {
          // Student exists
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
            "INSERT INTO students (uid, name, password, points, books_borrowed) VALUES (?, ?, ?, ?, ?)",
            [uid, studentName, 'password', 0, 0],
            function(err) {
              if (err) {
                res.status(500).json({ success: false, message: err.message });
                return;
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
    } else {
      res.status(400).json({ success: false, message: 'Invalid authentication type' });
    }
  } else {
    res.status(405).json({ error: 'Method not allowed' });
  }
}
