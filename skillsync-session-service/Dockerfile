FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/skillsync-session-service-1.0.0.jar app.jar
EXPOSE 9085
ENTRYPOINT ["java", "-jar", "app.jar"]
