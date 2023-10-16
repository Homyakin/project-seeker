FROM bellsoft/liberica-openjdk-alpine:21

RUN apk update && apk add tzdata
ENV TZ=Europe/Moscow

COPY ./target/project-seeker-1.0.jar /home/project-seeker-1.0.jar

CMD java -jar /home/project-seeker-1.0.jar