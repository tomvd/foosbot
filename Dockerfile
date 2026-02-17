FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew && ./gradlew --no-daemon dependencies

COPY src/ src/
RUN ./gradlew --no-daemon shadowJar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar baboon.jar

RUN mkdir -p /data

ENV BABOON_DB_PATH=/data/baboon.db

ENTRYPOINT ["java", "-jar", "baboon.jar"]
