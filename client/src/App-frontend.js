import React, { useState, useEffect } from 'react';
import './App.css';

// Demo data for static deployment with all original features
const INITIAL_BOOKS = [
  { id: 1, title: "The Great Gatsby", author: "F. Scott Fitzgerald", isbn: "978-0-7432-7356-5", year: 1925, category: "Fiction", is_available: true, rating: 4.2, borrowed_by: null, borrowed_by_name: null },
  { id: 2, title: "To Kill a Mockingbird", author: "Harper Lee", isbn: "978-0-06-112008-4", year: 1960, category: "Fiction", is_available: true, rating: 4.5, borrowed_by: null, borrowed_by_name: null },
  { id: 3, title: "1984", author: "George Orwell", isbn: "978-0-452-28423-4", year: 1949, category: "Dystopian", is_available: false, borrowed_by: "23BCS12345", borrowed_by_name: "Student Demo", rating: 4.7 },
  { id: 4, title: "Pride and Prejudice", author: "Jane Austen", isbn: "978-0-14-143951-8", year: 1813, category: "Romance", is_available: true, rating: 4.3, borrowed_by: null, borrowed_by_name: null },
  { id: 5, title: "The Catcher in the Rye", author: "J.D. Salinger", isbn: "978-0-316-76948-0", year: 1951, category: "Fiction", is_available: true, rating: 3.9, borrowed_by: null, borrowed_by_name: null },
  { id: 6, title: "Introduction to Algorithms", author: "Thomas H. Cormen", isbn: "978-0-262-03384-8", year: 2009, category: "Computer Science", is_available: true, rating: 4.8, borrowed_by: null, borrowed_by_name: null },
  { id: 7, title: "Clean Code", author: "Robert C. Martin", isbn: "978-0-13-235088-4", year: 2008, category: "Programming", is_available: false, borrowed_by: "22BCE10001", borrowed_by_name: "Another Student", rating: 4.6 },
  { id: 8, title: "Design Patterns", author: "Gang of Four", isbn: "978-0-201-63361-0", year: 1994, category: "Programming", is_available: true, rating: 4.4, borrowed_by: null, borrowed_by_name: null },
  { id: 9, title: "The Art of Computer Programming", author: "Donald E. Knuth", isbn: "978-0-201-89683-1", year: 1968, category: "Computer Science", is_available: true, rating: 4.9, borrowed_by: null, borrowed_by_name: null },
  { id: 10, title: "Harry Potter and the Sorcerer's Stone", author: "J.K. Rowling", isbn: "978-0-439-70818-8", year: 1997, category: "Fantasy", is_available: true, rating: 4.7, borrowed_by: null, borrowed_by_name: null },
  { id: 11, title: "Data Structures and Algorithms in Java", author: "Robert Lafore", isbn: "978-0-672-32453-5", year: 2002, category: "Computer Science", is_available: true, rating: 4.3, borrowed_by: null, borrowed_by_name: null },
  { id: 12, title: "Effective Java", author: "Joshua Bloch", isbn: "978-0-321-35668-0", year: 2008, category: "Programming", is_available: true, rating: 4.7, borrowed_by: null, borrowed_by_name: null },
  { id: 13, title: "Computer Networks", author: "Andrew S. Tanenbaum", isbn: "978-0-13-212695-3", year: 2010, category: "Computer Science", is_available: true, rating: 4.4, borrowed_by: null, borrowed_by_name: null },
  { id: 14, title: "Operating System Concepts", author: "Abraham Silberschatz", isbn: "978-1-118-06333-0", year: 2012, category: "Computer Science", is_available: true, rating: 4.5, borrowed_by: null, borrowed_by_name: null },
  { id: 15, title: "Database System Concepts", author: "Abraham Silberschatz", isbn: "978-0-07-352332-3", year: 2019, category: "Computer Science", is_available: true, rating: 4.6, borrowed_by: null, borrowed_by_name: null },
  { id: 16, title: "Lord of the Rings", author: "J.R.R. Tolkien", isbn: "978-0-544-00341-5", year: 1954, category: "Fantasy", is_available: true, rating: 4.8, borrowed_by: null, borrowed_by_name: null },
  { id: 17, title: "The Hobbit", author: "J.R.R. Tolkien", isbn: "978-0-547-92822-7", year: 1937, category: "Fantasy", is_available: true, rating: 4.6, borrowed_by: null, borrowed_by_name: null },
  { id: 18, title: "Python Crash Course", author: "Eric Matthes", isbn: "978-1-59327-928-8", year: 2019, category: "Programming", is_available: true, rating: 4.5, borrowed_by: null, borrowed_by_name: null },
  { id: 19, title: "JavaScript: The Good Parts", author: "Douglas Crockford", isbn: "978-0-596-51774-8", year: 2008, category: "Programming", is_available: true, rating: 4.2, borrowed_by: null, borrowed_by_name: null },
  { id: 20, title: "You Don't Know JS", author: "Kyle Simpson", isbn: "978-1-491-92415-0", year: 2014, category: "Programming", is_available: true, rating: 4.7, borrowed_by: null, borrowed_by_name: null }
];

