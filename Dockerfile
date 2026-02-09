# itemstorage: Item Storage Service (Java 25)
FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /app

ENV MAVEN_OPTS="-Xmx512m"

COPY pom.xml .
COPY src src

RUN mvn clean package -DskipTests -B

# --- Runtime ---
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
