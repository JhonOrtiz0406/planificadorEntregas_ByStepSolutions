# ==============================
# Backend Dockerfile
# DeliveryPlanner - ByStep Solutions S.A.S.
# ==============================

# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY gradlew gradlew.bat gradle.properties build.gradle main.gradle settings.gradle lombok.config ./
COPY gradle ./gradle
COPY domain ./domain
COPY infrastructure ./infrastructure
COPY applications ./applications

RUN chmod +x gradlew && ./gradlew :app-service:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/applications/app-service/build/libs/delivery-planner.jar app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=prod"

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8084/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
