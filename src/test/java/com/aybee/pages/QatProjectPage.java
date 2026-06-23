package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QatProjectPage extends BasePage {

    // RGshop confirms the page is ready — same landmark used by CreateProjectPage.
    private static final By STAGE_LANDMARK = By.id("RGshop");

    // Bubble BEP Multi File Uploader drop zones — each wraps a hidden <input type="file">.
    private final By dropZoneA = By.id("drop-zone-A-1");
    private final By dropZoneB = By.id("drop-zone-B-2");

    // A per-version delete control renders only after that version's upload finishes
    // processing server-side, so its appearance is the reliable sync point that the
    // upload completed. IDs are unique per version (delete-version-A / delete-version-B).
    private final By deleteVersionA = By.id("delete-version-A");
    private final By deleteVersionB = By.id("delete-version-B");

    // Advances from the asset-upload stage to the form-questions stage. Bubble keeps it
    // disabled (CSS, not the disabled attribute) until both uploads complete.
    private final By assetsNextButton = By.id("qat_assets_next_button");

    // First control on the form-questions stage — its clickable appearance confirms the
    // stage rendered and is interactive.
    private final By addQuestionButton = By.id("newproject_formquestions_addquestion_button");

    // Every uploaded creative renders into a div with this (shared, non-unique) id, with the
    // CDN image url on its inner <img>. We diff the rendered srcs against a pre-upload baseline
    // to attribute the newly-appeared src to the version that was just uploaded — robust to the
    // shared id and to any placeholder image shown before upload.
    private final Set<String> capturedCreativeSrcs = new HashSet<>();

    public boolean isLoaded() {
        return isElementVisible(STAGE_LANDMARK);
    }

    // Extracts a stable key for image comparison across views. Bubble's CDN embeds responsive
    // transform params in the PATH (/cdn-cgi/image/w=384,h=347,f=auto,dpr=2,fit=contain/) which
    // differ between the upload view (w=384) and the preview/selection view (w=768/1536). Strip
    // the transform segment and return just the file ID + filename, e.g.:
    //   "f1781238198245x878232137004073700/pringles.png"
    // This is stable regardless of which width/dpr the CDN was asked for.
    // Returns null for data: URIs (placeholder gifs) so they are excluded from comparisons.
    public static String creativeKey(String src) {
        if (src == null) return null;
        if (src.startsWith("data:")) return null;
        int cdnIdx = src.indexOf("/cdn-cgi/image/");
        if (cdnIdx >= 0) {
            String afterCdn = src.substring(cdnIdx + "/cdn-cgi/image/".length());
            int slash = afterCdn.indexOf('/');
            if (slash >= 0) return afterCdn.substring(slash + 1); // "f1234x5678/filename.ext"
        }
        // Fallback for legacy ?-query urls
        int q = src.indexOf('?');
        return q >= 0 ? src.substring(0, q) : src;
    }

    // Reads the inner-img src of every #creative-image-cell currently rendered. querySelectorAll
    // (unlike getElementById) returns ALL elements sharing the id, so both versions are seen.
    @SuppressWarnings("unchecked")
    private List<String> currentCreativeSrcs() {
        Object res = ((JavascriptExecutor) driver).executeScript(
            "return Array.prototype.slice.call(" +
            "  document.querySelectorAll('#creative-image-cell img'))" +
            "  .map(function(i){ return i.currentSrc || i.src; });");
        List<String> out = new ArrayList<>();
        if (res instanceof List) {
            for (Object o : (List<Object>) res) {
                if (o != null && !o.toString().isEmpty()) out.add(o.toString());
            }
        }
        return out;
    }

    // Seeds the baseline with whatever creatives are already on screen (placeholders, etc.)
    // so the first real upload is detected as "new". Call once, before the first upload.
    @Step("Snapshot baseline creative images before uploading")
    public QatProjectPage snapshotCreativeBaseline() {
        capturedCreativeSrcs.addAll(currentCreativeSrcs());
        return this;
    }

    // Polls (up to 10s) for a rendered creative src not seen before, records it, and returns
    // it. Returns null if nothing new appeared — caller soft-asserts on that.
    // Polls until a new, non-placeholder CDN src appears (skips data: URIs which are loading
    // placeholders). Waits up to 20s to allow Bubble's backend to process the upload and
    // inject the real CDN url into the image cell after the delete marker appears.
    private String newlyAddedCreativeSrc() {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            for (String src : currentCreativeSrcs()) {
                if (!capturedCreativeSrcs.contains(src) && !src.startsWith("data:")) {
                    capturedCreativeSrcs.add(src);
                    return src;
                }
            }
            try { Thread.sleep(300); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        return null;
    }

    // Uploads version A, waits for its upload to finish (delete-version-A), and returns the
    // newly-rendered creative src so the caller can store it for later image comparison.
    @Step("Upload Scenario A asset image and capture its creative src")
    public String uploadAssetImageA() {
        uploadFileToInput(dropZoneA, ConfigReader.get("ASSET_IMAGE_PATH_A"));
        new WebDriverWait(driver, 30)
            .until(ExpectedConditions.visibilityOfElementLocated(deleteVersionA));
        return newlyAddedCreativeSrc();
    }

    @Step("Upload Scenario B asset image and capture its creative src")
    public String uploadAssetImageB() {
        uploadFileToInput(dropZoneB, ConfigReader.get("ASSET_IMAGE_PATH_B"));
        new WebDriverWait(driver, 30)
            .until(ExpectedConditions.visibilityOfElementLocated(deleteVersionB));
        return newlyAddedCreativeSrc();
    }

    @Step("Confirm Scenario A upload completed — delete-version-A visible")
    public boolean isAssetAUploaded() {
        return isElementVisible(deleteVersionA);
    }

    @Step("Confirm Scenario B upload completed — delete-version-B visible")
    public boolean isAssetBUploaded() {
        return isElementVisible(deleteVersionB);
    }

    @Step("Proceed to form questions stage via qat_assets_next_button")
    public QatProjectPage proceedToFormQuestions() {
        // delete-version markers confirm client-side upload completion, but Bubble.io still
        // needs a server round-trip to render the next button. Wait up to 30s for it to appear
        // in the DOM, scroll it into view, then jsClick directly.
        jsClick(By.cssSelector("[id='qat_assets_next_button ']"));
        return this;
    }

    @Step("Verify form questions stage loaded — add-question button is clickable")
    public boolean isFormQuestionsStageReady() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(addQuestionButton));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
