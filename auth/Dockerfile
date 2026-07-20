
FROM maven:3.9.0-eclipse-temurin-17-alpine AS build

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests -Dfile.encoding=UTF-8

FROM eclipse-temurin:17-jre-alpine

ENV LANG=C.UTF-8
ENV LC_ALL=C.UTF-8

WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

RUN test -f /app/app.jar || (echo "JAR file not found!" && exit 1)

EXPOSE 8080

CMD ["java", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]