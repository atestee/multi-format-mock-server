# source: https://ktor.io/docs/docker.html#manual

ARG SOURCE_IMAGE_REGISTRY=docker.io/library

# Stage 1: Cache Gradle dependencies
FROM ${SOURCE_IMAGE_REGISTRY}/gradle:jdk21 AS cache
#ENV JAVA_TOOL_OPTIONS="-XX:UseSVE=0"
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.* gradle.properties /home/gradle/app/
COPY gradle /home/gradle/app/gradle
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application
FROM ${SOURCE_IMAGE_REGISTRY}/gradle:jdk21 AS build
#ENV JAVA_TOOL_OPTIONS="-XX:UseSVE=0"
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon

# Stage 3: Create the Runtime Image
FROM ${SOURCE_IMAGE_REGISTRY}/openjdk:21 AS runtime
#ENV JAVA_TOOL_OPTIONS="-XX:UseSVE=0"
EXPOSE 8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/mock-server.jar
COPY db.json /app/db.json
COPY identifiers.json /app/identifiers.json
COPY src/main/resources/application.yaml /app/application.yaml
ENTRYPOINT ["java", "-jar", "/app/mock-server.jar"]
