@echo off
REM CVitae Quick Setup Script for Windows

echo 🚀 CVitae - Elite Resume Builder Setup
echo ======================================

REM Check if .env file exists
if not exist ".env" (
    echo 📝 Creating .env file from template...
    copy .env.example .env
    echo ✅ .env file created. Please edit it with your actual credentials.
) else (
    echo ✅ .env file already exists.
)

REM Check if backend application.properties exists
if not exist "backend\src\main\resources\application.properties" (
    echo 📝 Creating backend application.properties...
    copy backend\src\main\resources\application.properties.example backend\src\main\resources\application.properties
    echo ✅ Backend properties file created.
) else (
    echo ✅ Backend properties file already exists.
)

REM Check if frontend .env exists
if not exist "frontend\.env" (
    echo 📝 Creating frontend .env file...
    copy frontend\.env.example frontend\.env
    echo ✅ Frontend .env file created.
) else (
    echo ✅ Frontend .env file already exists.
)

echo.
echo 📦 Installing dependencies...

REM Frontend dependencies
if exist "frontend\node_modules" (
    echo ✅ Frontend dependencies already installed.
) else (
    echo 📦 Installing frontend dependencies...
    cd frontend && npm install && cd ..
    echo ✅ Frontend dependencies installed.
)

REM Check for Java/Maven
mvn --version >nul 2>&1
if %errorlevel% == 0 (
    echo 📦 Installing backend dependencies...
    cd backend && mvn clean install -DskipTests && cd ..
    echo ✅ Backend dependencies installed.
) else (
    echo ⚠️  Maven not found. Please install Maven to build the backend.
)

REM Check for Docker
docker --version >nul 2>&1
if %errorlevel% == 0 (
    echo 🐳 Docker found. You can run 'docker-compose up' to start all services.
) else (
    echo ⚠️  Docker not found. Please install Docker to run the full stack.
)

echo.
echo 🎉 Setup Complete!
echo.
echo Next Steps:
echo 1. Edit .env with your actual API keys and database credentials
echo 2. Edit backend\src\main\resources\application.properties
echo 3. Edit frontend\.env with your API endpoint
echo.
echo Development Commands:
echo • Frontend: cd frontend ^&^& npm run dev
echo • Backend: cd backend ^&^& mvn spring-boot:run
echo • Docker: docker-compose -f docker-compose.dev.yml up
echo.
echo For production: docker-compose -f docker-compose.prod.yml up

pause
