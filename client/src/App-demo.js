import React, { useState, useEffect } from 'react';
import './App.css';

// Demo data for static deployment
const DEMO_BOOKS = [
  { id: 1, title: "The Great Gatsby", author: "F. Scott Fitzgerald", isbn: "978-0-7432-7356-5", year: 1925, category: "Fiction", is_available: true, rating: 4.2 },
  { id: 2, title: "To Kill a Mockingbird", author: "Harper Lee", isbn: "978-0-06-112008-4", year: 1960, category: "Fiction", is_available: true, rating: 4.5 },
  { id: 3, title: "1984", author: "George Orwell", isbn: "978-0-452-28423-4", year: 1949, category: "Dystopian", is_available: false, borrowed_by: "23BCS12345", borrowed_by_name: "Student Demo", rating: 4.7 },
  { id: 4, title: "Pride and Prejudice", author: "Jane Austen", isbn: "978-0-14-143951-8", year: 1813, category: "Romance", is_available: true, rating: 4.3 },
  { id: 5, title: "The Catcher in the Rye", author: "J.D. Salinger", isbn: "978-0-316-76948-0", year: 1951, category: "Fiction", is_available: true, rating: 3.9 },
  { id: 6, title: "Introduction to Algorithms", author: "Thomas H. Cormen", isbn: "978-0-262-03384-8", year: 2009, category: "Computer Science", is_available: true, rating: 4.8 },
  { id: 7, title: "Clean Code", author: "Robert C. Martin", isbn: "978-0-13-235088-4", year: 2008, category: "Programming", is_available: false, borrowed_by: "22BCE10001", borrowed_by_name: "Another Student", rating: 4.6 },
  { id: 8, title: "Design Patterns", author: "Gang of Four", isbn: "978-0-201-63361-0", year: 1994, category: "Programming", is_available: true, rating: 4.4 },
  { id: 9, title: "The Art of Computer Programming", author: "Donald E. Knuth", isbn: "978-0-201-89683-1", year: 1968, category: "Computer Science", is_available: true, rating: 4.9 },
  { id: 10, title: "Harry Potter and the Sorcerer's Stone", author: "J.K. Rowling", isbn: "978-0-439-70818-8", year: 1997, category: "Fantasy", is_available: true, rating: 4.7 }
];

const DEMO_STUDENTS = [
  { uid: "23BCS12345", name: "Student Demo", points: 85, books_borrowed: 3 },
  { uid: "22BCE10001", name: "Another Student", points: 120, books_borrowed: 5 },
  { uid: "21BBA10002", name: "Third Student", points: 45, books_borrowed: 2 }
];

