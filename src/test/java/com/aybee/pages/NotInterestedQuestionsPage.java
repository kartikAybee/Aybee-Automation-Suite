package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import java.util.List;
import java.util.concurrent.TimeUnit;

// Questions shown EXCLUSIVELY after clicking "Not Interested" on the PDP product detail page.
//
// Flow: two default long-text questions (min 35 words) then the FOUR split-test questions that were
// created in the form-questions setup (Q11..Q14), each displaying different content per its
// "What to Display" setting. Answering all four auto-advances back to the Form Questions editor page.
//
// DOM-vs-frontend instance handling: the platform renders hidden template copies of the display
// containers, so each display id/xpath appears MORE times in the DOM than on screen (main_picture 3×,
// image-gallery hero 4×, img_multi_desc 3×). We count only the instances that are BOTH visible
// (isDisplayed) AND contain an <img> with a non-empty src, which filters the hidden templates out and
// leaves the 2 real (one per scenario) frontend instances.
public class NotInterestedQuestionsPage extends BasePage {

    private static final By QUESTION_TITLE       = By.id("final-questions-title");
    // Split-test questions use a DIFFERENT title id than the long-text questions.
    private static final By SPLIT_QUESTION_TITLE = By.id("split-question-title");
    private static final By TEXT_INPUT           = By.id("textInputDeskdesktop");
    private static final By CONTINUE_BTN         =
        By.xpath("//button[@class='clickable-element bubble-element Button cpaOkf']");

    // Split-test choices: split-test-{Scenario} (A, B, ...) plus a No Difference button.
    // Clicking ANY of these auto-advances to the next question — there is NO continue button.
    private static final By SPLIT_OPTION_A        = By.id("split-test-A");
    private static final By SPLIT_OPTION_B        = By.id("split-test-B");
    private static final By NEUTRAL_SELECTION_BTN = By.id("neutral-selection-btn");
    private static final By ANY_SPLIT_OPTION      = By.cssSelector("[id^='split-test-']");

    // The scenarios we set up (PDP keeps Scenario A and B — C is deleted during shop setup). Each
    // split-test question should show EXACTLY these scenario options (split-test-A, split-test-B) plus
    // the No Difference button — no extras like a stray split-test-C.
    private static final java.util.List<String> EXPECTED_SCENARIOS = java.util.Arrays.asList("A", "B");

    // Q1 (long text) displays the whole (single) product alongside the question — interim XPath.
    private static final By SINGLE_PRODUCT_DISPLAY =
        By.xpath("//div[@class=\"bubble-element Group cpaRaEaO bubble-r-container flex column\"]");

    // Per "What to Display" content containers (each rendered twice on screen — one per scenario):
    private static final By FULL_PRODUCT_PAGE_DISPLAY =
        By.xpath("//div[@class=\"bubble-element Group cpkaPt bubble-r-container flex column\"]"); // full_product_page
    private static final By MAIN_PICTURE      = By.id("main_picture");                 // primary_image
    private static final By IMAGE_GALLERY_HERO =
        By.xpath("//div[@class=\"bubble-element Group cnnpaI2 bubble-r-container flex column\"]"); // image_gallery
    private static final By A_PLUS_CONTENT     = By.id("img_multi_desc");              // a__content__section_2_

    // After the split-test questions are answered, the logged-in (owner) journey redirects to the Shop
    // Setup page — verified by its Preview Shop button.
    private static final By SHOP_SETUP_REDIRECT = By.id("marketplacesimulation_shopsetup_previewshop_button");

    private static final String Q1_FRAGMENT = "holding you back";
    private static final String Q2_FRAGMENT = "After comparing both versions";

    // 54 words — comfortably above the 35-word minimum.
    private static final String LONG_TEXT_ANSWER =
        "This product delivers exceptional performance and outstanding build quality. " +
        "The design is both functional and aesthetically pleasing, making it a strong contender " +
        "in its category. The pricing is competitive and the overall value for money is excellent " +
        "given the range of features offered. I would highly recommend this product to anyone " +
        "looking for quality and long-term reliability.";

