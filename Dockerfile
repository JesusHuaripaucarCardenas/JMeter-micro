# syntax=docker/dockerfile:1.7

FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Código fuente
COPY src ./src

# Build sin tests
RUN mvn clean package -DskipTests -q


FROM gcr.io/distroless/java17-debian12

WORKDIR /app

# Copiar JAR
COPY --from=builder /build/target/*.jar app.jar

# Optimización JVM para contenedores
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 \
-Djava.security.egd=file:/dev/./urandom \
-XX:+ExitOnOutOfMemoryError"

EXPOSE 9091

# Ejecutar app
ENTRYPOINT ["java", "-jar", "app.jar"]