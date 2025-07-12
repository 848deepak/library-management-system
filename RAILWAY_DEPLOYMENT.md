# Railway Deployment Guide for Library Management System

## Overview
This guide will help you deploy the Library Management System to Railway, which provides excellent support for full-stack Node.js applications with databases.

## Prerequisites
1. Railway account (sign up at https://railway.app)
2. Railway CLI installed (optional but recommended)
3. GitHub repository with your code

## Deployment Methods

### Method 1: GitHub Integration (Recommended)

1. **Connect GitHub Repository**
   - Go to https://railway.app
   - Click "Start a New Project"
   - Select "Deploy from GitHub repo"
   - Connect your GitHub account if not already connected
   - Select the `library-management-system` repository

2. **Configure Environment Variables**
   - In Railway dashboard, go to your project
   - Click on "Variables" tab
   - Add the following environment variables:
     ```
     NODE_ENV=production
     PORT=3000
     DATABASE_PATH=./library.db
     ```

3. **Deploy**
   - Railway will automatically detect the Node.js app
   - It will run `npm install` and then `npm start`
   - The deployment will include both frontend and backend

### Method 2: Railway CLI

1. **Install Railway CLI**
   ```bash
   npm install -g @railway/cli
   ```

2. **Login to Railway**
   ```bash
   railway login
   ```

3. **Initialize and Deploy**
   ```bash
   cd "/Users/deepakpandey/Coding /Projects/library managment system"
   railway init
   railway up
   ```

## Project Structure for Railway

```
library-management-system/
├── server.js              # Main backend server
├── package.json           # Root package.json with start script
├── Procfile              # Process file for Railway
├── railway.json          # Railway configuration
├── library.db            # SQLite database
├── client/               # React frontend
│   ├── build/           # Built React app (auto-served by Express)
│   ├── package.json     # Frontend dependencies
│   └── src/             # React source code
└── java-desktop-app/    # Original Java desktop app
```

## Key Configuration Files

### package.json (Root)
- `"start": "node server.js"` - Railway runs this command
- `"build": "cd client && npm run build"` - Builds React app
- `"heroku-postbuild"` - Also works for Railway

### server.js
- Serves React build files from `client/build`
- Provides API endpoints for library operations
- Uses SQLite database (file-based, perfect for Railway)

### Procfile
```
web: npm start
```

## Railway Features Used

1. **Automatic Node.js Detection**: Railway automatically detects Node.js apps
2. **Build Process**: Runs npm install automatically
3. **Environment Variables**: Easy to set via dashboard
4. **Custom Domains**: Available with Railway Pro
5. **Automatic HTTPS**: Provided by default
6. **Database**: SQLite file persists with Railway's volume storage

## Database Considerations

- **SQLite**: Works great on Railway with persistent volumes
- **PostgreSQL**: Railway offers managed PostgreSQL if you want to upgrade
- **Current Setup**: Uses SQLite file database (`library.db`)

## Deployment URL

After successful deployment, Railway will provide:
- **App URL**: `https://[your-app-name].railway.app`
- **Custom Domain**: Available if you have Railway Pro

## Environment Variables

Set these in Railway dashboard:

| Variable | Value | Description |
|----------|-------|-------------|
| `NODE_ENV` | `production` | Enables production optimizations |
| `PORT` | `3000` | Port for the application (Railway auto-assigns) |
| `DATABASE_PATH` | `./library.db` | Path to SQLite database |

## Troubleshooting

### Common Issues:

1. **Build Failures**
   - Check that `client/package.json` exists
   - Ensure React build succeeds locally first

2. **Database Issues**
   - SQLite file should be included in repository
   - Check file permissions

3. **Static File Serving**
   - Ensure `server.js` serves files from `client/build`
   - Check Express static middleware setup

### Logs
- Use Railway dashboard to view application logs
- Check both build logs and runtime logs

## Scaling

Railway offers:
- **Automatic scaling** based on traffic
- **Custom resource limits** (CPU/Memory)
- **Multiple regions** for global deployment

## Cost

- **Hobby Plan**: Free tier with limitations
- **Pro Plan**: $5/month for better resources and custom domains
- **Usage-based pricing** for resources

## Next Steps After Deployment

1. **Custom Domain**: Set up custom domain if desired
2. **Monitoring**: Use Railway's built-in monitoring
3. **Database Backup**: Consider periodic database backups
4. **Performance**: Monitor and optimize as needed

## Support

- Railway Documentation: https://docs.railway.app
- Railway Discord: Active community support
- Railway Status: https://status.railway.app

---

## Quick Deploy Commands

```bash
# Clone repository (if needed)
git clone https://github.com/848deepak/library-management-system.git
cd library-management-system

# Install Railway CLI
npm install -g @railway/cli

# Login and deploy
railway login
railway init
railway up
```

Your Library Management System will be live and accessible worldwide!
