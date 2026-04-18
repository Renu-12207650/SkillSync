FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/skillsync-config-server-1.0.0.jar app.jar
EXPOSE 9888
ENTRYPOINT ["java", "-jar", "app.jar"]
