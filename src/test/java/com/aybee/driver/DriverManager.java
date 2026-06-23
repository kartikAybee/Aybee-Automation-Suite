package com.aybee.driver;

import com.aybee.utils.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class DriverManager {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();
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
