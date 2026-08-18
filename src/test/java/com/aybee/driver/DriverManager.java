package com.aybee.driver;

import com.aybee.utils.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class DriverManager {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
    // Held separately so the shutdown hook can quit after the suite finishes.
    private static volatile WebDriver activeDriver;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if ("true".equalsIgnoreCase(ConfigReader.get("KEEP_BROWSER_OPEN"))) return;
            if (activeDriver != null) {
                activeDriver.quit();
                activeDriver = null;
            }
        }));
    }

    public static WebDriver getDriver() {
        if (driver.get() == null) {
            WebDriver d = createDriver();
            driver.set(d);
            activeDriver = d;
        }
        return driver.get();
    }

    // Available for explicit teardown if ever needed, but NOT called between scenarios —
    // the browser is reused across the entire suite and quit only by the JVM shutdown hook.
    public static void quitDriver() {
        if (driver.get() != null) {
            driver.get().quit();
            driver.remove();
            activeDriver = null;
        }
    }

    private static WebDriver createDriver() {
        System.setProperty("webdriver.chrome.driver", ConfigReader.get("CHROME_DRIVER_PATH"));
        return new ChromeDriver(BrowserSetup.getChromeOptions());
    }
}
