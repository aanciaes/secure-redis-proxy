FROM gradle:6.0.1-jdk8

# Set the work dir and run the build
WORKDIR /home/gradle/project

# Copy project to container
COPY ./ /home/gradle/project

# Build Project
RUN gradle clean build

# Cmd
CMD gradle run
