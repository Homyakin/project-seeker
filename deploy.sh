docker build -f database/Dockerfile -t project-seeker-db-img database/ &&
mvn package spring-boot:repackage && (
sudo docker-compose build --no-cache
sudo docker-compose stop
sudo docker-compose up -d
)