# Multi-stage build pro optimalizaci velikosti
FROM gradle:8.14.2-jdk21 AS build

# Nastavení pracovního adresáře
WORKDIR /app

# Kopírování gradle souborů pro dependency caching
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY auth-service/build.gradle.kts auth-service/gradle.properties ./auth-service/

# Kopírování OpenAPI specs (potřebné pro build)
COPY auth-service/shared/ ./auth-service/shared/

# Download dependencies (tato vrstva se cachuje)
RUN ./gradlew :auth-service:dependencies --no-daemon

# Kopírování zdrojového kódu
COPY auth-service/src/ ./auth-service/src/

# Build aplikace
RUN ./gradlew :auth-service:shadowJar --no-daemon

# Runtime stage - menší image
FROM openjdk:21-jre-slim

# Instalace curl pro health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Vytvoření non-root uživatele pro bezpečnost
RUN groupadd -r app && useradd -r -g app app

# Nastavení pracovního adresáře
WORKDIR /app

# Kopírování jar souboru z build stage
COPY --from=build /app/auth-service/build/libs/*-all.jar app.jar

# Nastavení vlastníka
RUN chown app:app app.jar

# Přepnutí na non-root uživatele
USER app

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8081/health || exit 1

# Spuštění aplikace
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport", "-jar", "app.jar"]