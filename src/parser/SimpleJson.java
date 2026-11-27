package parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleJson {
    private SimpleJson() {
    }

    public static String quote(String value) {
        if (value == null) return "null";
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    public static String extractString(String json, String key) {
        if (json == null || key == null) return null;
        String cleaned = stripCodeFences(json).trim();
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(\"(?:\\\\.|[^\"\\\\])*\"|null)");
        Matcher matcher = pattern.matcher(cleaned);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1);
        if ("null".equals(raw)) {
            return null;
        }
        return unescape(raw.substring(1, raw.length() - 1));
    }

    public static String extractFirstStringValue(String json, String key) {
        return extractString(json, key);
    }

    public static String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private static String unescape(String s) {
        return s.replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}