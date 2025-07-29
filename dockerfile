FROM eclipse-temurin:17-jdk

# Install yt-dlp and ffmpeg
RUN apt-get update && \
    apt-get install -y python3-pip ffmpeg && \
    pip install yt-dlp

WORKDIR /app
COPY . /app

RUN ./mvnw package

CMD ["java", "-jar", "target/youtube-telegram-bot-1.0.jar"]