    // The four split-test questions in creation/display order, each with its expected display container.
    private enum SplitDisplay {
        FULL_PRODUCT_PAGE("full_product_page", FULL_PRODUCT_PAGE_DISPLAY),
        // primary_image is verified via the main_picture container (confirmed by kartik).
        PRIMARY_IMAGE("primary_image", MAIN_PICTURE),
        IMAGE_GALLERY("image_gallery", IMAGE_GALLERY_HERO),
        A_PLUS_CONTENT("a__content__section_2_", NotInterestedQuestionsPage.A_PLUS_CONTENT);

        final String label;
        final By display;
        SplitDisplay(String label, By display) { this.label = label; this.display = display; }

        static SplitDisplay fromLabel(String label) {
            for (SplitDisplay s : values()) if (s.label.equals(label)) return s;
            return null;
        }
    }

    private static final int EXPECTED_FRONTEND_INSTANCES = 2; // one per scenario

    // Answers the split-test questions (the only manually-added questions) in the SAME order they were
    // created — driven by GlobalTestState.splitDisplayOptions, the What-to-Display options that were
    // actually available at setup (one question per option). The expected number of split-test
    // questions is therefore DYNAMIC: exactly however many we set up, not a hard-coded four. Falls back
    // to all known displays only if the list wasn't recorded (e.g. running a preview case standalone).
    private void answerSplitTests(SoftAssert sa) {
        java.util.List<String> opts = com.aybee.context.GlobalTestState.splitDisplayOptions;
        if (opts == null || opts.isEmpty()) {
            opts = new java.util.ArrayList<>();
            for (SplitDisplay split : SplitDisplay.values()) opts.add(split.label);
            System.out.println("[NotInterested] splitDisplayOptions not recorded — falling back to all "
                + opts.size() + " known displays");
        }
        int expected = opts.size();
        System.out.println("[NotInterested] Expecting " + expected + " split-test question(s) to answer "
            + "(dynamic — one per What-to-Display option set up): " + opts);
        int answered = 0;
        for (String opt : opts) {
            SplitDisplay split = SplitDisplay.fromLabel(opt);
            if (split != null) {
                answerSplitQuestion(split, sa);
            } else {
                // Unknown What-to-Display value — no content locator to verify, so just answer it to
                // advance the journey (wait for split-question-title, pick a choice).
                System.out.println("[NotInterested] No display mapping for '" + opt + "' — answering generically");
                answerSplitQuestionGeneric(sa);
            }
            answered++;
        }
        System.out.println("[NotInterested] Answered " + answered + " of " + expected + " split-test question(s)");
    }

    // Generic split-test answer for an unmapped What-to-Display: wait for the split-question title,
    // then click Scenario A's choice (advances). No per-display content verification.
    private void answerSplitQuestionGeneric(SoftAssert sa) {
        if (!checkSplitQuestionTitle("(generic)", sa)) return;
        String prevTitle = readSplitTitle();
        // The option buttons render slightly after the title — wait for them before verifying/clicking.
        waitForSplitOptionsReady(15);
        verifySplitOptions("(generic)", sa);
        clickSplitChoiceAndAwaitAdvance("(generic)", prevTitle, sa);
    }

    // Current split-question-title text (empty string if absent/unreadable).
    private String readSplitTitle() {
        try {
            List<WebElement> els = driver.findElements(SPLIT_QUESTION_TITLE);
            if (els.isEmpty()) return "";
            String t = els.get(0).getText();
            return t == null ? "" : t.trim();
        } catch (Exception e) { return ""; }
    }

    // First split option that is actually on screen (visible, non-zero height) — skips Bubble's hidden
    // template copies. Returns null if none are rendered yet.
    private WebElement visibleSplitOption(By locator) {
        for (WebElement el : driver.findElements(locator)) {
            try {
                if (el.isDisplayed() && el.getRect().getHeight() > 0) return el;
            } catch (Exception ignored) {}
        }
        return null;
    }

    // Waits until at least one split option button is actually rendered on screen.
    private void waitForSplitOptionsReady(int secs) {
        try {
            new WebDriverWait(driver, secs).until(d -> visibleSplitOption(ANY_SPLIT_OPTION) != null);
        } catch (Exception ignored) {}
    }

