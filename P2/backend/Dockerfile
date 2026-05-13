# syntax=docker/dockerfile:1.6

FROM gradle:8.10-jdk21 AS build
WORKDIR /workspace

COPY settings.gradle build.gradle ./
COPY gradle ./gradle
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar
RUN chown -R app:app /app
USER app

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
