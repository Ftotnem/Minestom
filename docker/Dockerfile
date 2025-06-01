# Use a minimal base image with Java
FROM openjdk:23-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the server JAR into the container
COPY ./docker/Minestom-0.1.jar /app/server.jar

# Copy the resources folder into the container (relative to the root directory)
COPY ./ls
src/main/resources /app/resources

# Expose the port
EXPOSE 25565

# Start the server with dynamic port configuration
CMD ["java", "-jar", "/app/server.jar", "--port", "${SERVER_PORT:-25565}"]
