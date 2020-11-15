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

# Set work directory
WORKDIR /home/secure-proxy-redis

# Copy built jar and config file from builder environment into running environment
COPY --from=builder /home/gradle/project/build/libs/secure-redis-proxy-1.1.0.jar .
COPY --from=builder /home/gradle/project/src/main/resources/spring-application.yml .

# Copy Keys and Certificates to Container
# TLS and Attestation Signing Keys are fecthed from CAS
COPY ./production-keystores ./production-keystores

# Set production profile
ENV spring_profile prod

CMD java -jar -Dspring.config.location=spring-application.yml -Dspring.profiles.active=${spring_profile} /home/secure-proxy-redis/secure-redis-proxy-1.1.0.jar
