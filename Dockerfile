FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Resolve dependencies in their own layer so source changes don't re-download them.
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar
USER app

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
