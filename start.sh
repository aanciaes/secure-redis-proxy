#!/bin/sh

# Extract proxy server MRENCLAVE to file
# SCONE_HASH=1 java -jar -Dspring.config.location=spring-application.yml -Dspring.profiles.active=${spring_profile} /home/secure-proxy-redis/secure-redis-proxy-1.2.1.jar > /home/secure-proxy-redis/mrenclave

# Make MRENCLAVE file readonly to all
# chmod 444 /home/secure-proxy-redis/mrenclave

# Run redis with provided config file
SCONE_VERSION=1 SCONE_LOG=7 SCONE_CONFIG_ID=secure-redis-proxy/proxy java -jar -Dspring.config.location=spring-application.yml -Dspring.profiles.active=${spring_profile} /home/secure-proxy-redis/secure-redis-proxy-1.2.1.jar
