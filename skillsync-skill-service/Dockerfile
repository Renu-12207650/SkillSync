FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/skillsync-skill-service-1.0.0.jar app.jar
EXPOSE 9084
ENTRYPOINT ["java", "-jar", "app.jar"]
