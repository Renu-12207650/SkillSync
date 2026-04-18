FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/skillsync-eureka-server-1.0.0.jar app.jar
EXPOSE 9761
ENTRYPOINT ["java", "-jar", "app.jar"]
