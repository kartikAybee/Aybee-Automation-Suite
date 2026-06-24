package com.aybee.utils;

import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {

    private static final Properties properties = new Properties();
    private static final Properties secrets    = new Properties();

    static {
        // config.properties — non-sensitive defaults, committed to version control
        try (InputStream in = ConfigReader.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in == null) throw new RuntimeException("config.properties not found on classpath");
            properties.load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Failed to load config.properties: " + e.getMessage());
        }

        // secrets.properties — sensitive/machine-specific overrides, gitignored (optional)
        try (InputStream in = ConfigReader.class.getClassLoader()
                .getResourceAsStream("secrets.properties")) {
            if (in != null) secrets.load(in);
        } catch (Exception ignored) {}
    }

    public static String get(String key) {
        // Priority: Maven -D flag → env var → secrets.properties → config.properties
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) return value;
        value = System.getenv(key);
        if (value != null && !value.isEmpty()) return value;
        value = secrets.getProperty(key);
        if (value != null && !value.isEmpty()) return value;
        value = properties.getProperty(key);
        if (value == null) throw new RuntimeException("Missing config key: " + key);
        return value;
    }

    public static String get(String key, String defaultValue) {
        try {
            return get(key);
        } catch (RuntimeException e) {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }
}
