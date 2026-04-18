FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/skillsync-mentor-service-1.0.0.jar app.jar
EXPOSE 9083
ENTRYPOINT ["java", "-jar", "app.jar"]
