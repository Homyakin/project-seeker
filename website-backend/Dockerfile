FROM bellsoft/liberica-openjdk-alpine:24

RUN apk update && apk add tzdata
ENV TZ=Europe/Moscow

COPY ./target/project-seeker-website-backend-1.0.jar /home/project-seeker-website-backend-1.0.jar

CMD java -jar /home/project-seeker-website-backend-1.0.jar
