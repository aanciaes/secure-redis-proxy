#!/bin/sh

# Extract proxy server MRENCLAVE to file
SCONE_HASH=1 /home/secure-proxy-redis/secure-redis-proxy-0.3.jar > /home/secure-proxy-redis/mrenclave

# Make MRENCLAVE file readonly to all
chmod 444 /home/secure-proxy-redis/mrenclave

# Run redis with provided config file
SCONE_VERSION=1 java -jar -Dspring.config.location=spring-application.yml /home/secure-proxy-redis/secure-redis-proxy-0.3.jar
