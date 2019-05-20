FROM maven:3.6.0-jdk-8-alpine as builder
COPY . .
# Here we skip tests to save time, because if this image is being built - tests have already passed...
RUN mvn install -DskipTests

FROM openjdk:8-jre-alpine
COPY --from=builder /target/inbox-0.0.3-SNAPSHOT.jar .
COPY entrypoint.sh .
CMD ["java", "-jar", "inbox-0.0.3-SNAPSHOT.jar"]
