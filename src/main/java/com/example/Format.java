package com.example;

public class Format {
    public String format_id;
    public String format;
    public long filesizeMB;

    public Format(String id, String format, long sizeMB) {
        this.format_id = id;
        this.format = format;
        this.filesizeMB = sizeMB;
    }
}
