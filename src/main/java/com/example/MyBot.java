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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.*;

public class MyBot extends TelegramLongPollingBot {

    private final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    private final String BOT_USERNAME = "FetchifyYT_bot";

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (text.contains("youtube.com") || text.contains("youtu.be")) {
                sendMessage(chatId, "🎬 Choose format:");

                SendMessage msg = new SendMessage();
                msg.setChatId(chatId);
                msg.setText("Select a download option:");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

                buttons.add(Collections.singletonList(createButton("🔝 Best Quality (≤1080p)", text + "|best")));
                buttons.add(Collections.singletonList(createButton("720p", text + "|720p")));
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
            String option = data[1];
            long chatId = query.getMessage().getChatId();

            sendMessage(chatId, "⬇️ Downloading...");

            java.io.File file = downloadWithYtDlp(url, option);
            if (file == null) {
                sendMessage(chatId, "❌ Download failed or file too large.");
                return;
            }

            SendDocument sendDoc = new SendDocument();
            sendDoc.setChatId(chatId);
            sendDoc.setDocument(new InputFile(file));

            try {
                execute(sendDoc);
                file.delete();
            } catch (TelegramApiException e) {
                sendMessage(chatId, "❌ Could not send file.");
                e.printStackTrace();
            }
        }
    }

    private InlineKeyboardButton createButton(String text, String data) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(data);
        return button;
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

    private java.io.File downloadWithYtDlp(String url, String quality) {
        try {
            String filename = quality.equals("mp3") ? "audio.%(ext)s" : "video.%(ext)s";
            List<String> command = new ArrayList<>();
            command.add("yt-dlp");

            if (quality.equals("mp3")) {
                command.addAll(List.of("-x", "--audio-format", "mp3"));
            } else if (quality.equals("720p")) {
                command.add("-f");
                command.add("bestvideo[height<=720]+bestaudio/best[height<=720]");
            } else {
                command.add("-f");
                command.add("bestvideo[height<=1080]+bestaudio/best[height<=1080]");
            }

            command.add("-o");
            command.add(filename);
            command.add(url);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();

            String outFile = quality.equals("mp3") ? "audio.mp3" : "video.mp4";
            java.io.File file = new java.io.File(outFile);
            return file.exists() ? file : null;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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
