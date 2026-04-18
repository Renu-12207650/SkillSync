FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/skillsync-auth-service-1.0.0.jar app.jar
EXPOSE 9081
ENTRYPOINT ["java", "-jar", "app.jar"]
