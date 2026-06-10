package com.aybee.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a timestamped action log across each scenario (fed from BasePage interaction methods)
 * and, at failure time, reads Chrome console + network logs. The combined string is passed to
 * JamManager as the Jam comment so timestamps line up with the recorded video.
 *
 * Requires the LoggingPreferences capability set in BrowserSetup (BROWSER + PERFORMANCE).
 */
public class DiagnosticsCollector {

    private static final List<String> actions = new ArrayList<>();
    private static long startMs = 0;

    // Call in @Before — clears state so logs don't bleed between scenarios.
    public static void reset(long recordingStartMs) {
        actions.clear();
        startMs = recordingStartMs;
    }

    // Called from BasePage interaction methods. Elapsed time is from recording start
    // so timestamps map directly to video positions in the Jam comment.
    public static void recordAction(String description) {
        long elapsed = (System.currentTimeMillis() - startMs) / 1000;
        actions.add(String.format("%02d:%02d  %s", elapsed / 60, elapsed % 60, description));
    }

    // Call exactly once per failure — Chrome's log APIs consume entries on read.
    public static String collectAndFormat(WebDriver driver) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== ACTIONS ===\n");
        if (actions.isEmpty()) sb.append("(none)\n");
        else for (String a : actions) sb.append(a).append("\n");

        sb.append("\n=== CONSOLE ===\n");
        try {
            List<LogEntry> logs = driver.manage().logs().get(LogType.BROWSER).getAll();
            if (logs.isEmpty()) sb.append("(empty)\n");
            else for (LogEntry e : logs)
                sb.append("[").append(e.getLevel()).append("] ").append(e.getMessage()).append("\n");
        } catch (Exception e) {
            sb.append("(unavailable: ").append(e.getMessage()).append(")\n");
        }

        sb.append("\n=== NETWORK ===\n");
        try {
            boolean any = false;
            for (LogEntry e : driver.manage().logs().get(LogType.PERFORMANCE).getAll()) {
                String msg = e.getMessage();
                if (msg.contains("\"Network.requestWillBeSent\"")) {
                    String url = extractString(msg, "url");
                    if (url != null && !isAsset(url)) { sb.append("[REQ] ").append(url).append("\n"); any = true; }
                } else if (msg.contains("\"Network.responseReceived\"")) {
                    String url = extractString(msg, "url");
                    String status = extractValue(msg, "status");
                    int code = -1;
                    try { code = Integer.parseInt(status != null ? status.trim() : ""); } catch (NumberFormatException ignored) {}
                    if (code >= 400) { sb.append("[HTTP ").append(status).append("] ").append(url).append("\n"); any = true; }
                } else if (msg.contains("\"Network.loadingFailed\"")) {
                    String url = extractString(msg, "url");
                    sb.append("[FAILED] ").append(url != null ? url : "?").append("\n"); any = true;
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
        int idx = json.indexOf(needle); if (idx == -1) return null;
        int start = idx + needle.length(), end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    // Extracts an unquoted JSON value (number/boolean): "key":value
    private static String extractValue(String json, String key) {
        if (json.contains("\"" + key + "\":\"")) return extractString(json, key);
        String needle = "\"" + key + "\":";
        int idx = json.indexOf(needle); if (idx == -1) return null;
        int start = idx + needle.length();
        int end = json.indexOf(",", start), end2 = json.indexOf("}", start);
        int realEnd = (end == -1) ? end2 : (end2 == -1) ? end : Math.min(end, end2);
        return realEnd > start ? json.substring(start, realEnd).trim() : null;
    }
}
