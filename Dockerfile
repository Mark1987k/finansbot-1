# --- Build aşaması ---
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests || mvn -f pom.xml clean package -DskipTests

# --- Run aşaması ---
# Playwright Chromium için gerekli sistem paketleri dahil
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Playwright browser'ını yükle
RUN npx playwright install chromium

ENTRYPOINT ["java", "-jar", "app.jar"]
