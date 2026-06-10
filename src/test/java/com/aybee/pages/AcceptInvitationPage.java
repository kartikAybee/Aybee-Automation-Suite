package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcceptInvitationPage extends BasePage {

    private final By emailField    = By.id("invitee-email");
    private final By passwordField = By.id("password_invite");
    private final By acceptButton  = By.id("accept-invitation-btn");

    @Step("Enter invitation password")
    public AcceptInvitationPage enterPassword(String password) {
        // Bubble.io's reactive state gets stuck when clear() fires before sendKeys —
        // focus → sendKeys → blur avoids the empty-then-typed race condition.
        wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField)).sendKeys(password);
        blurActiveElement();
        return this;
    }

    @Step("Click Accept Invitation")
    public void clickAcceptInvitation() {
        // For already-registered emails Bubble.io permanently blocks client-side
        // validation — the button never reaches the visual enabled state.
        // Fall back to jsClick so the server still processes and returns the error.
        // UnhandledAlertException means the click worked and the server immediately
        // returned a native alert — swallow it so getAndDismissAlreadyRegisteredAlert()
        // can retrieve and verify it in the next step.
        try {
            clickWhenEnabled(acceptButton);
        } catch (UnhandledAlertException ignored) {
            // alert is now open and waiting — handled in the next step
        } catch (Exception e) {
            try {
                jsClick(acceptButton);
            } catch (UnhandledAlertException ignored) {
                // same — alert opened as a result of the click, which is expected
            }
        }
    }

    public boolean isLoaded() {
        return isElementPresent(emailField);
    }

    // Bubble.io reads the email from the invite URL and writes it into the field
    // asynchronously after page load. Wait for that to happen first; fall back to
    // extracting from the URL only if the field never gets populated.
    public String getInviteeEmail() {
        try {
            return new WebDriverWait(driver, 10).until(d -> {
                try {
                    String v = d.findElement(emailField).getAttribute("value");
                    return (v != null && !v.isEmpty()) ? v : null;
                } catch (Exception e) { return null; }
            });
        } catch (Exception ignored) {}

        // Bubble.io didn't populate the field — extract directly from the URL.
        String fromUrl = extractEmailFromUrl();
        return fromUrl != null ? fromUrl : "";
    }

    public boolean isEmailLocked() {
        // Wait for Bubble.io to populate (and lock) the field before checking.
        getInviteeEmail();
        try {
            var el = driver.findElement(emailField);
            if ("true".equalsIgnoreCase(el.getAttribute("readonly"))) return true;
            if (!el.isEnabled()) return true;
            String pe = (String) ((JavascriptExecutor) driver)
                    .executeScript("return window.getComputedStyle(arguments[0]).pointerEvents;", el);
            return "none".equals(pe);
        } catch (Exception e) {
            return false;
        }
    }

    // Returns the error text for an already-registered submission — waits for a native
    // browser alert first (Bubble.io behaviour), then falls back to the page toast.
    public String getAndDismissAlreadyRegisteredAlert() {
        try {
            new WebDriverWait(driver, 10).until(ExpectedConditions.alertIsPresent());
            var alert = driver.switchTo().alert();
            String text = alert.getText();
            alert.accept();
            return text != null ? text : "";
        } catch (Exception ignored) {}
        return getNotificationText();
    }

    // Scans the current URL for anything that looks like an email address — handles both
    // plain-text and URL-encoded (%40) formats that Bubble.io invite tokens may use.
    private String extractEmailFromUrl() {
        try {
            String url = driver.getCurrentUrl();

            // Encoded: xxx%40domain.com
            Matcher encoded = Pattern.compile("([A-Za-z0-9._%+\\-]+%40[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,})")
                    .matcher(url);
            if (encoded.find()) {
                return URLDecoder.decode(encoded.group(1), StandardCharsets.UTF_8.name());
            }

            // Plain: xxx@domain.com
            Matcher plain = Pattern.compile("([A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,})")
                    .matcher(url);
            if (plain.find()) return plain.group(1);

        } catch (Exception ignored) {}
        return null;
    }
}
