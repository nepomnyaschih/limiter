FROM amazoncorretto:17
EXPOSE 8080

COPY . /home

WORKDIR /home

RUN ./gradlew build -x test

ENTRYPOINT ["./gradlew", "bootRun"]
