package com.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MyBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return System.getenv("BOT_USERNAME");
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.startsWith("https://") || messageText.startsWith("http://")) {
                sendFormatOptions(chatId, messageText);
            } else {
                sendTextMessage(chatId, "Send a valid YouTube link.");
            }
        } else if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            String[] parts = data.split("\|", 2);
            if (parts.length == 2) {
                String option = parts[0];
                String url = parts[1];
                long chatId = update.getCallbackQuery().getMessage().getChatId();
                handleDownloadOption(chatId, url, option);
            }
        }
    }

    private void sendFormatOptions(long chatId, String url) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        buttons.add(List.of(InlineKeyboardButton.builder().text("Best (1080p)").callbackData("best|" + url).build()));
        buttons.add(List.of(InlineKeyboardButton.builder().text("720p").callbackData("720|" + url).build()));
        buttons.add(List.of(InlineKeyboardButton.builder().text("MP3").callbackData("mp3|" + url).build()));

        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder().keyboard(buttons).build();

        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Choose a format to download:")
                .replyMarkup(markup)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleDownloadOption(long chatId, String url, String option) {
        sendTextMessage(chatId, "Downloading " + option + " version. Please wait...");

        java.io.File file = downloadWithYtDlp(url, option);
        if (file == null) {
            sendTextMessage(chatId, "Failed to download. The format may not be available or the file is too large.");
            return;
        }

        SendDocument document = SendDocument.builder()
                .chatId(String.valueOf(chatId))
                .document(new org.telegram.telegrambots.meta.api.objects.InputFile(file))
                .build();

        try {
            execute(document);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendTextMessage(chatId, "Error sending file: " + e.getMessage());
        }

        file.delete();
    }

    private java.io.File downloadWithYtDlp(String url, String option) {
        String format;
        switch (option) {
            case "best":
                format = "bestvideo[height<=1080]+bestaudio/best[height<=1080]";
                break;
            case "720":
                format = "bestvideo[height<=720]+bestaudio/best[height<=720]";
                break;
            case "mp3":
                format = "bestaudio";
                break;
            default:
                return null;
        }

        String output = "output.%(ext)s";
        List<String> command = new ArrayList<>();
        command.add("yt-dlp");
        command.add("-f");
        command.add(format);
        command.add("-o");
        command.add(output);

        if (option.equals("mp3")) {
            command.add("--extract-audio");
            command.add("--audio-format");
            command.add("mp3");
        }

        command.add(url);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                String extension = option.equals("mp3") ? "mp3" : "mp4";
                java.io.File downloadedFile = new java.io.File("output." + extension);
                return downloadedFile.exists() ? downloadedFile : null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void sendTextMessage(long chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text(text)
                .build();

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}