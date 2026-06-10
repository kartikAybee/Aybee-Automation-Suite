package com.aybee.utils;

import java.awt.Toolkit;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Records each scenario with ffmpeg and, on failure, uploads the MP4 via the `jam` CLI,
 * adds a diagnostics comment, and returns the shareable URL. On pass the video is deleted.
 *
 * Gated by the JAM_ENABLED config flag — when false, every method no-ops so the suite runs
 * unaffected. ffmpeg and the `jam` CLI are assumed pre-installed on PATH when enabled.
 *
 * Static state is fine because scenarios run sequentially (one recording at a time).
 */
public class JamManager {

    private static Process recordingProcess;
    private static String  currentVideoPath;

    // Read once per scenario in startRecording so stop/discard agree with start.
    public static boolean isEnabled() {
        return ConfigReader.getBoolean("JAM_ENABLED", false);
    }

    // ── Recording ─────────────────────────────────────────────────────────────

    // No-arg overload kept so existing call sites that don't have a label still compile.
    public static void startRecording() {
        startRecording("scenario");
    }

    public static void startRecording(String label) {
        if (!isEnabled()) {
            recordingProcess = null;
            currentVideoPath = null;
            return;
        }
        try {
            Files.createDirectories(Paths.get("target/recordings"));
            String filename  = System.currentTimeMillis() + "_"
                + label.replaceAll("[^a-zA-Z0-9_-]", "_") + ".mp4";
            currentVideoPath = Paths.get("target/recordings", filename).toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-f",         "avfoundation",
                "-framerate", "30",
                "-i",         "1",            // display 1 — main screen (macOS avfoundation)
                "-vcodec",    "libx264",
                "-preset",    "ultrafast",    // keep CPU load low during the test run
                "-pix_fmt",   "yuv420p",
                "-y",         currentVideoPath
            );
            // Suppress ffmpeg output so it doesn't pollute the test log.
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            recordingProcess = pb.start();
            System.out.println("[Jam] Recording started → " + currentVideoPath);
        } catch (Exception e) {
            System.out.println("[Jam] Could not start recording: " + e.getMessage());
            recordingProcess = null;
            currentVideoPath = null;
        }
    }

    // ── Jam upload ────────────────────────────────────────────────────────────

    // Backward-compat no-arg overload — delegates to the three-arg version.
    public static String stopAndGetLink() {
        return stopAndUpload("", "Test failure", "");
    }

    public static String stopAndUpload(String pageUrl, String title, String comment) {
        stopRecording();
        if (currentVideoPath == null || !new File(currentVideoPath).exists()) {
            return null;
        }
        try {
            String json   = buildJamJson(pageUrl, title, currentVideoPath);
            String output = runCommand(60, "jam", "create", "jam", json);
            System.out.println("[Jam] Upload response: " + output);

            String jamId  = extractJsonString(output, "id");
            String jamUrl = extractJsonString(output, "url");
            if (jamId != null) addComment(jamId, comment);

            currentVideoPath = null;
            return jamUrl;
        } catch (Exception e) {
            System.out.println("[Jam] Upload failed: " + e.getMessage());
            return null;
        }
    }

    public static void addComment(String jamId, String comment) {
        try {
            runCommand(30, "jam", "create", "comment", jamId, comment);
            System.out.println("[Jam] Comment added to " + jamId);
        } catch (Exception e) {
            System.out.println("[Jam] Comment failed: " + e.getMessage());
        }
    }

    // On pass — stop and discard so disk isn't filled with passing-run footage.
    public static void discardRecording() {
        stopRecording();
        if (currentVideoPath != null) {
            try { Files.deleteIfExists(Paths.get(currentVideoPath)); } catch (Exception ignored) {}
            currentVideoPath = null;
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private static void stopRecording() {
        if (recordingProcess != null && recordingProcess.isAlive()) {
            try {
                // 'q' on stdin is ffmpeg's graceful-stop signal — finalises the MP4 container.
                recordingProcess.getOutputStream().write('q');
                recordingProcess.getOutputStream().flush();
                if (!recordingProcess.waitFor(15, TimeUnit.SECONDS)) recordingProcess.destroy();
            } catch (Exception e) {
                recordingProcess.destroy();
            }
        }
        recordingProcess = null;
    }

    private static String runCommand(int timeoutSecs, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);  // no shell quoting — args passed individually
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process proc = pb.start();
        String output = new String(proc.getInputStream().readAllBytes()).trim();
        proc.waitFor(timeoutSecs, TimeUnit.SECONDS);
        return output;
    }

    private static String buildJamJson(String url, String title, String videoPath) {
        // Read screen dimensions at runtime so they're accurate on any machine — never hardcode.
        java.awt.Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = (int) screen.getWidth();
        int h = (int) screen.getHeight();
        return "{"
            + "\"url\":\""       + escapeJson(url)       + "\","
            + "\"title\":\""     + escapeJson(title)     + "\","
            + "\"kind\":\"video\","
            + "\"videoPath\":\"" + escapeJson(videoPath) + "\","
            + "\"screenDimensions\":{\"width\":" + w + ",\"height\":" + h + "}"
            + "}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private static String extractJsonString(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int idx = json.indexOf(needle);
        if (idx == -1) return null;
        int start = idx + needle.length();
        int end   = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }
}