    // Clicks Scenario A's choice and confirms the journey moved on. NOTE: every split question renders
    // split-test-A/-B, so waiting for split-test-A to disappear is wrong — it reappears on the next
    // question. Instead we detect advance by the split-question TITLE changing, the split title vanishing,
    // or a terminal screen appearing (Form Questions editor for owners, toggle-sign-in for guests). The
    // click is NATIVE (not jsClick) because Bubble's "when clicked" workflow ignores synthetic clicks.
    private void clickSplitChoiceAndAwaitAdvance(String label, String prevTitle, SoftAssert sa) {
        try {
            new WebDriverWait(driver, 15).until(d -> visibleSplitOption(SPLIT_OPTION_A) != null);
            WebElement choice = visibleSplitOption(SPLIT_OPTION_A);
            scrollToCenter(choice);
            choice.click(); // native click — required for Bubble to fire the option's workflow
            boolean advanced = new WebDriverWait(driver, 20).until(d -> {
                if (isDisplayedNow(d, SHOP_SETUP_REDIRECT)) return true;            // owner terminal (shop setup)
                if (isDisplayedNow(d, By.id("toggle-sign-in"))) return true;        // guest terminal
                List<WebElement> t = d.findElements(SPLIT_QUESTION_TITLE);
                if (t.isEmpty()) return true;                                       // split title gone
                String now = t.get(0).getText();
                now = now == null ? "" : now.trim();
                return !now.isEmpty() && !now.equals(prevTitle);                    // moved to next question
            });
            System.out.println("[NotInterested] '" + label + "' answered (split-test-A) — advanced (was: '" + prevTitle + "')");
        } catch (Exception e) {
            sa.fail("[NotInterested] Could not answer split '" + label + "' / no advance detected: " + e.getMessage());
        }
    }

    private boolean isDisplayedNow(org.openqa.selenium.WebDriver d, By locator) {
        try {
            List<WebElement> els = d.findElements(locator);
            return !els.isEmpty() && els.get(0).isDisplayed();
        } catch (Exception e) { return false; }
    }

