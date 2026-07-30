package com.aybee.utils;

import java.awt.Toolkit;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class JamManager {

    // Set JAM_UPLOAD_ENABLED=true in config.properties to activate recording and upload.
    // Default is false so normal test runs stay fast and don't fill disk.
    private static final boolean ENABLED = ConfigReader.getBoolean("JAM_UPLOAD_ENABLED", false);

    private static Process recordingProcess;
    private static String  currentVideoPath;

    // ── Recording ─────────────────────────────────────────────────────────────

    // No-arg overload kept so existing call sites that don't have a label still compile.
    public static void startRecording() {
        startRecording("scenario");
    }

    public static void startRecording(String label) {
        if (!ENABLED) return;
        try {
            Files.createDirectories(Paths.get("target/recordings"));
            String filename  = System.currentTimeMillis() + "_" + label.replaceAll("[^a-zA-Z0-9_-]", "_") + ".mp4";
            currentVideoPath = Paths.get("target/recordings", filename).toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-f",       "avfoundation",
                "-framerate", "30",
                "-i",       "1",           // display 1 — main screen on this machine
                "-vcodec",  "libx264",
                "-preset",  "ultrafast",   // keep CPU load low during test run
                "-pix_fmt", "yuv420p",
                "-y",       currentVideoPath
            );
            // Suppress all ffmpeg console output so it doesn't pollute the test log.
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            recordingProcess = pb.start();
            System.out.println("[Jam] Recording started → " + currentVideoPath);
        } catch (Exception e) {
            System.out.println("[Jam] Could not start recording: " + e.getMessage());
            recordingProcess  = null;
            currentVideoPath  = null;
        }
    }

    // ── Jam upload ────────────────────────────────────────────────────────────

    public static String stopAndGetLink() {
        return stopAndUpload("", "Test failure", "");
    }

    public static String stopAndUpload(String pageUrl, String title, String comment) {
        if (!ENABLED) return null;
        stopRecording();

        if (currentVideoPath == null || !new File(currentVideoPath).exists()) {
            System.out.println("[Jam] No video file found — skipping upload.");
            return null;
        }

        try {
            String json   = buildJamJson(pageUrl, title, currentVideoPath);
            String output = runCommand(60, "jam", "create", "jam", json);
            System.out.println("[Jam] Upload response: " + output);

            String jamId  = extractJsonString(output, "id");
            String jamUrl = extractJsonString(output, "url");

            if (jamId != null) {
                addComment(jamId, comment);
            }

            currentVideoPath = null;
            return jamUrl;
        } catch (Exception e) {
            System.out.println("[Jam] Upload failed: " + e.getMessage());
            return null;
        }
    }

    public static void addComment(String jamId, String comment) {
        if (!ENABLED) return;
        try {
            runCommand(30, "jam", "create", "comment", jamId, comment);
            System.out.println("[Jam] Comment added to " + jamId);
        } catch (Exception e) {
            System.out.println("[Jam] Comment failed: " + e.getMessage());
        }
    }

    // On pass — stop and discard the video so disk isn't filled with passing-run footage.
    public static void discardRecording() {
        if (!ENABLED) return;
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
                if (!recordingProcess.waitFor(15, TimeUnit.SECONDS)) {
                    recordingProcess.destroy();
                }
            } catch (Exception e) {
                recordingProcess.destroy();
            }
        }
        recordingProcess = null;
    }

    private static String runCommand(int timeoutSecs, String... args) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectError(ProcessBuilder.Redirect.DISCARD);
        Process proc = pb.start();
        String output = new String(proc.getInputStream().readAllBytes()).trim();
        proc.waitFor(timeoutSecs, TimeUnit.SECONDS);
        return output;
    }

    private static String buildJamJson(String url, String title, String videoPath) {
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
