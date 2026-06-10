# ==========================================
# STAGE 1: Build (chạy từ root /backend)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy toàn bộ backend root (multi-module Maven)
COPY . .

# Build inventory-service và các common modules phụ thuộc
RUN mvn clean package -pl inventory-service -am -DskipTests

# ==========================================
# STAGE 2: Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh

COPY --from=builder /app/inventory-service/target/*.jar app.jar
EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
