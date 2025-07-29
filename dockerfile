FROM eclipse-temurin:17-jdk

# Install ffmpeg and maven
RUN apt-get update && \
    apt-get install -y ffmpeg maven

# Set working directory to repo root folder
WORKDIR /FetchifyYT-bot

# Copy all project files including yt-dlp binary
COPY . /FetchifyYT-bot

# Make yt-dlp executable
RUN chmod +x ./yt-dlp

# Add working directory to PATH so yt-dlp can be called directly
ENV PATH="/FetchifyYT-bot:${PATH}"

# Build the project using Maven
RUN mvn clean package

# Run the Java application
CMD ["java", "-jar", "target/youtube-telegram-bot-1.0-jar-with-dependencies.jar"]
