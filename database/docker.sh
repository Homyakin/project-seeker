docker stop project-seeker-db
docker rm project-seeker-db
docker build -f Dockerfile -t project-seeker-db-img .
docker run --name project-seeker-db -p 5432:5432 -d project-seeker-db-img
