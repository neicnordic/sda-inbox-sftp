FROM maven:3.6.0-jdk-8-alpine as builder
COPY . .
# Here we skip tests to save time, because if this image is being built - tests have already passed...
RUN mvn install -DskipTests

FROM openjdk:8-jre-alpine

RUN addgroup -g 1000 lega && \
    adduser -D -u 1000 -G lega lega

RUN mkdir -p /ega/inbox && \
    chgrp lega /ega/inbox && \
    chmod 2770 /ega/inbox

VOLUME /ega/inbox

COPY --from=builder /target/inbox-0.0.3-SNAPSHOT.jar .

COPY entrypoint.sh .

RUN chmod +x entrypoint.sh

#USER 1000

ENTRYPOINT ["/entrypoint.sh"]

CMD ["java", "-jar", "inbox-0.0.3-SNAPSHOT.jar"]
