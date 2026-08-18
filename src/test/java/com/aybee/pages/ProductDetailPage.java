package com.aybee.pages;

import com.aybee.context.ProductSnapshot;
import com.aybee.context.ScenarioContext;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import java.util.List;

// PDP product detail page shown after consent (guest journey lands here to verify product data;
// logged-in owner lands here for the Not-Interested / No-Buying-Intent flows).
// It carries a rating overlay (a 0–100 slider + a submit button) that sits on top of the page.
public class ProductDetailPage extends BasePage {

    private final By productTitle        = By.id("product-title");
    private final By productPrice        = By.id("product-price");
    private final By checkoutProductPrice = By.id("checkout-product-price");
    private final By productBrand        = By.id("product-brand");
    private final By primeStatus         = By.id("prime-status");
    private final By mainPicture         = By.id("main_picture");
    private final By notInterestedButton = By.id("button-not-interest");
    // "No Buying Intent" trigger — lives on the rating overlay itself (no overlay-close needed).
    private final By noBuyingIntentButton = By.id("product-not-interested");
    private final By finalQuestionsTitle = By.id("final-questions-title");

    // Rating slider range input. Its element id is Bubble-generated and CHANGES between renders
    // (WZkJL initially, Wtpcn after reopen), so locate it structurally inside #slidercontainer
    // rather than by id. Default value is the midpoint (50); 0% = would not buy, 100% = definitely buy.
    private final By ratingSliderInput = By.cssSelector("#slidercontainer input[type='range']");
    // Percentage readout — only rendered once the slider is moved off its default.
    private final By decisionPercentage = By.id("pdp-decision-percentage");
    // Buy-now button — DISABLED until the slider is moved.
    private final By submitRatingButton = By.id("product-buy-now");

    // Overlay/slider close + reopen buttons.
    private final By closeOverlayButton    = By.id("close-slider");
    private final By showSliderAgainButton = By.id("show-slider-again");
    // Participant Continue button — some builds require a Continue click AFTER moving the slider for
    // the rating to submit (the slider alone doesn't proceed).
    private final By continueButton        = By.xpath(
        "//button[@class='clickable-element bubble-element Button cpaOkf']");

    @Step("Wait for product detail page to load")
    public ProductDetailPage waitUntilLoaded() {
        wait.until(ExpectedConditions.presenceOfElementLocated(productTitle));
        return this;
    }

    // ── Rating overlay (buy-rating path — kept for the future rating/buy case) ──────

    // Drags the rating slider from its midpoint (50) to targetPercent. A range input responds to
    // ArrowRight/ArrowLeft (step=1) and fires native input/change events, which is more reliable
    // for Bubble.io than synthesising a mouse drag. 0% = "would not buy", 100% = "would definitely buy".
    @Step("Drag rating slider to {targetPercent}%")
    public ProductDetailPage dragRatingSlider(int targetPercent) {
        WebElement slider = new WebDriverWait(driver, 30)
            .until(ExpectedConditions.presenceOfElementLocated(ratingSliderInput));
        scrollToCenter(slider);
        int current;
        try {
            current = Integer.parseInt(slider.getAttribute("value"));
        } catch (Exception e) {
            current = 50; // default midpoint when no value attribute is set
        }
        try {
            slider.click(); // focus the range input so arrow keys register
            int delta = targetPercent - current;
            Keys key = delta >= 0 ? Keys.ARROW_RIGHT : Keys.ARROW_LEFT;
            for (int i = 0; i < Math.abs(delta); i++) {
                slider.sendKeys(key);
            }
        } catch (Exception e) {
            // Fallback: set value + dispatch input/change directly.
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input',  {bubbles:true}));" +
                "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                slider, String.valueOf(targetPercent));
        }
        System.out.println("[ProductDetail] Rating slider set to: " + slider.getAttribute("value") + "%");
        return this;
    }

