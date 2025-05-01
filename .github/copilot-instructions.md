# CVitae - Elite AI Resume Application

## Project Overview

CVitae is an elite AI-powered resume application that uses Groq AI to intelligently generate tailored, ATS-optimized resumes in Jake's LaTeX format.

## Technology Stack

- **Backend**: Java + Spring Boot
- **Frontend**: React + TypeScript + Tailwind CSS + Framer Motion
- **Database**: Supabase (PostgreSQL)
- **AI**: Groq AI with LangChain orchestration
- **File Processing**: LaTeX/Pandoc for resume rendering
- **Deployment**: Docker + Docker Compose + NGINX

## Development Progress

- [x] ✅ Verify copilot-instructions.md created
- [x] ✅ Clarify Project Requirements
- [x] ✅ Scaffold the Project
- [x] ✅ Customize the Project
- [x] ✅ Install Required Extensions (None needed for this setup)
- [x] ✅ Compile the Project
- [x] ✅ Create and Run Task
- [x] ✅ Launch the Project
- [x] ✅ Ensure Documentation is Complete

## Architecture

This is a full-stack application with microservices architecture for scalability and maintainability.

## Project Structure

```
CVitae/
├── backend/                 # Java Spring Boot API
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                # React TypeScript UI
│   ├── src/
│   ├── public/
│   ├── Dockerfile
│   ├── package.json
│   └── vite.config.ts
├── latex-service/           # Python LaTeX compilation service
│   ├── app.py
│   ├── Dockerfile
│   └── requirements.txt
├── deployment/              # Docker & deployment configs
│   ├── docker-compose.dev.yml
│   ├── docker-compose.prod.yml
│   └── nginx.conf
├── .github/workflows/       # CI/CD pipeline
└── README.md

## Quick Start
1. Copy `.env.example` to `.env` and update values
2. Run: `docker-compose -f deployment/docker-compose.dev.yml up -d`
3. Access frontend at http://localhost:3000
4. Access backend at http://localhost:8080/api

## Ready for Production
The application is architected for production deployment with:
- Docker containerization
- NGINX reverse proxy
- CI/CD pipeline
- Security best practices
- Scalable microservices architecture
```