const INITIAL_STUDENTS = [
  { uid: "23BCS12345", name: "Deepak Pandey", points: 85, books_borrowed: 3 },
  { uid: "22BCE10001", name: "Rahul Kumar", points: 120, books_borrowed: 5 },
  { uid: "21BBA10002", name: "Priya Sharma", points: 45, books_borrowed: 2 },
  { uid: "23BCS67890", name: "Arjun Singh", points: 95, books_borrowed: 4 },
  { uid: "22BCA11111", name: "Sneha Patel", points: 70, books_borrowed: 3 }
];

function App() {
  const [currentView, setCurrentView] = useState('login');
  const [currentUser, setCurrentUser] = useState(null);
  const [books, setBooks] = useState([]);
  const [students, setStudents] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchType, setSearchType] = useState('title');
  const [filteredBooks, setFilteredBooks] = useState([]);

  useEffect(() => {
    // Load data from localStorage or use initial data
    const savedBooks = localStorage.getItem('libraryBooks');
    const savedStudents = localStorage.getItem('libraryStudents');
    
    if (savedBooks) {
      setBooks(JSON.parse(savedBooks));
    } else {
      setBooks(INITIAL_BOOKS);
      localStorage.setItem('libraryBooks', JSON.stringify(INITIAL_BOOKS));
    }
    
    if (savedStudents) {
      setStudents(JSON.parse(savedStudents));
    } else {
      setStudents(INITIAL_STUDENTS);
      localStorage.setItem('libraryStudents', JSON.stringify(INITIAL_STUDENTS));
    }
  }, []);

  useEffect(() => {
    filterBooks();
  }, [searchTerm, searchType, books]);

  useEffect(() => {
    // Save to localStorage whenever data changes
    if (books.length > 0) {
      localStorage.setItem('libraryBooks', JSON.stringify(books));
    }
    if (students.length > 0) {
      localStorage.setItem('libraryStudents', JSON.stringify(students));
    }
  }, [books, students]);

  const filterBooks = () => {
    if (!searchTerm.trim()) {
      setFilteredBooks(books);
      return;
    }

    const filtered = books.filter(book => {
      const term = searchTerm.toLowerCase();
      switch (searchType) {
        case 'author':
          return book.author.toLowerCase().includes(term);
        case 'category':
          return book.category.toLowerCase().includes(term);
        case 'isbn':
          return book.isbn.toLowerCase().includes(term);
        default:
          return book.title.toLowerCase().includes(term);
      }
    });
    setFilteredBooks(filtered);
  };

  const handleLogin = (credentials) => {
    if (credentials.type === 'student') {
      const uidPattern = /^\d{2}[A-Z]{3}\d{5}$/;
      if (!uidPattern.test(credentials.uid)) {
        alert('Invalid UID format. Use format: YYDEPTnnnnn (e.g., 23BCS12345)');
        return;
      }

      // Find or create student
      let student = students.find(s => s.uid === credentials.uid);
      if (!student) {
        student = {
          uid: credentials.uid,
          name: `Student ${credentials.uid}`,
          points: 0,
          books_borrowed: 0
        };
        const updatedStudents = [...students, student];
        setStudents(updatedStudents);
      }

      setCurrentUser({ type: 'student', ...student });
      setCurrentView('student');
    } else if (credentials.type === 'admin') {
      if (credentials.password === 'admin123') {
        setCurrentUser({ type: 'admin', name: 'Administrator' });
        setCurrentView('admin');
      } else {
        alert('Invalid admin password');
      }
    }
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setCurrentView('login');
    setSearchTerm('');
  };

  const borrowBook = (bookId) => {
    if (!currentUser || currentUser.type !== 'student') return;

    const updatedBooks = books.map(book => {
      if (book.id === bookId && book.is_available) {
        return {
          ...book,
          is_available: false,
          borrowed_by: currentUser.uid,
          borrowed_by_name: currentUser.name,
          borrowed_date: new Date().toISOString().split('T')[0]
        };
      }
      return book;
    });

    setBooks(updatedBooks);
    
    // Update student points and stats
    const updatedStudents = students.map(student => {
      if (student.uid === currentUser.uid) {
        return {
          ...student,
          points: student.points + 10,
          books_borrowed: student.books_borrowed + 1
        };
      }
      return student;
    });
    setStudents(updatedStudents);
    setCurrentUser({ ...currentUser, points: currentUser.points + 10, books_borrowed: currentUser.books_borrowed + 1 });
    
    alert('Book borrowed successfully! +10 points earned');
  };

  const returnBook = (bookId) => {
    if (!currentUser || currentUser.type !== 'student') return;

    const updatedBooks = books.map(book => {
      if (book.id === bookId && book.borrowed_by === currentUser.uid) {
        return {
          ...book,
          is_available: true,
          borrowed_by: null,
          borrowed_by_name: null,
          borrowed_date: null
        };
      }
      return book;
    });

    setBooks(updatedBooks);
    
    // Update student points
    const updatedStudents = students.map(student => {
      if (student.uid === currentUser.uid) {
        return {
          ...student,
          points: student.points + 15
        };
      }
      return student;
    });
    setStudents(updatedStudents);
    setCurrentUser({ ...currentUser, points: currentUser.points + 15 });
    
    alert('Book returned successfully! +15 points earned');
  };

  const addBook = (bookData) => {
    const newBook = {
      id: Math.max(...books.map(b => b.id)) + 1,
      ...bookData,
      is_available: true,
      rating: 0,
      borrowed_by: null,
      borrowed_by_name: null
    };
    setBooks([...books, newBook]);
    alert('Book added successfully!');
  };

  const deleteBook = (bookId) => {
    if (window.confirm('Are you sure you want to delete this book?')) {
      setBooks(books.filter(book => book.id !== bookId));
      alert('Book deleted successfully!');
    }
  };

  const rateBook = (bookId, rating) => {
    const updatedBooks = books.map(book => {
      if (book.id === bookId) {
        return { ...book, rating: rating };
      }
      return book;
    });
    setBooks(updatedBooks);
    alert(`Book rated ${rating} stars!`);
  };

  const LoginForm = () => {
    const [loginType, setLoginType] = useState('student');
    const [uid, setUid] = useState('');
    const [password, setPassword] = useState('');

    const handleSubmit = (e) => {
      e.preventDefault();
      if (loginType === 'student') {
        handleLogin({ type: 'student', uid });
      } else {
        handleLogin({ type: 'admin', password });
      }
    };

    return (
      <div className="login-container">
        <div className="login-header">
          <h1>📚 Library Management System</h1>
          <p className="university-info">Chandigarh University - Computer Science Lab Project</p>
          <p className="author-info">Developed by: <strong>Deepak Pandey</strong> | 3rd Semester</p>
          <p className="demo-note">🌟 Original Full-Stack Application - All features functional!</p>
        </div>
        
        <form onSubmit={handleSubmit} className="login-form">
          <div className="login-tabs">
            <button
              type="button"
              className={loginType === 'student' ? 'active' : ''}
              onClick={() => setLoginType('student')}
            >
              👨‍🎓 Student Login
            </button>
            <button
              type="button"
              className={loginType === 'admin' ? 'active' : ''}
              onClick={() => setLoginType('admin')}
            >
              👨‍💼 Admin Login
            </button>
          </div>

          {loginType === 'student' ? (
            <div className="form-group">
              <label>Student UID:</label>
              <input
                type="text"
                value={uid}
                onChange={(e) => setUid(e.target.value)}
                placeholder="e.g., 23BCS12345"
                required
              />
              <small>Format: YYDEPTnnnnn (Year + Department + Roll Number)</small>
            </div>
          ) : (
            <div className="form-group">
              <label>Admin Password:</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter admin password"
                required
              />
              <small>Demo password: admin123</small>
            </div>
          )}

          <button type="submit" className="login-btn">
            {loginType === 'student' ? '🎓 Login as Student' : '💼 Login as Admin'}
          </button>
        </form>

        <div className="demo-info">
          <h3>🚀 Academic Project Features:</h3>
          <div className="features-grid">
            <div className="feature-card">
              <h4>👨‍🎓 Student Features</h4>
              <ul>
                <li>UID-based authentication</li>
                <li>Browse & search library catalog</li>
                <li>Borrow and return books</li>
                <li>Rate books (1-5 stars)</li>
                <li>View personal statistics</li>
                <li>Gamification with points</li>
                <li>Student leaderboard</li>
              </ul>
            </div>
            <div className="feature-card">
              <h4>👨‍💼 Admin Features</h4>
              <ul>
                <li>Complete book management</li>
                <li>Add/delete books</li>
                <li>Monitor borrowing activity</li>
                <li>View library statistics</li>
                <li>Student management</li>
                <li>Real-time analytics</li>
              </ul>
            </div>
          </div>
          
          <div className="demo-credentials">
            <div className="credential-section">
              <strong>👨‍🎓 Student Login (Try these UIDs):</strong>
              <div className="uid-examples">
                <span className="uid-badge">23BCS12345</span>
                <span className="uid-badge">22BCE10001</span>
                <span className="uid-badge">21BBA10002</span>
                <span className="uid-badge">23BCS67890</span>
                <span className="uid-badge">22BCA11111</span>
              </div>
            </div>
            <div className="credential-section">
              <strong>👨‍💼 Admin Login:</strong>
              <div className="admin-creds">
                <span className="password-badge">Password: admin123</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  };

  const StudentDashboard = () => {
    const myBooks = books.filter(book => book.borrowed_by === currentUser.uid);
    const myBorrowedBooks = filteredBooks.filter(book => book.borrowed_by === currentUser.uid);
    const displayBooks = searchTerm || myBorrowedBooks.length === 0 ? filteredBooks : myBorrowedBooks;

    return (
      <div className="dashboard">
        <div className="dashboard-header">
          <h2>👨‍🎓 Student Dashboard</h2>
          <div className="user-info">
            <span className="welcome">Welcome, {currentUser.name}!</span>
            <span className="points">⭐ {currentUser.points} points</span>
            <span className="books-count">📚 {currentUser.books_borrowed} books borrowed</span>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </div>
        </div>

        <div className="search-section">
          <div className="search-controls">
            <input
              type="text"
              placeholder="Search books..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
            <select value={searchType} onChange={(e) => setSearchType(e.target.value)} className="search-select">
              <option value="title">By Title</option>
              <option value="author">By Author</option>
              <option value="category">By Category</option>
              <option value="isbn">By ISBN</option>
            </select>
          </div>
        </div>

        <div className="stats-section">
          <div className="stat-card">
            <h3>📊 Library Statistics</h3>
            <p>Total Books: <strong>{books.length}</strong></p>
            <p>Available: <strong>{books.filter(b => b.is_available).length}</strong></p>
            <p>Borrowed: <strong>{books.filter(b => !b.is_available).length}</strong></p>
          </div>
          <div className="stat-card">
            <h3>🏆 Your Statistics</h3>
            <p>Books Borrowed: <strong>{myBooks.length}</strong></p>
            <p>Points Earned: <strong>{currentUser.points}</strong></p>
            <p>Your Rank: <strong>#{students.sort((a, b) => b.points - a.points).findIndex(s => s.uid === currentUser.uid) + 1}</strong></p>
          </div>
        </div>

        <div className="books-section">
          <h3>📚 {searchTerm ? `Search Results (${displayBooks.length})` : myBorrowedBooks.length > 0 ? 'Your Borrowed Books' : 'Available Books'}</h3>
          <div className="books-grid">
            {displayBooks.map(book => (
              <div key={book.id} className={`book-card ${!book.is_available ? 'borrowed' : ''}`}>
                <div className="book-header">
                  <h4>{book.title}</h4>
                  {book.rating > 0 && <div className="rating">⭐ {book.rating}/5</div>}
                </div>
                <div className="book-details">
                  <p><strong>Author:</strong> {book.author}</p>
                  <p><strong>Category:</strong> {book.category}</p>
                  <p><strong>Year:</strong> {book.year}</p>
                  <p><strong>ISBN:</strong> {book.isbn}</p>
                </div>
                
                <div className="book-actions">
                  {book.is_available ? (
                    <button 
                      onClick={() => borrowBook(book.id)}
                      className="action-btn borrow-btn"
                    >
                      📖 Borrow Book
                    </button>
                  ) : book.borrowed_by === currentUser.uid ? (
                    <div className="borrowed-actions">
                      <p className="borrowed-status">📖 You borrowed this book</p>
                      <div className="action-buttons">
                        <button 
                          onClick={() => returnBook(book.id)}
                          className="action-btn return-btn"
                        >
                          ↩️ Return Book
                        </button>
                        <button 
                          onClick={() => {
                            const rating = prompt('Rate this book (1-5 stars):');
                            if (rating && rating >= 1 && rating <= 5) {
                              rateBook(book.id, parseFloat(rating));
                            }
                          }}
                          className="action-btn rate-btn"
                        >
                          ⭐ Rate Book
                        </button>
                      </div>
                    </div>
                  ) : (
                    <div className="borrowed-info">
                      <p className="borrowed-status">📖 Borrowed by {book.borrowed_by_name}</p>
                      <p className="borrowed-uid">UID: {book.borrowed_by}</p>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="leaderboard-section">
          <h3>🏆 Student Leaderboard</h3>
          <div className="leaderboard">
            {students.sort((a, b) => b.points - a.points).map((student, index) => (
              <div key={student.uid} className={`leaderboard-item ${student.uid === currentUser.uid ? 'current-user' : ''}`}>
                <span className="rank">#{index + 1}</span>
                <span className="name">{student.name}</span>
                <span className="uid">{student.uid}</span>
                <span className="points">{student.points} pts</span>
                <span className="books">{student.books_borrowed} books</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  };

  const AdminDashboard = () => {
    const [showAddForm, setShowAddForm] = useState(false);
    const [newBook, setNewBook] = useState({
      title: '',
      author: '',
      isbn: '',
      year: '',
      category: ''
    });

    const handleAddBook = (e) => {
      e.preventDefault();
      addBook({
        ...newBook,
        year: parseInt(newBook.year) || new Date().getFullYear()
      });
      setNewBook({ title: '', author: '', isbn: '', year: '', category: '' });
      setShowAddForm(false);
    };

    return (
      <div className="dashboard">
        <div className="dashboard-header">
          <h2>👨‍💼 Admin Dashboard</h2>
          <div className="user-info">
            <span className="welcome">Welcome, {currentUser.name}!</span>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </div>
        </div>

        <div className="admin-stats">
          <div className="stat-card">
            <h3>📊 Library Overview</h3>
            <p>Total Books: <strong>{books.length}</strong></p>
            <p>Available: <strong>{books.filter(b => b.is_available).length}</strong></p>
            <p>Borrowed: <strong>{books.filter(b => !b.is_available).length}</strong></p>
            <p>Total Students: <strong>{students.length}</strong></p>
          </div>
          <div className="stat-card">
            <h3>📈 Activity Statistics</h3>
            <p>Most Active Student: <strong>{students.sort((a, b) => b.points - a.points)[0]?.name || 'N/A'}</strong></p>
            <p>Total Points Awarded: <strong>{students.reduce((sum, s) => sum + s.points, 0)}</strong></p>
            <p>Total Borrows: <strong>{students.reduce((sum, s) => sum + s.books_borrowed, 0)}</strong></p>
            <p>Average Rating: <strong>{(books.filter(b => b.rating > 0).reduce((sum, b) => sum + b.rating, 0) / books.filter(b => b.rating > 0).length || 0).toFixed(1)}</strong></p>
          </div>
        </div>

        <div className="admin-actions">
          <button 
            onClick={() => setShowAddForm(!showAddForm)}
            className={`action-btn ${showAddForm ? 'cancel-btn' : 'add-btn'}`}
          >
            {showAddForm ? '❌ Cancel' : '➕ Add New Book'}
          </button>
        </div>

        {showAddForm && (
          <div className="add-book-section">
            <form onSubmit={handleAddBook} className="add-book-form">
              <h3>➕ Add New Book</h3>
              <div className="form-grid">
                <input
                  type="text"
                  placeholder="Book Title *"
                  value={newBook.title}
                  onChange={(e) => setNewBook({...newBook, title: e.target.value})}
                  required
                />
                <input
                  type="text"
                  placeholder="Author Name *"
                  value={newBook.author}
                  onChange={(e) => setNewBook({...newBook, author: e.target.value})}
                  required
                />
                <input
                  type="text"
                  placeholder="ISBN *"
                  value={newBook.isbn}
                  onChange={(e) => setNewBook({...newBook, isbn: e.target.value})}
                  required
                />
                <input
                  type="number"
                  placeholder="Publication Year"
                  value={newBook.year}
                  onChange={(e) => setNewBook({...newBook, year: e.target.value})}
                />
                <input
                  type="text"
                  placeholder="Category *"
                  value={newBook.category}
                  onChange={(e) => setNewBook({...newBook, category: e.target.value})}
                  required
                />
              </div>
              <button type="submit" className="submit-btn">📚 Add Book to Library</button>
            </form>
          </div>
        )}

        <div className="search-section">
          <div className="search-controls">
            <input
              type="text"
              placeholder="Search books..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
            <select value={searchType} onChange={(e) => setSearchType(e.target.value)} className="search-select">
              <option value="title">By Title</option>
              <option value="author">By Author</option>
              <option value="category">By Category</option>
              <option value="isbn">By ISBN</option>
            </select>
          </div>
        </div>

        <div className="books-section">
          <h3>📚 All Books ({filteredBooks.length} total)</h3>
          <div className="books-grid">
            {filteredBooks.map(book => (
              <div key={book.id} className={`book-card admin-view ${!book.is_available ? 'borrowed' : ''}`}>
                <div className="book-header">
                  <h4>{book.title}</h4>
                  {book.rating > 0 && <div className="rating">⭐ {book.rating}/5</div>}
                </div>
                <div className="book-details">
                  <p><strong>Author:</strong> {book.author}</p>
                  <p><strong>Category:</strong> {book.category}</p>
                  <p><strong>Year:</strong> {book.year}</p>
                  <p><strong>ISBN:</strong> {book.isbn}</p>
                </div>
                
                <div className="book-status">
                  {book.is_available ? (
                    <span className="status available">✅ Available</span>
                  ) : (
                    <div className="status borrowed">
                      <span>📖 Borrowed</span>
                      <small>by {book.borrowed_by_name} ({book.borrowed_by})</small>
                      {book.borrowed_date && <small>on {book.borrowed_date}</small>}
                    </div>
                  )}
                </div>
                
                <div className="admin-actions">
                  <button 
                    onClick={() => deleteBook(book.id)}
                    className="action-btn delete-btn"
                  >
                    🗑️ Delete Book
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="students-section">
          <h3>👥 Student Management ({students.length} students)</h3>
          <div className="students-grid">
            {students.sort((a, b) => b.points - a.points).map((student, index) => (
              <div key={student.uid} className="student-card">
                <div className="student-rank">#{index + 1}</div>
                <div className="student-info">
                  <h4>{student.name}</h4>
                  <p><strong>UID:</strong> {student.uid}</p>
                  <p><strong>Points:</strong> {student.points}</p>
                  <p><strong>Books Borrowed:</strong> {student.books_borrowed}</p>
                </div>
                <div className="student-books">
                  <strong>Currently Borrowed:</strong>
                  {books.filter(b => b.borrowed_by === student.uid).map(book => (
                    <div key={book.id} className="borrowed-book-item">
                      <span>{book.title}</span>
                    </div>
                  ))}
                  {books.filter(b => b.borrowed_by === student.uid).length === 0 && <span className="no-books">No books borrowed</span>}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="App">
      {currentView === 'login' && <LoginForm />}
      {currentView === 'student' && <StudentDashboard />}
      {currentView === 'admin' && <AdminDashboard />}
    </div>
  );
}

export default App;
