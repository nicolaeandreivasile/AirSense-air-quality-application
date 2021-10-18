FROM maven:3.6.3-jdk-8 as builder

WORKDIR /app
COPY pom.xml ./
COPY src ./src

RUN mvn package -DskipTests

FROM adoptopenjdk/openjdk8:alpine-jre
COPY --from=builder /app/target/GattBackendApplication-0.0.1-SNAPSHOT.jar GattBackendApplication-0.0.1-SNAPSHOT.jar
CMD ["java", "-jar", "GattBackendApplication-0.0.1-SNAPSHOT.jar"]