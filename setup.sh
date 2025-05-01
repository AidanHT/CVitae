#!/bin/bash

# CVitae Quick Setup Script

echo "🚀 CVitae - Elite Resume Builder Setup"
echo "======================================"

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo "📝 Creating .env file from template..."
    cp .env.example .env
    echo "✅ .env file created. Please edit it with your actual credentials."
else
    echo "✅ .env file already exists."
fi

# Check if backend application.properties exists
if [ ! -f "backend/src/main/resources/application.properties" ]; then
    echo "📝 Creating backend application.properties..."
    cp backend/src/main/resources/application.properties.example backend/src/main/resources/application.properties
    echo "✅ Backend properties file created."
else
    echo "✅ Backend properties file already exists."
fi

# Check if frontend .env exists
if [ ! -f "frontend/.env" ]; then
    echo "📝 Creating frontend .env file..."
    cp frontend/.env.example frontend/.env
    echo "✅ Frontend .env file created."
else
    echo "✅ Frontend .env file already exists."
fi

# Install dependencies
echo ""
echo "📦 Installing dependencies..."

# Frontend dependencies
if [ -d "frontend/node_modules" ]; then
    echo "✅ Frontend dependencies already installed."
else
    echo "📦 Installing frontend dependencies..."
    cd frontend && npm install && cd ..
    echo "✅ Frontend dependencies installed."
fi

# Check for Java/Maven
if command -v mvn &> /dev/null; then
    echo "📦 Installing backend dependencies..."
    cd backend && mvn clean install -DskipTests && cd ..
    echo "✅ Backend dependencies installed."
else
    echo "⚠️  Maven not found. Please install Maven to build the backend."
fi

# Check for Docker
if command -v docker &> /dev/null; then
    echo "🐳 Docker found. You can run 'docker-compose up' to start all services."
else
    echo "⚠️  Docker not found. Please install Docker to run the full stack."
fi

echo ""
echo "🎉 Setup Complete!"
echo ""
echo "Next Steps:"
echo "1. Edit .env with your actual API keys and database credentials"
echo "2. Edit backend/src/main/resources/application.properties"
echo "3. Edit frontend/.env with your API endpoint"
echo ""
echo "Development Commands:"
echo "• Frontend: cd frontend && npm run dev"
echo "• Backend: cd backend && mvn spring-boot:run"
echo "• Docker: docker-compose -f docker-compose.dev.yml up"
echo ""
echo "For production: docker-compose -f docker-compose.prod.yml up"
