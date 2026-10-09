FROM eclipse-temurin:17-jdk-alpine
COPY build/libs/phoenix-scheduler-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-Xms512m","-Xmx5g","-jar","/app.jar"]
