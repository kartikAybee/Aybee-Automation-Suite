package com.aybee.utils;

import com.aybee.driver.DriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GoogleAuthHelper {

    private static final String GOOGLE_EMAIL    = ConfigReader.get("GOOGLE_TEST_EMAIL");
    private static final String GOOGLE_PASSWORD = ConfigReader.get("GOOGLE_TEST_PASSWORD");

    // Handles the Google OAuth popup: switches to the popup window, enters credentials,
    // and returns to the main window once auth completes or an error page is shown.
    public static void handleGoogleAuthPopup() {
        WebDriver driver = DriverManager.getDriver();
        String mainWindow = driver.getWindowHandle();

        // Wait for the OAuth popup to open
        WebDriverWait wait = new WebDriverWait(driver, 15);
        wait.until(d -> d.getWindowHandles().size() > 1);

        // Switch to the popup
        Set<String> handles = driver.getWindowHandles();
        for (String handle : handles) {
            if (!handle.equals(mainWindow)) {
                driver.switchTo().window(handle);
                break;
            }
        }

        try {
            // If a Chrome profile with Google already signed in is configured,
            // the account picker appears instead of the login form.
            // Try the account picker first, fall back to credentials entry.
            handleEmailStep(driver, wait);
            handlePasswordStep(driver, wait);
        } catch (Exception e) {
            // Account may already be selected from Chrome profile — continue
            System.out.println("Google auth step skipped (likely pre-authenticated): " + e.getMessage());
        }

        // Wait for popup to close (Google redirects back to the app)
        wait.until(d -> d.getWindowHandles().size() == 1);
        driver.switchTo().window(mainWindow);
    }

    private static void handleEmailStep(WebDriver driver, WebDriverWait wait) {
        By emailInput = By.cssSelector("input[type='email']");
        By nextButton = By.cssSelector("#identifierNext button, button[jsname='LgbsSe']");
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(emailInput))
                .sendKeys(GOOGLE_EMAIL);
            wait.until(ExpectedConditions.elementToBeClickable(nextButton)).click();
        } catch (Exception ignored) {
        }
    }

    private static void handlePasswordStep(WebDriver driver, WebDriverWait wait) {
        By passwordInput = By.cssSelector("input[type='password']");
        By nextButton    = By.cssSelector("#passwordNext button, button[jsname='LgbsSe']");
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(passwordInput))
                .sendKeys(GOOGLE_PASSWORD);
            wait.until(ExpectedConditions.elementToBeClickable(nextButton)).click();
        } catch (Exception ignored) {
        }
    }
}
