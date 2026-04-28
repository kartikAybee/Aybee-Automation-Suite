package com.aybee.driver;

import com.aybee.utils.ConfigReader;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.HashMap;
import java.util.Map;

public class BrowserSetup {

    public static ChromeOptions getChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        boolean headless = ConfigReader.getBoolean("BROWSER_HEADLESS", false);

        if (headless) {
            options.addArguments("--headless");
        }

        options.addArguments(
                "--disable-gpu",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-popup-blocking",
                "--disable-infobars",
                "--disable-notifications",
                "--disable-default-apps",
                "--window-size=1920,1080",
                "--remote-allow-origins=*"
        );

        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.default_content_setting_values.notifications", 2);

        // Pre-grant clipboard-read/write so navigator.clipboard.readText() works without a prompt.
        Map<String, Object> clipboardAllow = new HashMap<>();
        clipboardAllow.put("last_modified", "13000000000000000");
        clipboardAllow.put("setting", 1);
        Map<String, Object> clipboardExceptions = new HashMap<>();
        clipboardExceptions.put("https://platform.aybee.ai:443,*", clipboardAllow);
        prefs.put("profile.content_settings.exceptions.clipboard", clipboardExceptions);
        prefs.put("profile.content_settings.exceptions.clipboard-read-write", clipboardExceptions);

        options.setExperimentalOption("prefs", prefs);

        return options;
    }
}
