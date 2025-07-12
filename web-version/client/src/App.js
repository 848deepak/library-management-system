import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [currentView, setCurrentView] = useState('login');
  const [userType, setUserType] = useState(null);
  const [currentUser, setCurrentUser] = useState(null);
  const [books, setBooks] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [searchType, setSearchType] = useState('title');
  const [stats, setStats] = useState({});
  const [leaderboard, setLeaderboard] = useState([]);

  useEffect(() => {
    fetchBooks();
    fetchStats();
  }, []);

  const fetchBooks = async () => {
    try {
      const response = await fetch('/api/books');
      const data = await response.json();
      setBooks(data);
    } catch (error) {
      console.error('Error fetching books:', error);
    }
  };

  const fetchStats = async () => {
    try {
      const response = await fetch('/api/stats');
      const data = await response.json();
      setStats(data);
    } catch (error) {
      console.error('Error fetching stats:', error);
    }
  };

  const fetchLeaderboard = async () => {
    try {
      const response = await fetch('/api/leaderboard');
      const data = await response.json();
      setLeaderboard(data);
    } catch (error) {
      console.error('Error fetching leaderboard:', error);
    }
  };

  const handleLogin = async (credentials) => {
    if (credentials.type === 'student') {
      try {
        const response = await fetch('/api/auth/student', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ uid: credentials.uid })
        });
        
        if (response.ok) {
          const userData = await response.json();
          setCurrentUser(userData);
          setUserType('student');
          setCurrentView('student');
        } else {
          const error = await response.json();
          alert(error.error);
        }
      } catch (error) {
        alert('Login failed: ' + error.message);
      }
    } else if (credentials.type === 'admin') {
      try {
        const response = await fetch('/api/auth/admin', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ password: credentials.password })
        });
        
        if (response.ok) {
          setUserType('admin');
          setCurrentView('admin');
        } else {
          alert('Invalid admin password');
        }
      } catch (error) {
        alert('Login failed: ' + error.message);
      }
    }
  };

  const handleLogout = () => {
    setCurrentUser(null);
    setUserType(null);
    setCurrentView('login');
  };

  const searchBooks = async () => {
    if (!searchTerm.trim()) {
      fetchBooks();
      return;
    }
    
    try {
      const response = await fetch(`/api/books/search?q=${encodeURIComponent(searchTerm)}&type=${searchType}`);
      const data = await response.json();
      setBooks(data);
    } catch (error) {
      console.error('Error searching books:', error);
    }
  };

  const borrowBook = async (bookId) => {
    try {
      const response = await fetch(`/api/books/${bookId}/borrow`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ studentUid: currentUser.uid })
      });
      
      if (response.ok) {
        alert('Book borrowed successfully!');
        fetchBooks();
        fetchStats();
      } else {
        const error = await response.json();
        alert(error.error);
      }
    } catch (error) {
      alert('Error borrowing book: ' + error.message);
    }
  };

  const returnBook = async (bookId) => {
    try {
      const response = await fetch(`/api/books/${bookId}/return`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ studentUid: currentUser.uid })
      });
      
      if (response.ok) {
        alert('Book returned successfully!');
        fetchBooks();
        fetchStats();
      } else {
        const error = await response.json();
        alert(error.error);
      }
    } catch (error) {
      alert('Error returning book: ' + error.message);
    }
  };

  const rateBook = async (bookId, rating) => {
    try {
      const response = await fetch(`/api/books/${bookId}/rate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rating: parseInt(rating) })
      });
      
      if (response.ok) {
        alert('Book rated successfully!');
        fetchBooks();
      } else {
        const error = await response.json();
        alert(error.error);
      }
    } catch (error) {
      alert('Error rating book: ' + error.message);
    }
  };

  const addBook = async (bookData) => {
    try {
      const response = await fetch('/api/books', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(bookData)
      });
      
      if (response.ok) {
        alert('Book added successfully!');
        fetchBooks();
        fetchStats();
      } else {
        const error = await response.json();
        alert(error.error);
      }
    } catch (error) {
      alert('Error adding book: ' + error.message);
    }
  };

  const deleteBook = async (bookId) => {
    if (window.confirm('Are you sure you want to delete this book?')) {
      try {
        const response = await fetch(`/api/books/${bookId}`, {
          method: 'DELETE'
        });
        
        if (response.ok) {
          alert('Book deleted successfully!');
          fetchBooks();
          fetchStats();
        } else {
          const error = await response.json();
          alert(error.error);
        }
      } catch (error) {
        alert('Error deleting book: ' + error.message);
      }
    }
  };

  if (currentView === 'login') {
    return <LoginView onLogin={handleLogin} />;
  }

  if (currentView === 'admin') {
    return (
      <AdminView
        books={books}
        stats={stats}
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        searchType={searchType}
        setSearchType={setSearchType}
        onSearch={searchBooks}
        onAddBook={addBook}
        onDeleteBook={deleteBook}
        onLogout={handleLogout}
      />
    );
  }

  if (currentView === 'student') {
    return (
      <StudentView
        books={books}
        currentUser={currentUser}
        stats={stats}
        leaderboard={leaderboard}
        searchTerm={searchTerm}
        setSearchTerm={setSearchTerm}
        searchType={searchType}
        setSearchType={setSearchType}
        onSearch={searchBooks}
        onBorrow={borrowBook}
        onReturn={returnBook}
        onRate={rateBook}
        onShowLeaderboard={() => {
          fetchLeaderboard();
          setCurrentView('leaderboard');
        }}
        onLogout={handleLogout}
      />
    );
  }

  if (currentView === 'leaderboard') {
    return (
      <LeaderboardView
        leaderboard={leaderboard}
        currentUser={currentUser}
        onBack={() => setCurrentView('student')}
      />
    );
  }

  return null;
}

// Login Component
const LoginView = ({ onLogin }) => {
  const [loginType, setLoginType] = useState('student');
  const [uid, setUid] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (loginType === 'student' && uid.trim()) {
      onLogin({ type: 'student', uid: uid.trim() });
    } else if (loginType === 'admin' && password.trim()) {
      onLogin({ type: 'admin', password: password.trim() });
    }
  };

  const handleTabChange = (type) => {
    setLoginType(type);
    setUid('');
    setPassword('');
  };

  return (
    <div className="login-container">
      <div className="login-form">
        <h1>📚 Library Management System</h1>
        <div className="login-tabs">
          <button 
            className={loginType === 'student' ? 'active' : ''}
            onClick={() => handleTabChange('student')}
          >
            Student
          </button>
          <button 
            className={loginType === 'admin' ? 'active' : ''}
            onClick={() => handleTabChange('admin')}
          >
            Admin
          </button>
        </div>
        
        <form onSubmit={handleSubmit}>
          {loginType === 'student' ? (
            <div className="form-group">
              <label>Enter Your Student UID</label>
              <input
                type="text"
                value={uid}
                onChange={(e) => setUid(e.target.value)}
                placeholder="e.g., 23BCS12345"
                required
                autoFocus
              />
              <small>Format: YYDEPTnnnnn (Year + Department + 5 digits)</small>
              <div className="examples">
                <span>Examples: 23BCS12345, 22BCE10001, 21BBA10002</span>
              </div>
            </div>
          ) : (
            <div className="form-group">
              <label>Enter Admin Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Enter admin password"
                required
                autoFocus
              />
              <small>Use the default admin password</small>
              <div className="examples">
                <span>Default: admin123</span>
              </div>
            </div>
          )}
          <button type="submit" className="login-btn">
            {loginType === 'student' ? 'Student Login' : 'Admin Login'}
          </button>
        </form>
        
        <div className="quick-login">
          <p>Quick Login:</p>
          <div className="quick-buttons">
            <button 
              type="button" 
              className="quick-btn student-quick"
              onClick={() => {
                setLoginType('student');
                setUid('23BCS12345');
              }}
            >
              Demo Student
            </button>
            <button 
              type="button" 
              className="quick-btn admin-quick"
              onClick={() => {
                setLoginType('admin');
                setPassword('admin123');
              }}
            >
              Demo Admin
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

// Admin View Component
const AdminView = ({ books, stats, searchTerm, setSearchTerm, searchType, setSearchType, onSearch, onAddBook, onDeleteBook, onLogout }) => {
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
    onAddBook(newBook);
    setNewBook({ title: '', author: '', isbn: '', year: '', category: '' });
    setShowAddForm(false);
  };

  return (
    <div className="admin-container">
      <header>
        <h1>📚 Library Management - Admin Panel</h1>
        <button onClick={onLogout} className="logout-btn">Logout</button>
      </header>

      <div className="stats-panel">
        <div className="stat-card">
          <h3>Total Books</h3>
          <span>{stats.totalBooks || 0}</span>
        </div>
        <div className="stat-card">
          <h3>Available</h3>
          <span>{stats.availableBooks || 0}</span>
        </div>
        <div className="stat-card">
          <h3>Borrowed</h3>
          <span>{stats.borrowedBooks || 0}</span>
        </div>
        <div className="stat-card">
          <h3>Students</h3>
          <span>{stats.totalStudents || 0}</span>
        </div>
      </div>

      <div className="controls">
        <div className="search-section">
          <select value={searchType} onChange={(e) => setSearchType(e.target.value)}>
            <option value="title">Title</option>
            <option value="author">Author</option>
            <option value="category">Category</option>
            <option value="isbn">ISBN</option>
          </select>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search books..."
          />
          <button onClick={onSearch}>Search</button>
        </div>
        <button onClick={() => setShowAddForm(true)} className="add-btn">Add New Book</button>
      </div>

      {showAddForm && (
        <div className="modal">
          <div className="modal-content">
            <h3>Add New Book</h3>
            <form onSubmit={handleAddBook}>
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
              />
              <div className="form-buttons">
                <button type="submit">Add Book</button>
                <button type="button" onClick={() => setShowAddForm(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="books-table">
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Author</th>
              <th>Category</th>
              <th>Year</th>
              <th>Status</th>
              <th>Borrowed By</th>
              <th>Rating</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {books.map(book => (
              <tr key={book.id}>
                <td>{book.title}</td>
                <td>{book.author}</td>
                <td>{book.category}</td>
                <td>{book.year}</td>
                <td className={book.available ? 'available' : 'borrowed'}>
                  {book.available ? 'Available' : 'Borrowed'}
                </td>
                <td>{book.borrowed_by_name || '-'}</td>
                <td>
                  {book.rating > 0 ? `${book.rating.toFixed(1)} (${book.rating_count})` : 'No ratings'}
                </td>
                <td>
                  <button onClick={() => onDeleteBook(book.id)} className="delete-btn">Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

// Student View Component
const StudentView = ({ books, currentUser, stats, searchTerm, setSearchTerm, searchType, setSearchType, onSearch, onBorrow, onReturn, onRate, onShowLeaderboard, onLogout }) => {
  const [showRateModal, setShowRateModal] = useState(false);
  const [selectedBook, setSelectedBook] = useState(null);
  const [rating, setRating] = useState(5);

  const handleRate = (book) => {
    setSelectedBook(book);
    setShowRateModal(true);
  };

  const submitRating = () => {
    onRate(selectedBook.id, rating);
    setShowRateModal(false);
    setSelectedBook(null);
  };

  return (
    <div className="student-container">
      <header>
        <h1>📚 Library Management System</h1>
        <div className="user-info">
          <span>Welcome, {currentUser.name} ({currentUser.uid})</span>
          <span>Points: {currentUser.points || 0}</span>
          <button onClick={onLogout} className="logout-btn">Logout</button>
        </div>
      </header>

      <div className="stats-panel">
        <div className="stat-card">
          <h3>Total Books</h3>
          <span>{stats.totalBooks || 0}</span>
        </div>
        <div className="stat-card">
          <h3>Available</h3>
          <span>{stats.availableBooks || 0}</span>
        </div>
        <div className="stat-card">
          <h3>Borrowed</h3>
          <span>{stats.borrowedBooks || 0}</span>
        </div>
      </div>

      <div className="controls">
        <div className="search-section">
          <select value={searchType} onChange={(e) => setSearchType(e.target.value)}>
            <option value="title">Title</option>
            <option value="author">Author</option>
            <option value="category">Category</option>
          </select>
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search books..."
          />
          <button onClick={onSearch}>Search</button>
        </div>
        <button onClick={onShowLeaderboard} className="leaderboard-btn">View Leaderboard</button>
      </div>

      {showRateModal && (
        <div className="modal">
          <div className="modal-content">
            <h3>Rate Book: {selectedBook.title}</h3>
            <div className="rating-section">
              <label>Rating (1-5 stars):</label>
              <select value={rating} onChange={(e) => setRating(e.target.value)}>
                <option value="1">1 Star</option>
                <option value="2">2 Stars</option>
                <option value="3">3 Stars</option>
                <option value="4">4 Stars</option>
                <option value="5">5 Stars</option>
              </select>
            </div>
            <div className="form-buttons">
              <button onClick={submitRating}>Submit Rating</button>
              <button onClick={() => setShowRateModal(false)}>Cancel</button>
            </div>
          </div>
        </div>
      )}

      <div className="books-grid">
        {books.map(book => (
          <div key={book.id} className="book-card">
            <h3>{book.title}</h3>
            <p><strong>Author:</strong> {book.author}</p>
            <p><strong>Category:</strong> {book.category}</p>
            <p><strong>Year:</strong> {book.year}</p>
            {book.rating > 0 && (
              <p><strong>Rating:</strong> {book.rating.toFixed(1)} ⭐ ({book.rating_count} reviews)</p>
            )}
            <p className={`status ${book.available ? 'available' : 'borrowed'}`}>
              {book.available ? 'Available' : `Borrowed by ${book.borrowed_by_name}`}
            </p>
            <div className="book-actions">
              {book.available ? (
                <button onClick={() => onBorrow(book.id)} className="borrow-btn">Borrow</button>
              ) : book.borrowed_by === currentUser.uid ? (
                <button onClick={() => onReturn(book.id)} className="return-btn">Return</button>
              ) : null}
              <button onClick={() => handleRate(book)} className="rate-btn">Rate</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

// Leaderboard View Component
const LeaderboardView = ({ leaderboard, currentUser, onBack }) => {
  return (
    <div className="leaderboard-container">
      <header>
        <h1>🏆 Student Leaderboard</h1>
        <button onClick={onBack} className="back-btn">Back to Library</button>
      </header>

      <div className="leaderboard-table">
        <table>
          <thead>
            <tr>
              <th>Rank</th>
              <th>Student</th>
              <th>Department</th>
              <th>Points</th>
              <th>Books Borrowed</th>
              <th>Books Returned</th>
            </tr>
          </thead>
          <tbody>
            {leaderboard.map((student, index) => (
              <tr key={student.uid} className={student.uid === currentUser.uid ? 'current-user' : ''}>
                <td>#{index + 1}</td>
                <td>{student.name} ({student.uid})</td>
                <td>{student.department}</td>
                <td>{student.points}</td>
                <td>{student.books_borrowed}</td>
                <td>{student.books_returned}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default App;
