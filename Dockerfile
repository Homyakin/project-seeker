FROM bellsoft/liberica-openjdk-alpine:22

RUN apk update && apk add tzdata
ENV TZ=Europe/Moscow

COPY ./target/project-seeker-1.2.jar /home/project-seeker-1.2.jar

CMD java -jar /home/project-seeker-1.2.jar