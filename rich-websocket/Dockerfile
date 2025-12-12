FROM eclipse-temurin:17-jre
WORKDIR /app

COPY build/libs/rich-websocket-1.0.jar app.jar

EXPOSE 8090

CMD ["java", "-jar", "app.jar"]