FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*SNAPSHOT.jar app.jar
EXPOSE 9004
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
