# Build stage
FROM maven:3.9.4-eclipse-temurin-17 as build
WORKDIR /app
COPY . .
RUN mvn clean package

# Run stage
FROM eclipse-temurin:17-jdk
RUN apt-get update && \
    apt-get install -y python3-pip ffmpeg && \
    pip install yt-dlp

WORKDIR /app
COPY --from=build /app/target/youtube-telegram-bot-1.0.jar .

CMD ["java", "-jar", "youtube-telegram-bot-1.0.jar"]
