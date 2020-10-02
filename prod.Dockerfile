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
FROM sconecuratedimages/apps:openjdk-8-alpine

# Set work directory
WORKDIR /home/secure-proxy-redis

# Copy built jar and config file from builder environment into running environment
COPY --from=builder /home/gradle/project/build/libs/secure-redis-proxy-0.3.1.jar .
COPY --from=builder /home/gradle/project/src/main/resources/spring-application.yml .

COPY ./production-keystores/ ./production-keystores

# Set production profile
ENV spring_profile prod

# Run App
COPY start.sh /home/secure-proxy-redis/start.sh

RUN chmod +x /home/secure-proxy-redis/start.sh

CMD ["/home/secure-proxy-redis/start.sh"]
