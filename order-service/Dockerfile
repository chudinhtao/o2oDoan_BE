# ==========================================
# STAGE 1: Build
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy toàn bộ project (đã được lọc bởi .dockerignore bỏ qua target/)
COPY . .

# Build Order Service và các thư viện liên quan (như common-lib)
RUN mvn clean package -pl order-service -am -DskipTests

# ==========================================
# STAGE 2: Runtime
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ENV TZ=Asia/Ho_Chi_Minh

COPY --from=builder /app/order-service/target/*.jar app.jar
EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
