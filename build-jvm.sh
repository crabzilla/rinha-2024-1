mvn clean install -U

# docker buildx build --platform linux/amd64 -f src/main/docker/Dockerfile.jvm --no-cache --progress=plain -t rodolfodpk/rinhadebackend-jvm:latest .

docker build -f src/main/docker/Dockerfile.jvm --no-cache --progress=plain -t rodolfodpk/rinhadebackend-jvm:latest .