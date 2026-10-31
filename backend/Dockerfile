# --- Build ---
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
# Prima solo il pom: le dipendenze restano in cache finché non cambia
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- Runtime ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 app
COPY --from=build /app/target/fridge-manager-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
