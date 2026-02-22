# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем pom.xml и скачиваем зависимости (кешируем этот слой)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Сборка без тестов (так как мы их пропустили в POM)
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Копируем только собранный jar
COPY --from=build /app/target/*.jar app.jar

# Порт сервиса
EXPOSE 8082

# Исправленный Healthcheck
# Используем Actuator (если он настроен) или любой открытый эндпоинт
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8082/api/v1/actuator/health || exit 1

# Запуск
ENTRYPOINT ["java", "-jar", "app.jar"]