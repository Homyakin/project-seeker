FROM bellsoft/liberica-openjdk-alpine:21

RUN apk update && apk add tzdata
ENV TZ=Europe/Moscow

COPY ./target/project-seeker-1.1.jar /home/project-seeker-1.1.jar

CMD java -jar /home/project-seeker-1.1.jar