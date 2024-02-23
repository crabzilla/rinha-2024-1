mvn clean install -Pnative -Dquarkus.native.container-build=true

docker buildx build --platform linux/amd64 -f src/main/docker/Dockerfile.native --no-cache --progress=plain -t rodolfodpk/rinhadebackend-native:latest .
