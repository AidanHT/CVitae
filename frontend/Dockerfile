# Development Dockerfile for React frontend

FROM node:18-alpine
WORKDIR /app

# Install system dependencies for development
RUN apk add --no-cache git curl

# Copy package files for better caching
COPY package*.json ./

# Install all dependencies (including devDependencies for development)
RUN npm ci

# Copy source code
COPY . .

# Create non-root user for security
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nextjs -u 1001

# Change ownership of app directory
RUN chown -R nextjs:nodejs /app
USER nextjs

# Health check for development
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:3000/ || exit 1

# Expose development port
EXPOSE 3000

# Development command with hot reload
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0", "--port", "3000"]
