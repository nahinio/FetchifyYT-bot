package com.example;

import com.google.gson.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;

public class MyBot extends TelegramLongPollingBot {

    private final String BOT_TOKEN = "8441326925:AAGLk-ZAg-2dHGPkJBMzTPRRfd67n8yyHEQ";
    private final String BOT_USERNAME = "FetchifyYT_bot";

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (text.contains("youtube.com") || text.contains("youtu.be")) {
                sendMessage(chatId, "🔎 Fetching formats...");
                List<Format> formats = getAvailableFormats(text);
                if (formats.isEmpty()) {
                    sendMessage(chatId, "❌ No downloadable formats below 2GB found.");
                    return;
                }

                SendMessage msg = new SendMessage();
                msg.setChatId(chatId);
                msg.setText("🎞 Choose resolution:");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

                for (Format f : formats) {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(f.format + " (" + f.filesizeMB + "MB)");
                    button.setCallbackData(text + "|" + f.format_id);

                    buttons.add(Collections.singletonList(button));
                }

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
            String formatId = data[1];
            long chatId = query.getMessage().getChatId();

            sendMessage(chatId, "⬇️ Downloading...");

            File video = downloadVideo(url, formatId);
            if (video == null) {
                sendMessage(chatId, "❌ Failed to download.");
                return;
            }

            SendDocument sendDoc = new SendDocument();
            sendDoc.setChatId(chatId);
            sendDoc.setDocument(new InputFile(video));

            try {
                execute(sendDoc);
                video.delete();
            } catch (TelegramApiException e) {
                sendMessage(chatId, "❌ Could not send file (maybe too large?).");
                e.printStackTrace();
            }
        }
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

    private List<Format> getAvailableFormats(String url) {
        List<Format> filteredFormats = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-F", "--print-json", url);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder json = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            JsonObject obj = JsonParser.parseString(json.toString()).getAsJsonObject();
            JsonArray formats = obj.getAsJsonArray("formats");

            for (JsonElement f : formats) {
                JsonObject format = f.getAsJsonObject();

                String formatId = format.get("format_id").getAsString();
                String formatNote = format.get("format_note").getAsString();
                long fileSize = format.has("filesize") && !format.get("filesize").isJsonNull()
                        ? format.get("filesize").getAsLong()
                        : 0;

                if (fileSize == 0 || fileSize > 2L * 1024 * 1024 * 1024) continue;

                Format fmt = new Format(formatId, formatNote, fileSize / (1024 * 1024));
                filteredFormats.add(fmt);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return filteredFormats;
    }

    private File downloadVideo(String url, String formatId) {
        try {
            String filename = "video.mp4";
            ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-f", formatId, "-o", filename, url);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            File file = new File(filename);
            if (file.exists()) return file;
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

    // Inner class for video format info
    private static class Format {
        String format_id;
        String format;
        long filesizeMB;

        public Format(String format_id, String format, long filesizeMB) {
            this.format_id = format_id;
            this.format = format;
            this.filesizeMB = filesizeMB;
        }
    }
}
