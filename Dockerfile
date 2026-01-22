# Giai đoạn 1: Build code (Dùng Maven để đóng gói)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY . .
# Lệnh này sẽ tự tải Maven wrapper và build ra file .jar
# -DskipTests để build cho lẹ, bỏ qua test
RUN ./mvnw clean package -DskipTests

# Giai đoạn 2: Chạy ứng dụng (Chỉ lấy cái file .jar đã build sang đây cho nhẹ)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]