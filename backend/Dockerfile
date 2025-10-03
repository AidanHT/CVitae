# Development Dockerfile for Spring Boot backend

# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies for better caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for security (use existing group if GID exists)
RUN groupadd -g 1001 appuser || true && \
    useradd -r -u 1001 -g 1001 appuser || useradd -r appuser

# Copy built application
COPY --from=build /app/target/*.jar app.jar

# Create logs directory and set permissions
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# Switch to non-root user
USER appuser

# Health check for development
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose application port
EXPOSE 8080

# Development JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Dspring.profiles.active=development"

# Run application with development settings
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