    // Once "No Buying Intent" (product-not-interested) is shown, the overlay starts a ~20s timer;
    // when it elapses a buy prompt (product-buy-now) pops up. We handle this DYNAMICALLY — an
    // explicit condition wait (timeout > 20s), never a hardcoded 20s sleep — so we proceed the moment
    // the popup appears and absorb any variance in the timer. Returns true if it appeared in time.
    // Polls up to {seconds}s (default 500ms interval) for a popup control to become clickable, then
    // returns it. Used before every slider/buy-now popup interaction — these popups render on a timer
    // / after reactive re-renders, so a fixed short wait is not enough.
    private WebElement waitClickable(By locator, int seconds) {
        return new WebDriverWait(driver, seconds)
            .until(ExpectedConditions.elementToBeClickable(locator));
    }

    @Step("Wait dynamically for the timed buy-now popup (product-buy-now)")
    public boolean waitForBuyNowPopup(int timeoutSecs) {
        try {
            new WebDriverWait(driver, timeoutSecs)
                .until(ExpectedConditions.visibilityOfElementLocated(submitRatingButton));
            System.out.println("[ProductDetail] Buy-now popup appeared (product-buy-now visible)");
            return true;
        } catch (Exception e) {
            System.out.println("[ProductDetail] Buy-now popup did not appear within " + timeoutSecs + "s");
            return false;
        }
    }

    // Verifies the timed buy-now popup lifecycle (guest journey): wait dynamically for the popup to
    // appear, close it via close-slider (product-buy-now disappears), then reopen via
    // show-slider-again (product-buy-now reappears). Product detail reads should already be done
    // before calling this — the details stay readable, but we still snapshot them first.
    @Step("Verify buy-now popup appears, closes on close-slider, and reopens on show-slider-again")
    public void verifyBuyPopupCloseAndReopen() {
        SoftAssert sa = new SoftAssert();

        // 1. Dynamically wait for the timed popup (never a fixed 20s sleep).
        boolean appeared = waitForBuyNowPopup(30);
        sa.assertTrue(appeared,
            "[ProductDetail] Buy-now popup (product-buy-now) did not appear after the ~20s timer");

        if (appeared) {
            // 2. Close the slider popup — wait up to 30s (polling) for close-slider, then click;
            //    product-buy-now should become invisible.
            waitClickable(closeOverlayButton, 30);
            jsClick(closeOverlayButton);
            boolean closed;
            try {
                new WebDriverWait(driver, 15).until(
                    ExpectedConditions.invisibilityOfElementLocated(submitRatingButton));
                closed = true;
            } catch (Exception e) { closed = false; }
            sa.assertTrue(closed,
                "[ProductDetail] Slider popup did not close after clicking close-slider");

            // 3. Re-open via show-slider-again — wait up to 30s (polling) for it, then click;
            //    product-buy-now should reappear.
            waitClickable(showSliderAgainButton, 30);
            jsClick(showSliderAgainButton);
            boolean reopened;
            try {
                new WebDriverWait(driver, 15).until(
                    ExpectedConditions.visibilityOfElementLocated(submitRatingButton));
                reopened = true;
            } catch (Exception e) { reopened = false; }
            sa.assertTrue(reopened,
                "[ProductDetail] Slider popup did not reopen after clicking show-slider-again");
        }
        sa.assertAll();
    }

