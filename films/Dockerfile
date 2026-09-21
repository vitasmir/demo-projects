# Use a minimal base image for running the application
FROM alpine/java:22-jdk

# Set the working directory
WORKDIR /app

# Copy the packaged application from the builder stage
COPY target/*.jar app.jar

EXPOSE 8080
EXPOSE 8443

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
