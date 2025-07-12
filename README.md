# Library Management System - Web Version

A modern web-based library management system built with React frontend and Express.js backend.

## Features

- 📚 **Student Authentication**: UID-based login system
- 👨‍💼 **Admin Panel**: Full book management capabilities
- 🔍 **Search & Filter**: Search books by title, author, category, or ISBN
- 📖 **Book Operations**: Borrow, return, and rate books
- 🎮 **Gamification**: Points system and student leaderboard
- 📊 **Statistics**: Real-time library analytics
- 📱 **Responsive Design**: Works on desktop and mobile devices

## Tech Stack

- **Frontend**: React.js with modern CSS
- **Backend**: Node.js with Express.js
- **Database**: SQLite
- **Deployment**: Ready for Heroku, Vercel, Railway, or any Node.js hosting

## Quick Start

### Local Development

1. **Clone and setup**:
   ```bash
   cd web-version
   npm install
   cd client && npm install && cd ..
   ```

2. **Build frontend**:
   ```bash
   cd client && npm run build && cd ..
   ```

3. **Start the application**:
   ```bash
   npm start
   ```

4. **Access the application**:
   - Open http://localhost:3000 in your browser

### Login Credentials

**Student Login:**
- Use any valid UID format: `YYDEPTnnnnn`
- Examples: `23BCS12345`, `22BCE10001`, `21BBA10002`

**Admin Login:**
- Password: `admin123`

## Deployment Options

### Option 1: Heroku

1. Create a Heroku app:
   ```bash
   heroku create your-library-app
   ```

2. Set environment variables:
   ```bash
   heroku config:set NODE_ENV=production
   ```

3. Deploy:
   ```bash
   git add .
   git commit -m "Deploy library management system"
   git push heroku main
   ```

### Option 2: Railway

1. Connect your GitHub repository to Railway
2. Railway will automatically detect the Node.js app
3. Set PORT environment variable (Railway does this automatically)
4. Deploy with one click

### Option 3: Vercel

1. Install Vercel CLI:
   ```bash
   npm i -g vercel
   ```

2. Deploy:
   ```bash
   vercel
   ```

### Option 4: DigitalOcean App Platform

1. Create a new app on DigitalOcean
2. Connect your GitHub repository
3. Configure build command: `cd client && npm run build`
4. Configure run command: `npm start`
5. Deploy

## 🚀 Live Demo

### Web Version (Modern React App)
- **Railway**: [Coming Soon - Deployment in Progress]
- **Vercel**: [https://library-management-system-deepak.vercel.app](https://library-management-system-deepak.vercel.app)

The web version demonstrates all original java features in a modern, responsive web interface.

---

## Environment Variables

- `PORT`: Server port (default: 3000)
- `NODE_ENV`: Environment (development/production)

## Project Structure

```
web-version/
├── server.js              # Express.js backend server
├── package.json           # Backend dependencies
├── library.db             # SQLite database (auto-created)
├── client/                # React frontend
│   ├── src/
│   │   ├── App.js         # Main React component
│   │   ├── App.css        # Styles
│   │   └── ...
│   ├── public/
│   └── package.json       # Frontend dependencies
└── README.md
```

## API Endpoints

- `GET /api/books` - Get all books
- `GET /api/books/search` - Search books
- `POST /api/books` - Add new book (admin)
- `DELETE /api/books/:id` - Delete book (admin)
- `POST /api/auth/student` - Student authentication
- `POST /api/auth/admin` - Admin authentication
- `POST /api/books/:id/borrow` - Borrow book
- `POST /api/books/:id/return` - Return book
- `POST /api/books/:id/rate` - Rate book
- `GET /api/students/:uid` - Get student profile
- `GET /api/leaderboard` - Get student leaderboard
- `GET /api/stats` - Get library statistics

## Features in Detail

### Student Features
- Browse and search library books
- Borrow available books
- Return borrowed books
- Rate books (1-5 stars)
- View personal statistics
- Check leaderboard rankings
- Earn points for activities

### Admin Features
- View all books with detailed information
- Add new books to the library
- Delete books from the library
- Search and filter books
- View library statistics
- Monitor borrowing activity

### Gamification System
- **Points System**: 
  - 10 points for borrowing a book
  - 15 points for returning a book on time
- **Leaderboard**: Shows top students by points
- **Achievements**: Track student activities

## Database Schema

The application uses SQLite with the following tables:
- `books` - Book information and availability
- `students` - Student profiles and statistics
- `borrowing_history` - Borrowing and return records
- `achievements` - Student achievements and points

## Customization

### Adding New Book Categories
Edit the category dropdown in the admin panel by modifying the React component.

### Changing Point Values
Modify the point values in `server.js` in the borrow/return endpoints.

### Styling
Customize the appearance by editing `client/src/App.css`.

## Troubleshooting

### Database Issues
- The SQLite database is automatically created on first run
- If you need to reset the database, delete `library.db` and restart the server

### Port Issues
- The app uses PORT environment variable or defaults to 3000
- Make sure the port is not already in use

### Build Issues
- Make sure to run `npm run build` in the client directory before deploying
- Check that all dependencies are installed

## Support

For issues or questions:
1. Check the console for error messages
2. Verify all dependencies are installed
3. Ensure the database file has proper permissions
4. Check that the frontend is properly built

## License

MIT License - Feel free to use this project for educational or personal purposes.
