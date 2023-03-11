FROM bellsoft/liberica-openjdk-alpine:18.0.1-12

RUN apk update && apk add tzdata
ENV TZ=Europe/Moscow

COPY ./target/project-seeker-1.0.jar /home/project-seeker-1.0.jar

CMD java -jar /home/project-seeker-1.0.jar