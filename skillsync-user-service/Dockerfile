FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/skillsync-user-service-1.0.0.jar app.jar
EXPOSE 9082
ENTRYPOINT ["java", "-jar", "app.jar"]
