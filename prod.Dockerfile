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
FROM sconecuratedimages/apps:openjdk-8-alpine-scone4.2.1

# Set work directory
WORKDIR /home/secure-proxy-redis

# Copy built jar and config file from builder environment into running environment
COPY --from=builder /home/gradle/project/build/libs/secure-redis-proxy-1.3.0.jar .
COPY --from=builder /home/gradle/project/src/main/resources/spring-application.yml .

# Copy Keys and Certificates to Container
# TLS and Attestation Signing Keys are fecthed from CAS
COPY ./production-keystores/application-enc-keys.p12 ./production-keystores/application-enc-keys.p12
COPY ./production-keystores/proxy-api-truststore.p12 ./production-keystores/proxy-api-truststore.p12
COPY ./production-keystores/proxy-redis-client.p12 ./production-keystores/proxy-redis-client.p12
COPY ./production-keystores/proxy-redis-truststore.p12 ./production-keystores/proxy-redis-truststore.p12

# Set production profile
ENV spring_profile prod

# CAS and LAS environment variables
ENV SCONE_CAS_ADDR=4-2-1.scone-cas.cf
ENV SCONE_LAS_ADDR=51.210.0.209

# Run App
COPY start.sh /home/secure-proxy-redis/start.sh

RUN chmod +x /home/secure-proxy-redis/start.sh

CMD ["/home/secure-proxy-redis/start.sh"]
