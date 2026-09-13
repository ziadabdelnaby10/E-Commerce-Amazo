# syntax=docker/dockerfile:1.7
#
# Multi-stage build: the image is produced from source alone, so CI needs no JDK or Maven and the
# artifact cannot drift from a jar that happened to be sitting in target/.
#
# Generated from infra/docker/Dockerfile.template - keep the eight copies in sync.

# ---------- Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests \
    && cp target/*.jar application.jar

RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---------- Runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

COPY --from=build --chown=spring:spring /build/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /build/extracted/application/ ./

USER spring

ENV SERVER_PORT=9080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 9080

# This is the only container that should ever be published to the outside world.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=12 \
    CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar application.jar"]

