# Build environment
FROM gradle:6.5.0-jdk8 as builder

# Set the work dir and run the build
WORKDIR /home/gradle/project

# Copy project to container
COPY ./ /home/gradle/project

# Build Project
RUN gradle clean build

######## ------ ########

# Lightweight Run Environment
FROM openjdk:8-alpine

# Copy built jar and config file from builder environment into running environment
COPY --from=builder /home/gradle/project/build/libs/redis-homomorphic-enc-0.2.jar .
COPY --from=builder /home/gradle/project/src/main/resources/spring-application.yml .

# Run jar
CMD java -jar -Dspring.config.location=spring-application.yml ./redis-homomorphic-enc-0.2.jar