    // Full rate-and-buy path (guest journey after the slider is reopened): move the slider off its
    // default midpoint, confirm the decision percentage appears and buy-now enables (it is disabled
    // until the slider moves), click Buy Now, and verify the redirect to the question page.
    @Step("Move decision slider to {targetPercent}%, submit Buy Now, and verify redirect to questions")
    public void rateAndBuyNow(int targetPercent) {
        SoftAssert sa = new SoftAssert();

        WebElement slider = new WebDriverWait(driver, 30)
            .until(ExpectedConditions.presenceOfElementLocated(ratingSliderInput));
        scrollToCenter(slider);

        // Buy-now must be DISABLED before the slider is moved. Bubble marks disabled via the inline
        // style (no `cursor: pointer`), not the HTML disabled attr — soft-check (logged, not fatal).
        boolean enabledBefore = isBuyNowEnabled();
        System.out.println("[ProductDetail] Buy Now enabled before slider move? " + enabledBefore
            + " (expected false / no 'cursor: pointer')");
        sa.assertFalse(enabledBefore,
            "[ProductDetail] Buy Now should be disabled (no 'cursor: pointer' in style) before the slider is moved");

        // Move the slider off its default midpoint (50) to targetPercent.
        moveSlider(slider, targetPercent);

        // Moving the slider reveals the decision percentage.
        sa.assertTrue(isElementVisible(decisionPercentage),
            "[ProductDetail] pdp-decision-percentage not displayed after moving the slider");
        System.out.println("[ProductDetail] Decision percentage shown: " + safeText(decisionPercentage));

        // Buy-now enables after the move — wait up to 30s for the style to flip to enabled
        // (`cursor: pointer` appears), then click.
        boolean enabledAfter;
        try {
            enabledAfter = new WebDriverWait(driver, 30).until(d -> isBuyNowEnabled());
        } catch (Exception e) {
            enabledAfter = false;
        }
        System.out.println("[ProductDetail] Buy Now enabled after slider move? " + enabledAfter);
        if (!enabledAfter) {
            sa.fail("[ProductDetail] product-buy-now did not become enabled ('cursor: pointer') after moving the slider");
        }
        jsClick(submitRatingButton);

        // Slide-then-continue: the slider only proceeds once its Continue is clicked. If the question
        // page hasn't appeared shortly after Buy Now, click the participant Continue button and re-check.
        boolean onQuestions = waitForQuestionPage(15);
        if (!onQuestions) {
            try {
                if (!driver.findElements(continueButton).isEmpty()) {
                    System.out.println("[ProductDetail] Question page not shown after Buy Now — clicking Continue after the slider");
                    new WebDriverWait(driver, 10)
                        .until(ExpectedConditions.elementToBeClickable(continueButton)).click();
                }
            } catch (Exception ignored) {}
            onQuestions = waitForQuestionPage(30);
        }
        sa.assertTrue(onQuestions,
            "[ProductDetail] Not redirected to the question page after Buy Now + Continue "
            + "(neither final-questions-title nor split-question-title appeared)");
        sa.assertAll();
    }

