package com.aybee.pages;

import com.aybee.driver.DriverManager;
import com.aybee.utils.DiagnosticsCollector;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.concurrent.TimeUnit;

public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    private final By toastContainer    = By.id("toast-animate-in");
    private final By notificationToast = By.id("toast-message");

    public BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait   = new WebDriverWait(driver, 15);
    }

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

    // Retries on StaleElementReferenceException — Bubble.io re-renders the DOM reactively.
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
    // width:0px and elements obscured by overlapping Bubble.io containers.
    protected void jsClick(By locator) {
        DiagnosticsCollector.recordAction("jsClick: " + locator);
        WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
    }

    protected void scrollToCenter(WebElement el) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", el);
    }

    protected void blurActiveElement() {
        ((JavascriptExecutor) driver).executeScript("document.activeElement.blur();");
    }

    protected void type(By locator, String text) {
        String display = locator.toString().contains("password") ? "***" : (text != null && text.length() > 60 ? text.substring(0, 60) + "…" : text);
        DiagnosticsCollector.recordAction("type: " + locator + " → " + display);
        int attempts = 0;
        while (attempts < 3) {
            try {
                WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
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

    // Bubble.io buttons have no HTML disabled attribute — enabled state detected by comparing
    // computed background-color to border-color: they match when filled (enabled).
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

    public void dismissToastIfPresent() {
        try {
            new WebDriverWait(driver, 2)
                    .until(ExpectedConditions.elementToBeClickable(By.id("dismiss-toast")))
                    .click();
        } catch (Exception ignored) {}
    }

    public String getAndDismissAlert() {
        try {
            new WebDriverWait(driver, 5).until(ExpectedConditions.alertIsPresent());
            var alert = driver.switchTo().alert();
            String text = alert.getText();
            alert.dismiss();
            return text != null ? text : "";
        } catch (Exception e) {
            return "";
        }
    }

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

    protected void clickWhenEnabled(By locator, int timeoutSeconds) {
        DiagnosticsCollector.recordAction("clickWhenEnabled: " + locator);
        new WebDriverWait(driver, timeoutSeconds).until(d -> isButtonEnabled(locator));
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

    protected void waitForBubbleReady() {
        new FluentWait<>(driver)
            .withTimeout(10, TimeUnit.SECONDS)
            .pollingEvery(150, TimeUnit.MILLISECONDS)
            .ignoring(Exception.class)
            .until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                "var els = document.querySelectorAll('[class*=\"loading-bar\"],[class*=\"progress-bar\"],[class*=\"backend-loading\"]');" +
                "for (var i = 0; i < els.length; i++) {" +
                "  var s = window.getComputedStyle(els[i]);" +
                "  if (s.display !== 'none' && s.visibility !== 'hidden' && parseFloat(s.opacity || '1') > 0) return false;" +
                "}" +
                "return true;"));
    }

    protected WebElement findById(String id) {
        Object result = ((JavascriptExecutor) driver)
            .executeScript("return document.getElementById(arguments[0]);", id);
        return (result instanceof WebElement) ? (WebElement) result : null;
    }

    protected boolean isVisibleById(String id) {
        WebElement el = findById(id);
        if (el == null) return false;
        try { return el.isDisplayed(); } catch (Exception e) { return false; }
    }

    // ── File upload helper ────────────────────────────────────────────────────
    // The Bubble BEP Multi File Uploader renders a native <input type="file"> with
    // opacity:0 (not display:none) inside the drop zone container. WebDriver's
    // file-upload sendKeys path bypasses the visibility guard and writes the path
    // directly to the input. Scope containerLocator to the drop-zone id so the
    // correct slot is targeted when multiple uploaders exist on the page.
    protected void uploadFileToInput(By containerLocator, String absoluteFilePath) {
        WebElement container = wait.until(ExpectedConditions.presenceOfElementLocated(containerLocator));
        container.findElement(By.cssSelector("input[type='file']")).sendKeys(absoluteFilePath);
    }

    // Bubble.io occasionally fails to render a step's content on initial load (blank / infinite
    // loading spinner); a page reload reliably brings it up. Waits for ANY given landmark to be PRESENT;
    // if none appear within timeoutSecs it reloads the page and waits again, up to maxReloads reloads.
    protected boolean waitForAnyLandmarkElseReload(java.util.List<By> landmarks, int timeoutSecs, int maxReloads) {
        for (int attempt = 0; attempt <= maxReloads; attempt++) {
            try {
                new WebDriverWait(driver, timeoutSecs).until((org.openqa.selenium.WebDriver d) -> {
                    for (By by : landmarks) { if (!d.findElements(by).isEmpty()) return true; }
                    return false;
                });
                if (attempt > 0) System.out.println("[Reload] Landmark appeared after reload " + attempt);
                return true;
            } catch (Exception e) {
                if (attempt < maxReloads) {
                    System.out.println("[Reload] None of " + landmarks + " within " + timeoutSecs + "s — reloading (" + (attempt + 1) + "/" + maxReloads + ")");
                    try { driver.navigate().refresh(); } catch (Exception ignored) {}
                }
            }
        }
        System.out.println("[Reload] Landmarks " + landmarks + " never appeared after " + maxReloads + " reload(s)");
        return false;
    }
    protected boolean waitForLandmarkElseReload(By landmark, int timeoutSecs, int maxReloads) {
        return waitForAnyLandmarkElseReload(java.util.Collections.singletonList(landmark), timeoutSecs, maxReloads);
    }
}
