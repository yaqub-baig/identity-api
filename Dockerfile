# ===========================================
# Multi-stage Dockerfile for Identity Service
# ===========================================

# ============================================
# Stage 1: Build Stage
# ============================================
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /build

# Install Maven
RUN apk add --no-cache maven

# Copy all project files
COPY pom.xml .
COPY src ./src

# Build the application (skip tests completely - don't compile or run)
RUN mvn clean package -Dmaven.test.skip=true -B

# ============================================
# Stage 2: Runtime Stage
# ============================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install wget for health checks
RUN apk add --no-cache wget

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy JAR from build stage
COPY --from=build /build/target/*.jar app.jar

# Create directories with proper permissions
RUN mkdir -p /app/documents /app/logs && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8010

# Health check configuration
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8010/api/v1/actuator/health || exit 1

# JVM optimization flags
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