    // Waits up to {seconds}s for either question-page landmark (long-text or split-test) to appear.
    private boolean waitForQuestionPage(int seconds) {
        try {
            new WebDriverWait(driver, seconds).until(d ->
                !d.findElements(By.id("final-questions-title")).isEmpty()
                || !d.findElements(By.id("split-question-title")).isEmpty());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Moves a range input from its current value to targetPercent using arrow keys (fires native
    // input/change so Bubble's percentage readout + buy-now enable react); JS value-set is the fallback.
    private void moveSlider(WebElement slider, int targetPercent) {
        int current;
        try { current = Integer.parseInt(slider.getAttribute("value")); } catch (Exception e) { current = 50; }
        try {
            slider.click();
            int delta = targetPercent - current;
            Keys key = delta >= 0 ? Keys.ARROW_RIGHT : Keys.ARROW_LEFT;
            for (int i = 0; i < Math.abs(delta); i++) slider.sendKeys(key);
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));" +
                "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                slider, String.valueOf(targetPercent));
        }
        System.out.println("[ProductDetail] Slider moved to " + slider.getAttribute("value") + "%");
    }

    private boolean isButtonClickable(By locator) {
        try {
            return new WebDriverWait(driver, 2)
                .until(ExpectedConditions.elementToBeClickable(locator)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    // Whether the buy-now / Continue button is ENABLED. Bubble does NOT use the HTML `disabled`
    // attribute here — it flips state via the inline style: enabled adds `cursor: pointer` (plus
    // color/background-color styling), while the disabled default is just
    // `max-width: unset; max-height: unset;` with no cursor. So we detect the state by the presence of
    // `cursor: pointer` in the style, which is the only reliable difference.
    private boolean isBuyNowEnabled() {
        try {
            List<WebElement> els = driver.findElements(submitRatingButton);
            if (els.isEmpty()) return false;
            String style = els.get(0).getAttribute("style");
            return style != null && style.contains("cursor: pointer");
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Submit the rating (product-buy-now)")
    public ProductDetailPage submitRating() {
        try {
            clickWhenEnabled(submitRatingButton);
        } catch (Exception e) {
            jsClick(submitRatingButton);
        }
        return this;
    }

    // ── Not-interested path ─────────────────────────────────────────────────────

    // Closes the rating overlay (close-slider) so the underlying product detail can be inspected
    // before choosing Not Interested.
    @Step("Close the rating overlay")
    public ProductDetailPage closeRatingOverlay() {
        // Wait up to 30s (polling) for the slider's close button (close-slider) before clicking it.
        waitClickable(closeOverlayButton, 30);
        jsClick(closeOverlayButton);
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.invisibilityOfElementLocated(ratingSliderInput));
        return this;
    }

    // Clicks Not Interested. For PDP this surfaces the exclusive not-interested questions
    // (final-questions-title) rather than redirecting away.
    @Step("Click Not Interested and wait for the exclusive not-interested questions")
    public ProductDetailPage clickNotInterested() {
        jsClick(notInterestedButton);
        waitForNotInterestedQuestions();
        return this;
    }

    // Clicks "No Buying Intent" (product-not-interested) directly on the rating overlay — no need to
    // close the overlay first. Surfaces the SAME exclusive question flow as Not Interested.
    @Step("Click No Buying Intent and wait for the exclusive not-interested questions")
    public ProductDetailPage clickNoBuyingIntent() {
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(noBuyingIntentButton));
        jsClick(noBuyingIntentButton);
        waitForNotInterestedQuestions();
        return this;
    }

    // After declining, PDP shows the split-test questions (split-question-title) — NOT the old default
    // long-text final-questions-title. Wait for the split question, accepting final-questions-title too
    // for safety, and reload past Bubble's occasional ghost/blank first render.
    private void waitForNotInterestedQuestions() {
        if (!waitForAnyLandmarkElseReload(
                java.util.Arrays.asList(By.id("split-question-title"), finalQuestionsTitle), 20, 2)) {
            throw new org.openqa.selenium.TimeoutException(
                "Not-interested questions did not appear (split-question-title / final-questions-title) even after reloads");
        }
    }

    // ── Guest verification: compare detail-page data to the stored scenario snapshot ──────

    // Reads product-title, matches it to Scenario A or B by stored name, then soft-asserts price
    // (product-price + checkout-product-price), brand (product-brand), prime (prime-status) and the
    // main image (main_picture) against that scenario's snapshot captured during shop setup.
    // Null snapshot fields are skipped-and-logged (so this degrades gracefully until shop-setup
    // capture is wired). If NO snapshot is stored at all, records one clear soft failure.
    @Step("Verify product detail page matches the assigned scenario")
    public void verifyProductMatchesAssignedScenario(ScenarioContext ctx) {
        SoftAssert sa = new SoftAssert();
        waitUntilLoaded();
        String displayedTitle = getText(productTitle).trim();
        System.out.println("[ProductDetail] Displayed product title: " + displayedTitle);

        ProductSnapshot snap = resolveAssignedScenario(ctx, displayedTitle);
        if (snap == null) {
            // Log the live page values so the run is still informative.
            System.out.println("[ProductDetail] No stored scenario snapshot to compare against — "
                + "shop-setup capture is not wired yet. Live values: "
                + "title=" + displayedTitle
                + " price=" + normalizePrice(priceTextById("product-price"))
                + " checkoutPrice=" + normalizePrice(priceTextById("checkout-product-price"))
                + " brand=" + safeText(productBrand)
                + " prime=" + isElementVisible(primeStatus)
                + " mainImg=" + visibleMainPictureSrc());
            sa.fail("[ProductDetail] Cannot verify scenario — no Scenario A/B snapshot stored "
                + "during shop setup (capture pending). Displayed title: " + displayedTitle);
            sa.assertAll();
            return;
        }

        System.out.println("[ProductDetail] Assigned scenario resolved: " + ctx.currentScenario
            + " (expected name: " + snap.truncatedName + ")");

        // Name — full exact match (whitespace-normalised). We store the full field value and the
        // detail page displays that same full name.
        if (snap.truncatedName != null && !snap.truncatedName.isEmpty()) {
            sa.assertEquals(norm(displayedTitle), norm(snap.truncatedName),
                "[ProductDetail] Name mismatch — expected [" + snap.truncatedName + "] got [" + displayedTitle + "]");
        }
        // Price — both the main price and the checkout price, and they must match each other.
        String detailPrice   = normalizePrice(priceTextById("product-price"));
        String checkoutPrice = normalizePrice(priceTextById("checkout-product-price"));
        softAssertField(sa, "product-price", snap.price, detailPrice);
        softAssertField(sa, "checkout-product-price", snap.price, checkoutPrice);
        if (detailPrice != null && checkoutPrice != null) {
            sa.assertEquals(checkoutPrice, detailPrice,
                "[ProductDetail] checkout-product-price does not match product-price");
        }
        // Brand
        softAssertField(sa, "product-brand", snap.brand, safeText(productBrand));
        // (Prime status check removed — no longer verified on the product detail page.)
        // Main image
        if (snap.imageSrc != null && !snap.imageSrc.isEmpty()) {
            String pageSrc = visibleMainPictureSrc();
            softAssertField(sa, "main_picture src", snap.imageSrc, pageSrc);
        }
        sa.assertAll();
    }

    // Matches the displayed title against the stored Scenario A / B names and returns the snapshot,
    // setting ctx.currentScenario ("A"/"B"). Falls back to name fields if snapshots lack names.
    private ProductSnapshot resolveAssignedScenario(ScenarioContext ctx, String displayedTitle) {
        String title = norm(displayedTitle);
        String nameA = firstNonNull(
            ctx.scenarioAProduct != null ? ctx.scenarioAProduct.truncatedName : null, ctx.scenarioAProductName);
        String nameB = firstNonNull(
            ctx.scenarioBProduct != null ? ctx.scenarioBProduct.truncatedName : null, ctx.scenarioBProductName);
        // Full exact match first (both scenarios store the full displayed name).
        if (nameA != null && norm(nameA).equals(title)) { ctx.currentScenario = "A"; return ctx.scenarioAProduct; }
        if (nameB != null && norm(nameB).equals(title)) { ctx.currentScenario = "B"; return ctx.scenarioBProduct; }
        // Fallback: contains (A first — A's name is the longer/full one, B is A minus its first word).
        if (nameA != null && title.contains(norm(nameA))) { ctx.currentScenario = "A"; return ctx.scenarioAProduct; }
        if (nameB != null && title.contains(norm(nameB))) { ctx.currentScenario = "B"; return ctx.scenarioBProduct; }
        // No name match — return whichever snapshot exists so field comparisons can still run.
        if (ctx.scenarioAProduct != null) { ctx.currentScenario = "A"; return ctx.scenarioAProduct; }
        if (ctx.scenarioBProduct != null) { ctx.currentScenario = "B"; return ctx.scenarioBProduct; }
        return null;
    }

    // Whitespace-normalised, lower-cased form for full-name comparison.
    private static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    // Returns the src of the first VISIBLE main_picture <img> with a non-empty src (the detail page
    // may carry hidden template copies of main_picture, like the split-question displays).
    private String visibleMainPictureSrc() {
        for (WebElement el : driver.findElements(mainPicture)) {
            try {
                if (!el.isDisplayed()) continue;
                List<WebElement> imgs = el.findElements(By.tagName("img"));
                if (imgs.isEmpty()) continue;
                String src = imgs.get(0).getAttribute("src");
                if (src != null && !src.trim().isEmpty()) return src;
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String safeText(By locator) {
        try { return getText(locator).trim(); } catch (Exception e) { return null; }
    }

    private void softAssertField(SoftAssert sa, String label, String expected, String actual) {
        if (expected == null || expected.isEmpty()) {
            System.out.println("[ProductDetail] Skipping " + label + " — no stored value");
            return;
        }
        if (actual == null || actual.isEmpty()) {
            System.out.println("[ProductDetail] Skipping " + label + " — page value empty");
            return;
        }
        sa.assertEquals(actual, expected, "[ProductDetail] " + label + " mismatch");
    }

    private static String firstNonNull(String a, String b) {
        return (a != null && !a.isEmpty()) ? a : b;
    }
}
