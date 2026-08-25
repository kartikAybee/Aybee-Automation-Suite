package com.aybee.pages;

import com.aybee.driver.DriverManager;
import com.aybee.utils.DiagnosticsCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    // toast-animate-in only exists in the DOM while a toast is actively shown —
    // waiting for it is more reliable than polling toast-message (which may sit empty in DOM).
    private final By toastContainer    = By.id("toast-animate-in");
    private final By notificationToast = By.id("toast-message");

    public BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait   = new WebDriverWait(driver, 15);
    }

    // Two-phase wait: (1) toast-animate-in appears → confirms a toast is live;
    // (2) poll toast-message for text → handles Bubble.io's async text population.
    public String getNotificationText() {
        try {
            new WebDriverWait(driver, 15)
                    .until(ExpectedConditions.presenceOfElementLocated(toastContainer));
            return new WebDriverWait(driver, 5, 100).until(d -> {
                try {
                    String text = driver.findElement(notificationToast).getText();
                    return (text != null && !text.trim().isEmpty()) ? text : null;
                } catch (Exception e) {
                    return null;
                }
            });
        } catch (Exception e) {
            return "";
        }
    }

    public boolean isNotificationVisible() {
        try {
            return !driver.findElement(notificationToast).getText().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // Retries on StaleElementReferenceException — Bubble.io re-renders the DOM reactively,
    // which can invalidate a found element before the click fires.
    protected void click(By locator) {
        DiagnosticsCollector.recordAction("click: " + locator);
        int attempts = 0;
        while (attempts < 3) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
                return;
            } catch (StaleElementReferenceException e) {
                attempts++;
            }
        }
    }

    // JS click bypasses elementToBeClickable — used for Bubble.io Text elements with
    // width:0px (blur targets) and elements obscured by overlapping Bubble.io containers.
    protected void jsClick(By locator) {
        DiagnosticsCollector.recordAction("jsClick: " + locator);
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    // Blurs whatever field currently has focus — triggers Bubble.io's reactive state update
    // without relying on a specific title element ID that may be hidden or absent.
    protected void blurActiveElement() {
        ((JavascriptExecutor) driver).executeScript("document.activeElement.blur();");
    }

    // Retries on StaleElementReferenceException for same reason as click().
    protected void type(By locator, String text) {
        // Mask passwords; truncate long text so the action log stays readable.
        String display = locator.toString().contains("password") ? "***"
            : (text != null && text.length() > 60 ? text.substring(0, 60) + "…" : text);
        DiagnosticsCollector.recordAction("type: " + locator + " → " + display);
        int attempts = 0;
        while (attempts < 3) {
            try {
                WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                // Force-clear before typing: Chrome autofill / Bubble prefill can leave a stale value
                // that native clear() doesn't remove, polluting the field (the typed text concatenates
                // with it). Blank via JS + fire an input event so Bubble registers the clear, then
                // clear() as a belt-and-braces, then type.
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].value=''; arguments[0].dispatchEvent(new Event('input',{bubbles:true}));", el);
                el.clear();
                if (text != null && !text.isEmpty()) {
                    el.sendKeys(text);
                }
                return;
            } catch (StaleElementReferenceException e) {
                attempts++;
            }
        }
    }

    // Uses presenceOfElementLocated — Bubble.io Text divs can have width:0px which
    // causes visibilityOfElementLocated to wrongly report them as invisible.
    protected String getText(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator)).getText();
    }

    protected boolean isElementVisible(By locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean isElementPresent(By locator) {
        try {
            driver.findElement(locator);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Bubble.io buttons have no HTML disabled attribute — enabled state is detected by comparing
    // computed background-color to border-color: they match when filled (enabled) and differ when ghost (disabled).
    protected boolean isButtonEnabled(By locator) {
        try {
            WebElement el = driver.findElement(locator);
            JavascriptExecutor js = (JavascriptExecutor) driver;
            String bg     = (String) js.executeScript("return window.getComputedStyle(arguments[0]).backgroundColor;", el);
            String border = (String) js.executeScript("return window.getComputedStyle(arguments[0]).borderTopColor;", el);
            return bg != null && bg.equals(border);
        } catch (Exception e) {
            return false;
        }
    }

    // Dismisses the active toast by clicking its X button — no-op if no toast is present.
    // Used before asserting an error toast on invite pages where a join-team toast fires first.
    public void dismissToastIfPresent() {
        try {
            new WebDriverWait(driver, 2)
                    .until(ExpectedConditions.elementToBeClickable(By.id("dismiss-toast")))
                    .click();
            // Wait for the toast to actually animate out — otherwise a stale toast lingers and the
            // next notification read grabs it instead of the fresh one that's still loading.
            new WebDriverWait(driver, 5)
                    .until(ExpectedConditions.invisibilityOfElementLocated(toastContainer));
        } catch (Exception ignored) {}
    }

    // Polls the toast text until it CONTAINS expected — a fresh toast may replace a stale one that
    // is still animating out, so a single read can catch the wrong (old) toast. Returns the matching
    // text, or the last text seen on timeout (so the caller's assertion reports what actually showed).
    public String waitForNotificationContaining(String expected, int timeoutSecs) {
        final String[] last = {""};
        try {
            return new WebDriverWait(driver, timeoutSecs, 200).until(d -> {
                try {
                    String text = d.findElement(notificationToast).getText();
                    if (text != null && !text.trim().isEmpty()) last[0] = text;
                    return (text != null && text.contains(expected)) ? text : null;
                } catch (Exception e) {
                    return null;
                }
            });
        } catch (Exception e) {
            return last[0];
        }
    }

    // Waits up to 5 s for a browser native alert, returns its text, and dismisses it.
    // Returns "" if no alert appears within the timeout.
    public String getAndDismissAlert() {
        try {
            new WebDriverWait(driver, 5).until(org.openqa.selenium.support.ui.ExpectedConditions.alertIsPresent());
            var alert = driver.switchTo().alert();
            String text = alert.getText();
            alert.dismiss();
            return text != null ? text : "";
        } catch (Exception e) {
            return "";
        }
    }

    // Waits for the Bubble.io button to reach its visual enabled state, then JS-clicks it.
    // Regular .click() can be swallowed by Bubble.io's event system after the colour check
    // passes — JS click dispatches the event directly on the element, bypassing that gap.
    protected void clickWhenEnabled(By locator) {
        DiagnosticsCollector.recordAction("clickWhenEnabled: " + locator);
        new WebDriverWait(driver, 15).until(d -> isButtonEnabled(locator));
        int attempts = 0;
        while (attempts < 3) {
            try {
                WebElement el = driver.findElement(locator);
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                return;
            } catch (StaleElementReferenceException e) {
                attempts++;
            }
        }
    }
}
