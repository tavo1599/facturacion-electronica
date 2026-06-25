# ============================================
# Multi-stage build para Motor Java SUNAT
# Java 17 + Spring Boot 3.2.5
# ============================================

# ----------- ETAPA 1: BUILD -----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copiar pom.xml primero (mejor cache)
COPY pom.xml .

# Descargar dependencias (capa cacheable)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar y empaquetar
RUN mvn clean package -DskipTests -B


# ----------- ETAPA 2: PRODUCCIÓN -----------
FROM eclipse-temurin:17-jre-alpine AS production

WORKDIR /app

# Variables por defecto
ENV PORT=8089
ENV JAVA_OPTS="-Xms256m -Xmx768m -Djava.security.egd=file:/dev/./urandom"

# Crear TODAS las carpetas que tu app usa
RUN mkdir -p \
    /app/certificates \
    /app/generated-xml \
    /app/generated-zip \
    /app/received-cdr \
    /app/generated-bajas \
    /app/generated-resumenes \
    /app/generated-guias \
    /app/generated-retenciones \
    /app/generated-percepciones

# Copiar el JAR compilado
COPY --from=builder /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8089

# Comando
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]