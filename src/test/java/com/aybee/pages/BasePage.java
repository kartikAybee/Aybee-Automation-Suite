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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

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

    // Scrolls a WebElement into the centre of the viewport. Used before native .click()
    // so Bubble.io's full mouse-event chain fires on a visible, centred target.
    protected void scrollToCenter(WebElement el) {
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", el);
    }

    // Blurs whatever field currently has focus — triggers Bubble.io's reactive state update.
    protected void blurActiveElement() {
        ((JavascriptExecutor) driver).executeScript("document.activeElement.blur();");
    }

    // Retries on StaleElementReferenceException for same reason as click().
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
    public void dismissToastIfPresent() {
        try {
            new WebDriverWait(driver, 2)
                    .until(ExpectedConditions.elementToBeClickable(By.id("dismiss-toast")))
                    .click();
        } catch (Exception ignored) {}
    }

    // Waits up to 5 s for a browser native alert, returns its text, and dismisses it.
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

    // Extended-timeout variant — for buttons that require server-side setup before enabling
    // (e.g. buttons on experiment creation wizard steps).
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

    // Waits for Bubble.io's backend loading bar to disappear before proceeding.
    // The bar appears as a thin overlay during backend workflow execution and blocks
    // all interactions while visible. We check via JS so we can inspect computed styles
    // rather than relying on a single class name.
    // Update the querySelectorAll argument if the bar uses a different class in this app.
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

    // ── Special-character ID helpers ──────────────────────────────────────────
    // Bubble.io uses full Amazon product names as ID suffixes. These names contain
    // commas, ampersands, and other CSS-special characters that break By.id() and
    // By.cssSelector(). document.getElementById() handles any string safely.

    protected WebElement findById(String id) {
        Object result = ((JavascriptExecutor) driver)
            .executeScript("return document.getElementById(arguments[0]);", id);
        return (result instanceof WebElement) ? (WebElement) result : null;
    }

    // Returns the src of the first <img> inside a container element found by id.
    // Scrolls the image into view first (triggers lazy-load), then polls until
    // naturalWidth > 0 — meaning the real image has loaded and the src is no longer
    // a data: placeholder URI. Returns null if the image never loads within 15s.
    protected String imgSrcFromContainer(String containerId) {
        WebElement container = findById(containerId);
        if (container == null) {
            System.out.println("[BasePage] Container not found: " + containerId);
            return null;
        }
        try {
            WebElement img = container.findElement(By.tagName("img"));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", img);
            new FluentWait<>(driver)
                .withTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .pollingEvery(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> {
                    Object nw = ((JavascriptExecutor) d).executeScript(
                        "return arguments[0].naturalWidth;", img);
                    return nw instanceof Long && (Long) nw > 0;
                });
            String src = img.getAttribute("src");
            return (src != null && !src.startsWith("data:") && !src.isEmpty()) ? src : null;
        } catch (Exception e) {
            return null;
        }
    }

    protected String textById(String id) {
        WebElement el = findById(id);
        if (el == null) return null;
        String text = el.getText();
        return (text != null && !text.isBlank()) ? text.trim() : null;
    }

    // Handles the Bubble.io split-price format: $INT<sup>CENTS</sup>.
    // getText() concatenates all child text including the superscript, giving "$INTCENTS"
    // with no decimal. This method reconstructs the decimal form "$INT.CENTS" via JS
    // so it can be normalised by normalizePrice the same way the shop-setup value is.
    protected String priceTextById(String id) {
        Object raw = ((JavascriptExecutor) driver).executeScript(
            "var el = document.getElementById(arguments[0]);" +
            "if (!el) return null;" +
            "var inner = el.querySelector('div') || el;" +
            "var txt = '';" +
            "for (var i = 0; i < inner.childNodes.length; i++) {" +
            "  if (inner.childNodes[i].nodeType === 3) txt += inner.childNodes[i].textContent;" +
            "}" +
            "var sup = inner.querySelector('sup');" +
            "var supTxt = sup ? sup.textContent.trim() : '';" +
            "return txt.trim() + (supTxt ? '.' + supTxt : '');",
            id);
        String s = (raw instanceof String) ? ((String) raw).trim() : null;
        return (s != null && !s.isEmpty()) ? s : null;
    }

    protected boolean isVisibleById(String id) {
        WebElement el = findById(id);
        if (el == null) return false;
        try { return el.isDisplayed(); } catch (Exception e) { return false; }
    }

    // ── File upload helpers ───────────────────────────────────────────────────

    // Derives a MIME type from the file extension so callers don't hardcode it.
    protected static String mimeTypeFrom(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    // ── QAT uploader (BEP Multi File Uploader) ───────────────────────────────
    // sendKeys on a hidden <input type="file"> already present in the DOM inside
    // the container. Works because the input is opacity:0, not display:none.
    protected void uploadFileToInput(By containerLocator, String absoluteFilePath) {
        WebElement container = wait.until(ExpectedConditions.presenceOfElementLocated(containerLocator));
        container.findElement(By.cssSelector("input[type='file']")).sendKeys(absoluteFilePath);
    }

    // ── Form Questions uploader (custom drag-drop plugin) ─────────────────────
    // JS DataTransfer with real file content.
    // Reads the file from disk in Java, passes the content string to JS, and creates a
    // proper File object so the server receives actual bytes — not an empty payload.
    // Fires the full dragenter → dragover → drop lifecycle on the drop zone.
    protected void uploadFileViaDataTransfer(By dropZoneLocator, String absoluteFilePath, String mimeType) {
        String fileName = absoluteFilePath.substring(absoluteFilePath.lastIndexOf('/') + 1);
        System.out.println("[FileUpload] Approach 3 — JS DataTransfer with real content: " + fileName);
        String base64Content;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(absoluteFilePath));
            base64Content = Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("[FileUpload] Could not read file: " + absoluteFilePath, e);
        }
        WebElement dropZone = wait.until(ExpectedConditions.presenceOfElementLocated(dropZoneLocator));
        ((JavascriptExecutor) driver).executeScript(
            "var zone   = arguments[0];" +
            "var b64    = arguments[1];" +
            "var name   = arguments[2];" +
            "var mime   = arguments[3];" +
            "var bytes  = atob(b64);" +
            "var arr    = new Uint8Array(bytes.length);" +
            "for(var i=0;i<bytes.length;i++) arr[i]=bytes.charCodeAt(i);" +
            "var file = new File([arr], name, {type: mime});" +
            "var dt   = new DataTransfer();" +
            "dt.items.add(file);" +
            "['dragenter','dragover','drop'].forEach(function(t) {" +
            "  zone.dispatchEvent(new DragEvent(t, {bubbles:true, cancelable:true, dataTransfer:dt}));" +
            "});",
            dropZone, base64Content, fileName, mimeType);
        System.out.println("[FileUpload] DataTransfer dispatched (" + base64Content.length() + " base64 chars)");
    }

    // ── Data normalisation helpers ────────────────────────────────────────────

    // Strips everything except digits and "." — turns "$89.99" into "89.99".
    protected static String normalizePrice(String raw) {
        if (raw == null) return null;
        String n = raw.replaceAll("[^0-9.]", "").trim();
        return n.isEmpty() ? null : n;
    }

    // Returns the leading numeric token — turns "1102 ratings" or "1,102" into "1102".
    protected static String extractNumber(String raw) {
        if (raw == null) return null;
        String first = raw.trim().split("\\s+")[0];
        String n = first.replaceAll("[^0-9]", "");
        return n.isEmpty() ? null : n;
    }
}
