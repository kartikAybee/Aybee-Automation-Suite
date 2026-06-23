package com.aybee.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.util.ArrayList;
import java.util.List;

public class DiagnosticsCollector {

    private static final List<String> actions = new ArrayList<>();
    private static long startMs = 0;

    // Called at the start of each scenario so logs don't bleed between cases.
    public static void reset(long recordingStartMs) {
        actions.clear();
        startMs = recordingStartMs;
    }

    // Called from BasePage interaction methods — timestamps are relative to recording start
    // so they line up with the video the Jam comment is attached to.
    public static void recordAction(String description) {
        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
        actions.add(String.format("%02d:%02d  %s", elapsed / 60, elapsed % 60, description));
    }

    // Reads Chrome console + network logs and combines them with the action log into a
    // single string suitable for a Jam comment. Capped at 5 000 chars to stay within
    // Jam's comment size limit.
    public static String collectAndFormat(WebDriver driver) {
        StringBuilder sb = new StringBuilder();

        // Actions
        sb.append("=== ACTIONS ===\n");
        if (actions.isEmpty()) {
            sb.append("(none)\n");
        } else {
            for (String a : actions) sb.append(a).append("\n");
        }

        // Browser console
        sb.append("\n=== CONSOLE ===\n");
        try {
            List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
            if (logs.isEmpty()) {
                sb.append("(empty)\n");
            } else {
                for (LogEntry e : logs) {
                    sb.append("[").append(e.getLevel()).append("] ").append(e.getMessage()).append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("(unavailable: ").append(e.getMessage()).append(")\n");
        }

        // Network — API requests + non-2xx responses + load failures
        sb.append("\n=== NETWORK ===\n");
        try {
            List<LogEntry> perfLogs = driver.manage().logs().get(LogType.PERFORMANCE).getAll();
            boolean any = false;
            for (LogEntry e : perfLogs) {
                String msg = e.getMessage();
                if (msg.contains("\"Network.requestWillBeSent\"")) {
                    String url = extractString(msg, "url");
                    if (url != null && !isAsset(url)) {
                        sb.append("[REQ] ").append(url).append("\n");
                        any = true;
                    }
                } else if (msg.contains("\"Network.responseReceived\"")) {
                    String url    = extractString(msg, "url");
                    String status = extractValue(msg, "status");
                    int code = -1;
                    try { code = Integer.parseInt(status != null ? status.trim() : ""); } catch (NumberFormatException ignored) {}
                    if (code >= 400) {
                        sb.append("[HTTP ").append(status).append("] ").append(url).append("\n");
                        any = true;
                    }
                } else if (msg.contains("\"Network.loadingFailed\"")) {
                    String url = extractString(msg, "url");
                    sb.append("[FAILED] ").append(url != null ? url : "?").append("\n");
                    any = true;
                }
            }
            if (!any) sb.append("(no errors)\n");
        } catch (Exception e) {
            sb.append("(unavailable: ").append(e.getMessage()).append(")\n");
        }

        String result = sb.toString();
        return result.length() > 5000 ? result.substring(0, 5000) + "\n...(truncated)" : result;
    }

    private static boolean isAsset(String url) {
        return url.contains(".js") || url.contains(".css") || url.contains(".png")
            || url.contains(".svg") || url.contains(".woff") || url.contains("font");
    }

    // Extracts a quoted JSON string value: "key":"value"
    private static String extractString(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int idx = json.indexOf(needle);
        if (idx == -1) return null;
        int start = idx + needle.length();
        int end   = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    // Extracts an unquoted JSON value (number/boolean): "key":value
    private static String extractValue(String json, String key) {
        if (json.contains("\"" + key + "\":\"")) return extractString(json, key);
        String needle = "\"" + key + "\":";
        int idx = json.indexOf(needle);
        if (idx == -1) return null;
        int start = idx + needle.length();
        int end   = json.indexOf(",", start);
        int end2  = json.indexOf("}", start);
        int realEnd = (end == -1) ? end2 : (end2 == -1) ? end : Math.min(end, end2);
        return realEnd > start ? json.substring(start, realEnd).trim() : null;
    }
}
