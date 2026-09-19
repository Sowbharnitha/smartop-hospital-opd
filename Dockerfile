# =======================================================
# SmartOP Production Dockerfile
# Multi-stage build for Core Java + JDBC + Static Frontend
# Compatible with Railway, Render, Fly.io, Koyeb
# =======================================================

# Stage 1: Compile Core Java Backend
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build

# Copy Java sources and JDBC driver
COPY backend/src ./src
COPY backend/lib ./lib

# Compile all classes into bin/
RUN mkdir -p bin && \
    javac -encoding UTF-8 -cp "lib/*" -d bin \
    src/config/*.java src/db/*.java src/util/*.java \
    src/model/*.java src/dao/*.java src/service/*.java \
    src/handler/*.java src/server/*.java

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy compiled classes and dependencies
COPY --from=builder /build/bin ./bin
COPY --from=builder /build/lib ./lib

# Copy static frontend files and SQL initialization script
COPY frontend ./frontend
COPY database.sql ./database.sql

# Expose default port
EXPOSE 8080
ENV PORT=8080

# Run Core Java HTTP Server using Linux classpath delimiter (colon)
CMD ["sh", "-c", "java -cp \"bin:lib/*\" server.SmartOPServer"]
