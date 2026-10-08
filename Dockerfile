FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy AS runtime

WORKDIR /app
COPY --from=build /workspace/target/WebThiTracNghiem-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
CMD ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
