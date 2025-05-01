# CVitae - Elite AI Resume Application 🚀

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18+-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5+-blue.svg)](https://www.typescriptlang.org/)

## 🎯 Overview

CVitae is an elite AI-powered resume application that intelligently transforms your master resume into targeted, ATS-optimized resumes using Groq AI and Jake's LaTeX format. Features a minimalist black & white design with smooth animations and an integrated AI chatbot for resume optimization.

## ✨ Features

- **AI-Powered Resume Tailoring**: Uses Groq AI to analyze job postings and optimize resumes
- **Jake's LaTeX Format**: Professional, ATS-friendly resume formatting
- **Multi-Format Export**: PDF, PNG, JPG, and LaTeX code
- **AI Chatbot Assistant**: Intelligent suggestions and modifications
- **Elegant UI/UX**: Minimalist black & white design with smooth animations
- **Flexible Input**: Support for text, file uploads, and partial job descriptions

## 🏗️ Architecture

```
CVitae/
├── backend/           # Java Spring Boot API
├── frontend/          # React TypeScript UI
├── latex-service/     # LaTeX compilation microservice
├── deployment/        # Docker & CI/CD configurations
└── docs/             # Documentation
```

## 🛠️ Technology Stack

### Backend

- **Java 17+** with **Spring Boot 3.x**
- **Spring Security** for future authentication
- **Spring Data JPA** for database operations
- **Maven** for dependency management

### Frontend

- **React 18+** with **TypeScript 5+**
- **Tailwind CSS** for styling
- **Framer Motion** for animations
- **Vite** for build tooling

### Database

- **Supabase** (PostgreSQL)
- Real-time capabilities for future features

### AI & Processing

- **Groq AI** for resume intelligence
- **LangChain** for AI orchestration
- **LaTeX/Pandoc** for document generation

### DevOps

- **Docker** & **Docker Compose**
- **NGINX** reverse proxy
- **GitHub Actions** CI/CD

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose
- Git

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/cvitae.git
cd cvitae
```

### 2. Environment Setup

```bash
# Copy environment files
cp backend/src/main/resources/application.properties.example backend/src/main/resources/application.properties
cp frontend/.env.example frontend/.env

# Update with your API keys:
# - GROQ_API_KEY
# - SUPABASE_URL
# - SUPABASE_ANON_KEY
```

### 3. Development Mode

```bash
# Start all services
docker-compose -f deployment/docker-compose.dev.yml up -d

# Or run individually:
# Backend
cd backend && ./mvnw spring-boot:run

# Frontend
cd frontend && npm install && npm run dev

# LaTeX Service
cd latex-service && docker build -t cvitae-latex . && docker run -p 8082:8080 cvitae-latex
```

### 4. Production Deployment

```bash
docker-compose -f deployment/docker-compose.prod.yml up -d
```

## 📱 Application Workflow

1. **Job Input**: Submit job posting (text/file/keywords)
2. **Master Resume**: Upload comprehensive resume
3. **AI Processing**: Groq AI analyzes and tailors content
4. **Customization**: User controls content and length
5. **Export**: Generate LaTeX, PDF, PNG, JPG formats
6. **Chat Assistant**: AI-powered suggestions and modifications

## 🎨 Design Philosophy

- **Minimalist Aesthetic**: Clean black & white interface
- **Smooth Animations**: Framer Motion for engaging interactions
- **Professional Feel**: Enterprise-grade UI/UX
- **Accessibility**: WCAG 2.1 AA compliant
- **Mobile-First**: Responsive design for all devices

## 🔧 API Endpoints

### Resume Processing

- `POST /api/resumes/analyze` - Analyze job posting
- `POST /api/resumes/generate` - Generate tailored resume
- `GET /api/resumes/{id}` - Retrieve resume
- `PUT /api/resumes/{id}` - Update resume

### Export Services

- `POST /api/export/latex` - Generate LaTeX
- `POST /api/export/pdf` - Generate PDF
- `POST /api/export/image` - Generate image formats

### AI Chat

- `POST /api/chat/message` - Send chat message
- `GET /api/chat/suggestions` - Get AI suggestions

## 🔮 Future Enhancements

- **Interview Prep Module**: AI-powered interview questions and practice
- **Cover Letter Generator**: Automated cover letter creation
- **Recruiter Insights**: Industry trends and keyword optimization
- **Portfolio Integration**: Link to GitHub, LinkedIn, personal websites
- **Multi-language Support**: Resumes in multiple languages
- **Template Marketplace**: Various LaTeX templates
- **Analytics Dashboard**: Application tracking and success metrics

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Jake's LaTeX Resume Template** - Professional formatting
- **Groq AI** - Intelligent resume processing
- **Supabase** - Database and backend services
- **React & TypeScript** - Modern frontend development

---

**Built with ❤️ by Elite Technology Stack Architects**

For questions or support, please open an issue or contact the development team.
