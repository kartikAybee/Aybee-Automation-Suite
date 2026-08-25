package com.aybee.driver;

import com.aybee.utils.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DriverManager {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    // Every session ever created is tracked so the shutdown hook can quit them all — the
    // join-request flow runs two concurrent sessions (admin + requester).
    private static final List<WebDriver> allDrivers = new CopyOnWriteArrayList<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (WebDriver d : allDrivers) {
                try { d.quit(); } catch (Exception ignored) {}
            }
            allDrivers.clear();
        }));
    }

    public static WebDriver getDriver() {
        if (driver.get() == null) {
            driver.set(newDriver());
        }
        return driver.get();
    }

    // Creates an independent browser session (fresh temp profile) WITHOUT changing the active
    // session. Used to run a second concurrent session alongside the default one.
    public static WebDriver newDriver() {
        WebDriver d = createDriver();
        allDrivers.add(d);
        return d;
    }

    // Points subsequent getDriver() calls (and page objects created afterward) at the given session.
    public static void setDriver(WebDriver d) {
        driver.set(d);
    }

    // Available for explicit teardown if ever needed, but not called between scenarios.
    public static void quitDriver() {
        WebDriver d = driver.get();
        if (d != null) {
            try { d.quit(); } catch (Exception ignored) {}
            allDrivers.remove(d);
            driver.remove();
        }
    }

    private static WebDriver createDriver() {
        System.setProperty("webdriver.chrome.driver", ConfigReader.get("CHROME_DRIVER_PATH"));
        return new ChromeDriver(BrowserSetup.getChromeOptions());
    }
}
