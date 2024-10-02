# Use the official gradle image to create a build artifact.
FROM gradle:8.10.1-jdk21 AS build

# Set the working directory in the image to /app
WORKDIR /app

# Copy the build.gradle.kts file to our app directory
COPY build.gradle.kts .
COPY settings.gradle.kts .

# This command builds our application and downloads all gradle dependencies
RUN gradle build --no-daemon

# Copy the rest of the application source code
COPY src /app/src

# Build the application
RUN gradle build

# Use the official openjdk image for a lean production stage of our multi-stage build
FROM openjdk:21

# Set the working directory in the image to /app
WORKDIR /app

# Copy the jar file from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Run the application
CMD ["java", "-jar", "app.jar"]