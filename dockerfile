FROM eclipse-temurin:17-jdk

# Install yt-dlp and ffmpeg
RUN apt-get update && \
    apt-get install -y python3-pip ffmpeg maven && \
    pip install yt-dlp

# Set working directory
WORKDIR /app

# Copy project files
COPY . /app

# Build the project using Maven
RUN mvn clean package

# Run the application
CMD ["java", "-jar", "target/youtube-telegram-bot-1.0-jar-with-dependencies.jar"]
