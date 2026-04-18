FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/skillsync-api-gateway-1.0.0.jar app.jar
EXPOSE 9080
ENTRYPOINT ["java", "-jar", "app.jar"]
