#!/bin/bash
echo "🚀 Building and starting Library Management System..."

# Build the React frontend
echo "📦 Building React frontend..."
cd client
npm install
npm run build
cd ..

# Install backend dependencies if needed
echo "📦 Installing backend dependencies..."
npm install

# Start the server
echo "🌟 Starting the application..."
npm start
