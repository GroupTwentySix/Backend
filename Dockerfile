# Use the official gradle image to create a build artifact.
FROM gradle:8.10.1-jdk21 AS build

# Set the working directory in the image to /app
WORKDIR /app

# Copy the entire project to the container
COPY . .

# This command builds our application and downloads all gradle dependencies
RUN gradle shadowJar --no-daemon

# Use the official openjdk image for a lean production stage of our multi-stage build
FROM openjdk:21

# Set the working directory in the image to /app
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

COPY .env .env


# Run the application
CMD ["java", "-jar", "app.jar"]