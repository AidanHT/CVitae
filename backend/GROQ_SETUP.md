# Groq AI Integration Setup Guide

## Overview

This guide explains how to set up the Groq AI integration for CVitae's resume optimization features.

## Prerequisites

1. Groq API account and API key
2. Java 17+ installed
3. Maven or Gradle build system
4. PostgreSQL database

## Step 1: Get Groq API Key

1. Visit [console.groq.com](https://console.groq.com)
2. Sign up or log in to your account
3. Navigate to API Keys section
4. Create a new API key
5. Copy the API key (starts with `gsk_...`)

## Step 2: Configure Environment Variables

### Option A: Environment Variables

```bash
export GROQ_API_KEY=your_actual_groq_api_key_here
export DB_USERNAME=postgres
export DB_PASSWORD=your_database_password
```

### Option B: Update application.properties

```properties
# Replace the placeholder in application.properties
groq.api.key=your_actual_groq_api_key_here

# Ensure database is configured
spring.datasource.url=jdbc:postgresql://localhost:5432/cvitae
spring.datasource.username=postgres
spring.datasource.password=your_password
```

## Step 3: Verify Integration

### Start the Application

```bash
cd backend
./mvnw spring-boot:run
```

### Test the Integration

1. Check application logs for Groq AI initialization
2. Look for log message: "Groq API key configured successfully"
3. If you see "Groq API key not configured, using fallback response", check your API key

### Test Endpoints

```bash
# Test job analysis
curl -X POST http://localhost:8080/api/resumes/analyze-job \
  -H "Content-Type: application/json" \
  -d '{
    "jobPosting": "Software Engineer position...",
    "jobTitle": "Software Engineer",
    "companyName": "Tech Corp"
  }'

# Test resume generation
curl -X POST http://localhost:8080/api/resumes/generate \
  -H "Content-Type: application/json" \
  -d '{
    "masterResume": "Your resume content...",
    "jobPosting": "Job description...",
    "jobTitle": "Software Engineer",
    "companyName": "Tech Corp",
    "targetLength": 1,
    "includeExperience": true,
    "includeEducation": true,
    "includeProjects": true,
    "includeSkills": true,
    "includeLeadership": true,
    "userId": "test-user",
    "sessionId": "test-session"
  }'

# Test chat
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How can I improve my resume?",
    "sessionId": "chat-session",
    "context": "resume_optimization",
    "userId": "test-user"
  }'
```

## Architecture Overview

### New AI Orchestration Layer

```
/backend/src/main/java/com/cvitae/ai/
├── GroqClient.java              # Enhanced API client with error handling
├── GroqRequest.java             # Structured request objects
├── GroqResponse.java            # Response handling with fallbacks
├── ResumeTailorService.java     # Complete resume tailoring workflow
├── ResumeTailoringResult.java   # Comprehensive results object
├── ChatbotService.java          # AI-powered chat assistance
├── ChatbotResponse.java         # Chat response handling
└── ChatbotSuggestion.java       # Structured suggestions
```

### Key Features

1. **Robust Error Handling**: Graceful fallbacks when API is unavailable
2. **Cost Optimization**: Efficient token usage and caching strategies
3. **Structured Prompts**: Engineered prompts for optimal AI responses
4. **ATS Optimization**: Built-in ATS compatibility scoring
5. **Context Awareness**: Chat responses consider resume and job context

## Prompt Engineering

### Job Analysis Prompts

- Structured extraction of requirements, skills, and keywords
- Experience level determination
- Company culture indicators
- ATS optimization recommendations

### Resume Tailoring Prompts

- Content prioritization based on job relevance
- Keyword optimization while maintaining truthfulness
- Achievement quantification suggestions
- Professional formatting guidelines

### LaTeX Generation Prompts

- Jake's template structure compliance
- Professional formatting standards
- Clean, compilable code generation
- Target length optimization

## Monitoring and Debugging

### Log Messages to Watch For

```
INFO  - Groq API key configured successfully
INFO  - Starting complete resume tailoring for job: [Job Title]
INFO  - Job analysis completed with [X] requirements extracted
INFO  - Resume content generated with ATS score: [X]%
WARN  - Groq API key not configured, using fallback response
ERROR - Groq API error - Status: [XXX], Body: [Response]
```

### Performance Metrics

- Token usage per request
- Response time tracking
- ATS compatibility scores
- User satisfaction indicators

## Troubleshooting

### Common Issues

#### 1. "Groq API key not configured"

**Problem**: API key is not properly set
**Solution**:

- Verify environment variable is set: `echo $GROQ_API_KEY`
- Check application.properties has correct key
- Restart application after setting key

#### 2. "API request failed: 401 Unauthorized"

**Problem**: Invalid or expired API key
**Solution**:

- Generate new API key from Groq console
- Update environment variable or configuration
- Verify API key format (should start with `gsk_`)

#### 3. "Response parsing failed"

**Problem**: Unexpected API response format
**Solution**:

- Check Groq API status
- Verify model compatibility
- Review API documentation for changes

#### 4. Fallback responses only

**Problem**: AI integration not working, using local responses
**Solution**:

- Check network connectivity
- Verify API key permissions
- Review application logs for specific errors

### Debug Mode

Enable detailed logging:

```properties
logging.level.com.cvitae.ai=DEBUG
logging.level.org.springframework.web.reactive.function.client=DEBUG
```

## Production Considerations

### Security

- Store API keys in secure environment variables
- Use secrets management in production
- Implement rate limiting
- Monitor API usage and costs

### Performance

- Implement response caching for identical requests
- Use connection pooling for HTTP clients
- Set appropriate timeouts
- Monitor token usage and optimize prompts

### Scalability

- Consider async processing for large resumes
- Implement request queuing for high load
- Use circuit breakers for API failures
- Cache common job analysis results

## Support

- Check application logs first
- Verify Groq API status at [status.groq.com](https://status.groq.com)
- Review API documentation at [docs.groq.com](https://docs.groq.com)
- Test with minimal examples before complex requests