    // Confirms we're on a split-test question at this step: split-question-title must be present,
    // visible, and non-empty. Logs the title text. Returns false (and soft-fails) if it never shows.
    private boolean checkSplitQuestionTitle(String label, SoftAssert sa) {
        // Bubble sometimes doesn't render the split-test question on load — reload and retry (up to 2
        // reloads) until the split-question-title appears before asserting on it.
        if (!waitForLandmarkElseReload(SPLIT_QUESTION_TITLE, 15, 2)) {
            sa.fail("[NotInterested] split-question-title did not appear for '" + label + "' (even after reloads)");
            return false;
        }
        try {
            WebElement title = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(SPLIT_QUESTION_TITLE));
            String text = title.getText() == null ? "" : title.getText().trim();
            System.out.println("[NotInterested] Split question (" + label + ") — split-question-title: '" + text + "'");
            if (text.isEmpty()) {
                sa.fail("[NotInterested] split-question-title present but empty for '" + label + "'");
            }
            return true;
        } catch (Exception e) {
            sa.fail("[NotInterested] split-question-title did not appear for '" + label + "' within 30s");
            return false;
        }
    }

    // Owner decline flow (cases 4 & 5): after Not-Interested / No-Buying-Intent the participant goes
    // straight to our split-test questions (as many as were set up; the old 2 exclusive long-text
    // default questions were stripped), then is redirected back to the Form Questions editor.
    @Step("Answer the split-test questions and verify redirect to the Shop Setup page (owner)")
    public void answerNotInterestedQuestions() {
        SoftAssert sa = new SoftAssert();
        answerSplitTests(sa);
        try {
            new WebDriverWait(driver, 45)
                .until(ExpectedConditions.visibilityOfElementLocated(SHOP_SETUP_REDIRECT));
            System.out.println("[NotInterested] Redirected to Shop Setup page (marketplacesimulation_shopsetup_previewshop_button visible)");
        } catch (Exception e) {
            sa.fail("[NotInterested] Did not redirect to Shop Setup page after the split questions — "
                + "marketplacesimulation_shopsetup_previewshop_button not visible within 45s");
        }
        sa.assertAll();
    }

    // Guest buy flow (case 6): after Buy Now the guest answers our split-test questions (as many as
    // were set up), then the survey completes and the guest is sent to the sign-up/sign-in page
    // (toggle-sign-in). Completion is soft-checked (or the split-question title simply disappearing) so
    // a differing terminal screen doesn't hard-fail the guest run.
    @Step("Answer the split-test questions as a guest and verify the journey completes")
    public void answerSplitTestQuestionsAsGuest() {
        SoftAssert sa = new SoftAssert();
        answerSplitTests(sa);
        try {
            new WebDriverWait(driver, 45).until(ExpectedConditions.or(
                ExpectedConditions.visibilityOfElementLocated(By.id("toggle-sign-in")),
                ExpectedConditions.invisibilityOfElementLocated(SPLIT_QUESTION_TITLE)));
            System.out.println("[Guest] Split-test questions answered; journey reached its terminal screen");
        } catch (Exception e) {
            sa.fail("[Guest] Did not reach a terminal screen after answering the 4 split-test questions");
        }
        sa.assertAll();
    }

    // Waits for the split question, verifies its per-type display content is shown twice (once per
    // scenario), then clicks a choice which auto-advances.
    @Step("Answer split-test question showing '{split}' (verify 2 displays, choice auto-advances)")
    private void answerSplitQuestion(SplitDisplay split, SoftAssert sa) {
        // Confirm we're on a split-test question (split-question-title present/visible/non-empty).
        if (!checkSplitQuestionTitle(split.label, sa)) return;
        String prevTitle = readSplitTitle();

        int shown = waitForVisibleWithImageCount(split.display, EXPECTED_FRONTEND_INSTANCES, 30);
        if (shown != EXPECTED_FRONTEND_INSTANCES) {
            logInstances(split.display);
            sa.fail("[NotInterested] '" + split.label + "' expected " + EXPECTED_FRONTEND_INSTANCES
                + " visible product displays (one per scenario), got: " + shown);
        }

        // Verify the scenario options shown are EXACTLY the ones we expect (split-test-A/-B) with the
        // No Difference button, and no extras — before choosing.
        waitForSplitOptionsReady(15);
        verifySplitOptions(split.label, sa);

        // Pick a choice — scenario A. (split-test-B / neutral-selection-btn are equally valid.)
        clickSplitChoiceAndAwaitAdvance(split.label, prevTitle, sa);
    }

    // Confirms the split-test question shows EXACTLY the expected scenario options (split-test-{letter}
    // for each expected scenario) plus the No Difference button, with NO extras (e.g. a stray
    // split-test-C). Filters to VISIBLE elements (Bubble leaves hidden id copies in the DOM). Soft-fails
    // on any mismatch so the whole set of split questions is still exercised.
    private void verifySplitOptions(String label, SoftAssert sa) {
        java.util.Set<String> shown = new java.util.TreeSet<>();
        for (WebElement el : driver.findElements(ANY_SPLIT_OPTION)) {
            try {
                // Scroll the option into view before checking — Bubble can render options below the
                // fold, and isDisplayed()/getRect() are only reliable once the element is on screen.
                scrollToCenter(el);
                if (!el.isDisplayed() || el.getRect().getHeight() == 0) continue;
            } catch (Exception e) { continue; }
            String id = el.getAttribute("id");
            if (id == null || !id.startsWith("split-test-")) continue;
            String letter = id.substring("split-test-".length());
            if (!letter.isEmpty()) shown.add(letter);
        }
        java.util.Set<String> expected = new java.util.TreeSet<>(EXPECTED_SCENARIOS);
        if (shown.equals(expected)) {
            System.out.println("[NotInterested] '" + label + "' scenario options OK: split-test-" + shown);
        } else {
            sa.fail("[NotInterested] '" + label + "' scenario options mismatch — expected split-test-"
                + expected + " only, but shown split-test-" + shown);
        }
        boolean noDiff = false;
        try {
            java.util.List<WebElement> nd = driver.findElements(NEUTRAL_SELECTION_BTN);
            if (!nd.isEmpty()) {
                scrollToCenter(nd.get(0)); // scroll into view before the visibility check
                noDiff = nd.get(0).isDisplayed();
            }
        } catch (Exception ignored) {}
        if (!noDiff) {
            sa.fail("[NotInterested] '" + label + "' No Difference button (neutral-selection-btn) not shown");
        }
    }

    // ── DOM-vs-frontend instance filtering ──────────────────────────────────────

    // Counts elements matching locator that are BOTH visible AND contain an <img> with a non-empty
    // src. This filters out the hidden template copies Bubble.io leaves in the DOM.
    private int countVisibleWithImage(By locator) {
        int count = 0;
        for (WebElement el : driver.findElements(locator)) {
            try {
                if (!el.isDisplayed()) continue;
                // Guard against zero-size (collapsed) containers that report displayed.
                if (el.getRect().getHeight() <= 0 || el.getRect().getWidth() <= 0) continue;
                boolean hasImg = el.findElements(By.tagName("img")).stream().anyMatch(img -> {
                    String src = img.getAttribute("src");
                    return src != null && !src.trim().isEmpty();
                });
                if (hasImg) count++;
            } catch (Exception ignored) {}
        }
        return count;
    }

    // Waits until countVisibleWithImage reaches expected (or timeout), then returns the actual count.
    private int waitForVisibleWithImageCount(By locator, int expected, int timeoutSecs) {
        try {
            new WebDriverWait(driver, timeoutSecs)
                .until(d -> countVisibleWithImage(locator) >= expected);
        } catch (Exception ignored) {}
        return countVisibleWithImage(locator);
    }

    // Diagnostic dump of every DOM instance (displayed state + img src) when the count is off.
    private void logInstances(By locator) {
        List<WebElement> all = driver.findElements(locator);
        System.out.println("[NotInterested] Instance dump for " + locator + " (" + all.size() + " in DOM):");
        for (WebElement el : all) {
            try {
                List<WebElement> imgs = el.findElements(By.tagName("img"));
                String src = imgs.isEmpty() ? "(no img)" : imgs.get(0).getAttribute("src");
                System.out.println("  displayed=" + el.isDisplayed()
                    + " size=" + el.getRect().getWidth() + "x" + el.getRect().getHeight()
                    + " img=" + src);
            } catch (Exception e) {
                System.out.println("  <stale/error reading instance>");
            }
        }
    }

    // ── Long-text helpers (ported from the d2c participant-form precautions) ────

    private String waitForLongTextTitle(SoftAssert sa, String prev) {
        try {
            String result = new WebDriverWait(driver, 60).until(d -> {
                List<WebElement> els = d.findElements(QUESTION_TITLE);
                if (els.isEmpty()) return null;
                String text = els.get(0).getText().trim();
                return (!text.isEmpty() && !text.equals(prev)) ? text : null;
            });
            return (result == null || result.isEmpty()) ? null : result;
        } catch (Exception e) {
            sa.fail("[NotInterested] final-questions-title did not appear within 60s");
            return null;
        }
    }

    private void answerLongText(SoftAssert sa) {
        try {
            WebElement input = new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(TEXT_INPUT));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", input);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].value = arguments[1];" +
                "arguments[0].dispatchEvent(new Event('input',  {bubbles:true}));" +
                "arguments[0].dispatchEvent(new Event('change', {bubbles:true}));",
                input, LONG_TEXT_ANSWER);
            String actual = input.getAttribute("value");
            if (actual == null || !actual.contains(LONG_TEXT_ANSWER.substring(0, 20))) {
                sa.fail("[NotInterested] Long text not reflected. Got: [" + actual + "]");
            }
            blurActiveElement();
        } catch (Exception e) {
            sa.fail("[NotInterested] Long text input not found: " + e.getMessage());
        }
    }

    private void clickContinue(SoftAssert sa) {
        try {
            new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> {
                    List<WebElement> btns = d.findElements(CONTINUE_BTN);
                    if (btns.isEmpty()) return false;
                    String style = btns.get(0).getAttribute("style");
                    return style != null && style.contains("cursor: pointer");
                });
            jsClick(CONTINUE_BTN);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            sa.fail("[NotInterested] Continue button not enabled (cursor: pointer never appeared): " + e.getMessage());
        }
    }

    private boolean isDisplayed(By locator) {
        try {
            return new WebDriverWait(driver, 20)
                .until(ExpectedConditions.visibilityOfElementLocated(locator)) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
