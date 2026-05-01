# ==========================================
# STAGE 1: Build
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY . .
RUN mvn clean package -pl report-service -am -DskipTests

# ==========================================
# STAGE 2: Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh

COPY --from=builder /app/report-service/target/*.jar app.jar
EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
