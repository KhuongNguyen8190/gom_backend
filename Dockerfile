# Bước 1: Môi trường Build code sử dụng JDK 25 và tận dụng Maven Wrapper (.mvnw) có sẵn trong dự án
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

# Sao chép các tệp tin cấu hình Maven Wrapper để tối ưu hóa bộ nhớ đệm (cache) của Docker
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Sao chép toàn bộ mã nguồn vào trong môi trường chứa
COPY src ./src

# Thực hiện biên dịch và đóng gói ứng dụng thành file .jar
RUN ./mvnw clean package -DskipTests

# Bước 2: Môi trường thực thi siêu nhẹ sử dụng JRE 25 chuyên dụng cho vận hành thương mại
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Mở cổng 8080 để hệ thống Render thực hiện định tuyến mạng
EXPOSE 8080

# Lệnh khởi chạy ứng dụng Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]