function App() {
  const [currentView, setCurrentView] = useState('login');
  const [currentUser, setCurrentUser] = useState(null);
  const [books, setBooks] = useState(DEMO_BOOKS);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchType, setSearchType] = useState('title');
  const [filteredBooks, setFilteredBooks] = useState(DEMO_BOOKS);
  const [students, setStudents] = useState(DEMO_STUDENTS);

  useEffect(() => {
    filterBooks();
  }, [searchTerm, searchType, books]);

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
        setStudents([...students, student]);
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
          borrowed_by_name: currentUser.name
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
          points: student.points + 10,
          books_borrowed: student.books_borrowed + 1
        };
      }
      return student;
    });
    setStudents(updatedStudents);
    setCurrentUser({ ...currentUser, points: currentUser.points + 10, books_borrowed: currentUser.books_borrowed + 1 });
    
    alert('Book borrowed successfully! +10 points');
  };

  const returnBook = (bookId) => {
    if (!currentUser || currentUser.type !== 'student') return;

    const updatedBooks = books.map(book => {
      if (book.id === bookId && book.borrowed_by === currentUser.uid) {
        return {
          ...book,
          is_available: true,
          borrowed_by: null,
          borrowed_by_name: null
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
    
    alert('Book returned successfully! +15 points');
  };

  const addBook = (bookData) => {
    const newBook = {
      id: Math.max(...books.map(b => b.id)) + 1,
      ...bookData,
      is_available: true,
      rating: 0
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
          <p>Chandigarh University - Academic Project</p>
          <p className="demo-note">🌟 Live Demo Version - All features functional!</p>
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
          <h3>🚀 Try the Demo:</h3>
          <div className="demo-credentials">
            <div>
              <strong>👨‍🎓 Student Login:</strong>
              <ul>
                <li>UID: 23BCS12345 (or any valid format)</li>
                <li>UID: 22BCE10001</li>
                <li>UID: 21BBA10002</li>
              </ul>
            </div>
            <div>
              <strong>👨‍💼 Admin Login:</strong>
              <ul>
                <li>Password: admin123</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    );
  };

  const StudentDashboard = () => {
    const myBooks = books.filter(book => book.borrowed_by === currentUser.uid);
    const availableBooks = filteredBooks.filter(book => book.is_available);
    const myBorrowedBooks = filteredBooks.filter(book => book.borrowed_by === currentUser.uid);
    const displayBooks = searchTerm || myBorrowedBooks.length === 0 ? filteredBooks : myBorrowedBooks;

    return (
      <div className="dashboard">
        <div className="dashboard-header">
          <h2>👨‍🎓 Student Dashboard</h2>
          <div className="user-info">
            <span>Welcome, {currentUser.name}!</span>
            <span className="points">⭐ {currentUser.points} points</span>
            <span>📚 {currentUser.books_borrowed} books borrowed</span>
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
            <select value={searchType} onChange={(e) => setSearchType(e.target.value)}>
              <option value="title">Title</option>
              <option value="author">Author</option>
              <option value="category">Category</option>
              <option value="isbn">ISBN</option>
            </select>
          </div>
        </div>

        <div className="stats-section">
          <div className="stat-card">
            <h3>📊 Library Stats</h3>
            <p>Total Books: {books.length}</p>
            <p>Available: {books.filter(b => b.is_available).length}</p>
            <p>Borrowed: {books.filter(b => !b.is_available).length}</p>
          </div>
          <div className="stat-card">
            <h3>🏆 Your Stats</h3>
            <p>Books Borrowed: {myBooks.length}</p>
            <p>Points Earned: {currentUser.points}</p>
            <p>Rank: #{students.sort((a, b) => b.points - a.points).findIndex(s => s.uid === currentUser.uid) + 1}</p>
          </div>
        </div>

        <div className="books-section">
          <h3>📚 {searchTerm ? 'Search Results' : myBorrowedBooks.length > 0 ? 'Your Borrowed Books' : 'Available Books'}</h3>
          <div className="books-grid">
            {displayBooks.map(book => (
              <div key={book.id} className={`book-card ${!book.is_available ? 'borrowed' : ''}`}>
                <h4>{book.title}</h4>
                <p><strong>Author:</strong> {book.author}</p>
                <p><strong>Category:</strong> {book.category}</p>
                <p><strong>Year:</strong> {book.year}</p>
                <p><strong>ISBN:</strong> {book.isbn}</p>
                {book.rating > 0 && <p><strong>Rating:</strong> ⭐ {book.rating}/5</p>}
                
                {book.is_available ? (
                  <button 
                    onClick={() => borrowBook(book.id)}
                    className="action-btn borrow-btn"
                  >
                    📖 Borrow Book
                  </button>
                ) : book.borrowed_by === currentUser.uid ? (
                  <div>
                    <p className="borrowed-status">📖 You borrowed this book</p>
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
                ) : (
                  <p className="borrowed-status">📖 Borrowed by {book.borrowed_by_name}</p>
                )}
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
            <span>Welcome, {currentUser.name}!</span>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </div>
        </div>

        <div className="admin-stats">
          <div className="stat-card">
            <h3>📊 Library Overview</h3>
            <p>Total Books: {books.length}</p>
            <p>Available: {books.filter(b => b.is_available).length}</p>
            <p>Borrowed: {books.filter(b => !b.is_available).length}</p>
            <p>Total Students: {students.length}</p>
          </div>
          <div className="stat-card">
            <h3>📈 Activity</h3>
            <p>Most Active: {students.sort((a, b) => b.points - a.points)[0]?.name || 'N/A'}</p>
            <p>Total Points Awarded: {students.reduce((sum, s) => sum + s.points, 0)}</p>
            <p>Total Borrows: {students.reduce((sum, s) => sum + s.books_borrowed, 0)}</p>
          </div>
        </div>

        <div className="admin-actions">
          <button 
            onClick={() => setShowAddForm(!showAddForm)}
            className="action-btn add-btn"
          >
            {showAddForm ? '❌ Cancel' : '➕ Add New Book'}
          </button>
        </div>

        {showAddForm && (
          <form onSubmit={handleAddBook} className="add-book-form">
            <h3>➕ Add New Book</h3>
            <div className="form-grid">
              <input
                type="text"
                placeholder="Title"
                value={newBook.title}
                onChange={(e) => setNewBook({...newBook, title: e.target.value})}
                required
              />
              <input
                type="text"
                placeholder="Author"
                value={newBook.author}
                onChange={(e) => setNewBook({...newBook, author: e.target.value})}
                required
              />
              <input
                type="text"
                placeholder="ISBN"
                value={newBook.isbn}
                onChange={(e) => setNewBook({...newBook, isbn: e.target.value})}
                required
              />
              <input
                type="number"
                placeholder="Year"
                value={newBook.year}
                onChange={(e) => setNewBook({...newBook, year: e.target.value})}
              />
              <input
                type="text"
                placeholder="Category"
                value={newBook.category}
                onChange={(e) => setNewBook({...newBook, category: e.target.value})}
                required
              />
            </div>
            <button type="submit" className="submit-btn">📚 Add Book</button>
          </form>
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
            <select value={searchType} onChange={(e) => setSearchType(e.target.value)}>
              <option value="title">Title</option>
              <option value="author">Author</option>
              <option value="category">Category</option>
              <option value="isbn">ISBN</option>
            </select>
          </div>
        </div>

        <div className="books-section">
          <h3>📚 All Books ({filteredBooks.length})</h3>
          <div className="books-grid">
            {filteredBooks.map(book => (
              <div key={book.id} className={`book-card ${!book.is_available ? 'borrowed' : ''}`}>
                <h4>{book.title}</h4>
                <p><strong>Author:</strong> {book.author}</p>
                <p><strong>Category:</strong> {book.category}</p>
                <p><strong>Year:</strong> {book.year}</p>
                <p><strong>ISBN:</strong> {book.isbn}</p>
                {book.rating > 0 && <p><strong>Rating:</strong> ⭐ {book.rating}/5</p>}
                
                <div className="book-status">
                  {book.is_available ? (
                    <span className="status available">✅ Available</span>
                  ) : (
                    <span className="status borrowed">📖 Borrowed by {book.borrowed_by_name}</span>
                  )}
                </div>
                
                <button 
                  onClick={() => deleteBook(book.id)}
                  className="action-btn delete-btn"
                >
                  🗑️ Delete
                </button>
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
