FROM gradle:6.5.0-jdk8 as builder

# Set the work dir and run the build
WORKDIR /home/gradle/project

# Copy project to container
COPY ./ /home/gradle/project

# Build Project
RUN gradle clean build

# ------

FROM openjdk:8-alpine
COPY --from=builder /home/gradle/project/build/libs/redis-homomorphic-enc-0.2.jar .
COPY --from=builder /home/gradle/project/src/main/resources/spring-application.yml .
CMD java -jar -Dspring.config.location=spring-application.yml ./redis-homomorphic-enc-0.2.jar
