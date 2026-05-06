# --- Stage 1: Build ----------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
# Strip BOM from YAML/properties files (added by Windows PowerShell)
RUN find /app/src -name "*.yml" -o -name "*.yaml" -o -name "*.properties" | \
    xargs sed -i '1s/^\xef\xbb\xbf//' 2>/dev/null || true
RUN mvn clean package -DskipTests -q
# --- Stage 2: Runtime --------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S banking && adduser -S eurekavc -G banking
USER eurekavc
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8761
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8761/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
