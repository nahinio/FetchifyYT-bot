package com.example;

import com.google.gson.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.InputFile;

import java.util.*;
import java.io.*;

public class MyBot extends TelegramLongPollingBot {

    private final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    private final String BOT_USERNAME = "FetchifyYT_bot";

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (text.contains("youtube.com") || text.contains("youtu.be")) {
                sendMessage(chatId, "🎬 Choose download option:");

                SendMessage msg = new SendMessage();
                msg.setChatId(chatId);
                msg.setText("Select format:");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

                buttons.add(Collections.singletonList(createButton("🎥 Best Quality (1080p)", text + "|best")));
                buttons.add(Collections.singletonList(createButton("📽 720p", text + "|720")));
                buttons.add(Collections.singletonList(createButton("🎵 MP3", text + "|mp3")));

                markup.setKeyboard(buttons);
                msg.setReplyMarkup(markup);

                try {
                    execute(msg);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {
                sendMessage(chatId, "⚠️ Please send a valid YouTube URL.");
            }

        } else if (update.hasCallbackQuery()) {
            CallbackQuery query = update.getCallbackQuery();
            String[] data = query.getData().split("\\|");
            String url = data[0];
            String type = data[1];
            long chatId = query.getMessage().getChatId();

            sendMessage(chatId, "⬇️ Downloading...");

            java.io.File file = null;

            switch (type) {
                case "best":
                    file = downloadWithYtDlp(url, "bestvideo[height<=1080]+bestaudio/best[height<=1080]");
                    break;
                case "720":
                    file = downloadWithYtDlp(url, "bestvideo[height=720]+bestaudio/best[height=720]");
                    break;
                case "mp3":
                    file = downloadWithYtDlp(url, "bestaudio", true);
                    break;
            }

            if (file == null) {
                sendMessage(chatId, "❌ Failed to download.");
                return;
            }

            SendDocument sendDoc = new SendDocument();
            sendDoc.setChatId(chatId);
            sendDoc.setDocument(new InputFile(file));

            try {
                execute(sendDoc);
                file.delete();
            } catch (TelegramApiException e) {
                sendMessage(chatId, "❌ Could not send file (maybe too large?).");
                e.printStackTrace();
            }
        }
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    private java.io.File downloadWithYtDlp(String url, String format) {
        return downloadWithYtDlp(url, format, false);
    }

    private java.io.File downloadWithYtDlp(String url, String format, boolean isAudioOnly) {
        try {
            String output = isAudioOnly ? "audio.%(ext)s" : "video.%(ext)s";

            List<String> command = new ArrayList<>();
            command.add("yt-dlp");
            command.add("-f");
            command.add(format);
            command.add("-o");
            command.add(output);

            if (isAudioOnly) {
                command.add("-x");
                command.add("--audio-format");
                command.add("mp3");
            }

            command.add(url);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            // Find downloaded file
            File dir = new File(".");
            File[] files = dir.listFiles((d, name) -> name.startsWith(isAudioOnly ? "audio" : "video"));
            if (files != null && files.length > 0) {
                return files[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendMessage(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }
}
