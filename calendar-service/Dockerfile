# Multi-stage build pro optimalizaci velikosti
FROM gradle:8.14.2-jdk21 AS build

# Nastavení pracovního adresáře
WORKDIR /app

# Kopírování gradle souborů pro dependency caching
COPY gradle/ gradle/
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY calendar-service/build.gradle.kts calendar-service/gradle.properties ./calendar-service/

# Kopírování OpenAPI specs (potřebné pro build)
COPY calendar-service/shared/ ./calendar-service/shared/

# Download dependencies (tato vrstva se cachuje)
RUN ./gradlew :calendar-service:dependencies --no-daemon

# Kopírování zdrojového kódu
COPY calendar-service/src/ ./calendar-service/src/

# Build aplikace
RUN ./gradlew :calendar-service:shadowJar --no-daemon

# Runtime stage - menší image
FROM eclipse-temurin:21-jre-jammy

# Instalace curl pro health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Vytvoření non-root uživatele pro bezpečnost
RUN groupadd -r app && useradd -r -g app app

# Nastavení pracovního adresáře
WORKDIR /app

# Kopírování jar souboru z build stage
COPY --from=build /app/calendar-service/build/libs/*-all.jar app.jar

# Nastavení vlastníka
RUN chown app:app app.jar

# Přepnutí na non-root uživatele
USER app

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/health || exit 1

# Spuštění aplikace
ENTRYPOINT ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport", "-jar", "app.jar"]
