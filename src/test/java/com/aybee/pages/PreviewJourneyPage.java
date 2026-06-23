package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// QAT preview journey.
//
// Two passes over the captured preview URL:
//  1. Logged-in pass (no session clear) — the experiment owner previews as a logged-in user.
//     QAT shows only gender + age demographics, then a consent form. We accept all three, then
//     click "Not Interested", which filters the participant out and redirects back to the
//     asset-upload step (verified by qat_assets_next_button).
//  2. Guest pass — clear the session and re-open the preview URL to run the usual journeys.
//
// The demographic option / consent / continue element ids are the same components as the
// msjourney preview (answer-Option-{value}, continue-button, agree-statement-button,
// continue-statement). The QAT-only ids are Not-Interested-Option and qat_assets_next_button.
public class PreviewJourneyPage extends BasePage {

    private final By continueButton          = By.id("continue-button");
    private final By continueStatementButton = By.id("continue-statement");
    private final By agreeStatementButton    = By.id("agree-statement-button");
    private final By notInterestedOption     = By.id("Not-Interested-Option");
    private final By assetsNextButton        = By.cssSelector("[id='qat_assets_next_button ']");
    private final By anyDemographicOption    = By.cssSelector("[id^='answer-Option-']");

    // Survey questions (revealed after picking a version) — same components as the msjourney
    // participant form: title element, choice option ids (single-choice-/multiple-choice-{text}),
    // and the Continue button. LOGIN_MARKER signals the survey finished (guest is redirected).
    private static final By QUESTION_TITLE = By.id("final-questions-title");
    private static final By CONTINUE_BTN   = By.xpath(
        "//button[@class='clickable-element bubble-element Button cpaOkf']");
    private static final By LOGIN_MARKER   = By.id("toggle-sign-in");

    // The version-selection reason must be > 50 words or the platform rejects it. 66 words.
    private static final String SELECTION_REASON =
        "I selected this version because the packaging communicates the product benefits clearly " +
        "and the colour contrast makes the brand name stand out on a crowded shelf. The layout " +
        "feels balanced, the imagery looks premium, and the overall composition draws my attention " +
        "immediately. Compared to the other option it simply feels more trustworthy, more modern, " +
        "and far more likely to make me stop and pick it up.";

    // ── Demographic question (option + continue + advance) ──────────────────────
    // Three-phase, each independently wrapped: a failure soft-asserts and returns without
    // throwing so the caller can keep going.
    private void answerDemographicQuestion(String optionValue, SoftAssert sa) {
        By optionLocator = By.id("answer-Option-" + optionValue);
        try {
            WebElement option = new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(optionLocator));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", option);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
        } catch (Exception e) {
            sa.fail("[QAT DemographicQ] Option not found or not clickable: " + optionValue);
            return;
        }
        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(continueButton));
            jsClick(continueButton);
        } catch (Exception e) {
            sa.fail("[QAT DemographicQ] Continue not clickable after selecting: " + optionValue);
            return;
        }
        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.invisibilityOfElementLocated(optionLocator));
        } catch (Exception e) {
            sa.fail("[QAT DemographicQ] Page did not advance after answering: " + optionValue);
        }
    }

    // Accept the consent form. Used by the logged-in pass (after gender + age) and by the
    // guest pass (after all demographics); agreeing leads to the QAT selection page.
    public void agreeToConsent(SoftAssert sa) {
        try {
            new FluentWait<>(driver)
                .withTimeout(20, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.presenceOfElementLocated(agreeStatementButton));
            jsClick(agreeStatementButton);
            jsClick(continueStatementButton);
        } catch (Exception e) {
            sa.fail("[QAT Consent] Agree / continue not available: " + e.getMessage());
        }
    }

    // ── Logged-in preview pass ──────────────────────────────────────────────────

    // Navigates the CURRENT (logged-in) session to the preview URL — no session clear — so
    // the owner previews as a logged-in user. In this mode only gender + age are asked before
    // the consent form. Accepts all three, then clicks Not Interested and verifies the redirect
    // back to the asset-upload step. Uses the shared SoftAssert so failures aggregate across
    // the scenario (AllureHooks calls assertAll at teardown).
    @Step("Preview as logged-in user, accept gender/age/consent, then click Not Interested and verify redirect")
    public void previewLoggedInAndClickNotInterested(String previewUrl, SoftAssert sa) {
        driver.get(previewUrl);
        // First demographic option confirms the preview rendered (continue-button only
        // appears AFTER an option is picked, so it cannot be the page-ready signal).
        try {
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.presenceOfElementLocated(anyDemographicOption));
        } catch (Exception e) {
            String pageUrl = "";
            String pageTitle = "";
            String visibleIds = "";
            try { pageUrl   = driver.getCurrentUrl(); } catch (Exception ignored) {}
            try { pageTitle = driver.getTitle(); } catch (Exception ignored) {}
            try {
                visibleIds = driver.findElements(By.cssSelector("[id]")).stream()
                    .filter(el -> { try { return el.isDisplayed(); } catch (Exception x) { return false; } })
                    .map(el -> el.getAttribute("id"))
                    .filter(id -> id != null && !id.isEmpty())
                    .limit(20)
                    .collect(Collectors.joining(", "));
            } catch (Exception ignored) {}
            sa.fail("[QAT Preview] Demographic questions did not appear for the logged-in preview." +
                " URL: " + pageUrl + " | Title: " + pageTitle + " | Visible ids: " + visibleIds);
            return;
        }

        // Expectation differs by mode: the logged-in preview asks ONLY gender + age before the
        // consent form (the guest pass asks every demographic). agreeToConsent waits for the
        // consent button next, so it soft-fails if an unexpected third demographic appears here.
        answerDemographicQuestion("Male", sa);       // gender
        answerDemographicQuestion("25 to 34", sa);   // age
        agreeToConsent(sa);                           // consent form

        // Not Interested — wait until clickable, then click. Filters this participant out.
        try {
            WebElement notInterested = new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(notInterestedOption));
            scrollToCenter(notInterested);
            jsClick(notInterestedOption);
        } catch (Exception e) {
            sa.fail("[QAT NotInterested] Not-Interested-Option did not become clickable within 30s");
            return;
        }

        // Clicking Not Interested should redirect back to the asset-upload step —
        // qat_assets_next_button reappearing confirms it.
        try {
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.visibilityOfElementLocated(assetsNextButton));
        } catch (Exception e) {
            sa.fail("[QAT NotInterested] Did not redirect to the asset-upload step — " +
                "qat_assets_next_button not visible within 30s");
        }
    }

    // Guest pass: with no saved session, EVERY demographic question is shown (not just gender
    // + age — that is the logged-in expectation). Answer them one at a time, in display order —
    // answerDemographicQuestion asserts each step (option clickable → continue clickable → page
    // advances), so a missing or out-of-order question surfaces as a soft failure naming it.
    @Step("Answer all demographic questions in order as a guest")
    public void answerAllDemographicsAsGuest(SoftAssert sa) {
        answerDemographicQuestion("Male", sa);                      // gender
        answerDemographicQuestion("25 to 34", sa);                  // age
        answerDemographicQuestion("Full-Time Employee", sa);        // employment status
        answerDemographicQuestion("Single", sa);                    // marital status
        answerDemographicQuestion("Homeowner", sa);                 // housing
        answerDemographicQuestion("1", sa);                         // household size
        answerDemographicQuestion("<50k", sa);                      // income
        answerDemographicQuestion("Master’s degree or higher", sa); // education
    }

    // ── QAT selection page ──────────────────────────────────────────────────────

    // Creatives render into divs with the shared id "image" (CDN url on the inner <img>) — on
    // the selection page AND inside the survey questions. Returns the srcs of only the imgs that
    // are actually VISIBLE and fully LOADED (laid out with width/height, complete, naturalWidth>0).
    // This is the "proper visibility wait" filter: it ignores hidden leftover elements from a
    // previous screen and not-yet-loaded placeholders, so reads reflect what the user truly sees.
    // Collects srcs of #image imgs that are LOADED and ACTUALLY VISIBLE.
    // Two filters:
    //  1. complete && naturalWidth > 0 — image has been fetched and decoded by the browser.
    //  2. Ancestor-visibility walk — skips images whose parent chain contains display:none,
    //     visibility:hidden, or opacity:0. Bubble's SPA hides previous-screen images this way
    //     rather than removing them from the DOM; without this check, stale images from an
    //     earlier screen appear as "displayed" alongside the current question's creative.
    @SuppressWarnings("unchecked")
    private List<String> displayedCreativeSrcs() {
        Object res = ((JavascriptExecutor) driver).executeScript(
            "function ancestorsVisible(el) {" +
            "  while (el) {" +
            "    var s = window.getComputedStyle(el);" +
            "    if (s.display === 'none' || s.visibility === 'hidden' || parseFloat(s.opacity || '1') === 0) return false;" +
            "    el = el.parentElement;" +
            "  }" +
            "  return true;" +
            "}" +
            "return Array.prototype.slice.call(document.querySelectorAll('#image img'))" +
            "  .filter(function(i){ return i.complete && i.naturalWidth > 0 && ancestorsVisible(i); })" +
            "  .map(function(i){ return i.currentSrc || i.src; });");
        List<String> out = new ArrayList<>();
        if (res instanceof List) {
            for (Object o : (List<Object>) res) {
                if (o != null && !o.toString().isEmpty()) out.add(o.toString());
            }
        }
        return out;
    }

    // Normalized (query-stripped) keys of the currently displayed creatives.
    private Set<String> displayedCreativeKeys() {
        return displayedCreativeSrcs().stream()
            .map(QatProjectPage::creativeKey)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toSet());
    }

    // Scrolls every #image container into the viewport so the browser fetches CDN images.
    // Must be called before any creative-key check — same as the selection-page pre-scroll.
    private void scrollCreativesIntoView() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('#image').forEach(function(el){" +
                "  el.scrollIntoView({behavior:'instant',block:'center'});" +
                "});");
        } catch (Exception ignored) {}
    }

    // Waits (up to 20s) until every expected key is present among the displayed creatives.
    // Extra/ghost images carry different keys, so they neither satisfy nor break this wait.
    private boolean waitForCreativeKeys(Set<String> expectedKeys) {
        scrollCreativesIntoView();
        try {
            new FluentWait<>(driver)
                .withTimeout(20, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> displayedCreativeKeys().containsAll(expectedKeys));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Verifies every uploaded creative is displayed on the selection page. Normalizes both
    // sides with QatProjectPage.creativeKey so the responsive query params (?w=&dpr=…), which
    // differ between the upload view and this view, don't cause false mismatches.
    @Step("Verify all uploaded creatives are displayed on the QAT selection page")
    public void verifyAllCreativesDisplayed(Map<String, String> uploadedSrcByVersion, SoftAssert sa) {
        int expected = uploadedSrcByVersion.size();
        // Scroll each #image container into the viewport so the browser fetches the CDN image.
        // Lazy-loaded or off-screen images may have naturalWidth==0 until they enter the viewport.
        try {
            ((JavascriptExecutor) driver).executeScript(
                "document.querySelectorAll('#image').forEach(function(el){" +
                "  el.scrollIntoView({behavior:'instant',block:'center'});" +
                "});");
        } catch (Exception ignored) {}

        try {
            new FluentWait<>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> displayedCreativeSrcs().size() >= expected);
        } catch (Exception e) {
            sa.fail("[QAT Selection] Expected " + expected + " creative image(s) but the " +
                "selection page did not render them within 30s (found " +
                displayedCreativeSrcs().size() + ")");
        }

        Set<String> displayedKeys = displayedCreativeKeys();
        System.out.println("[QAT Selection] Displayed creative keys (" + displayedKeys.size() + "): " + displayedKeys);

        for (Map.Entry<String, String> e : uploadedSrcByVersion.entrySet()) {
            String expectedKey = QatProjectPage.creativeKey(e.getValue());
            System.out.println("[QAT Selection] Version '" + e.getKey() +
                "' expected key: " + expectedKey);
            if (expectedKey == null || !displayedKeys.contains(expectedKey)) {
                sa.fail("[QAT Selection] Uploaded creative for version '" + e.getKey() +
                    "' is not displayed on the selection page (expected key: " + expectedKey + ")");
            } else {
                System.out.println("[QAT Selection] MATCHED — version '" + e.getKey() +
                    "' creative confirmed displayed: " + expectedKey);
            }
        }
    }

    // Selects a creative version and advances to the survey questions.
    //  1. choose-version{X} picks the version; once chosen it is replaced by selected-version{X}.
    //  2. input1 takes a (required) free-text reason for the choice.
    //  3. A REAL click on choice-question-title blurs input1 — Bubble's reactive validation runs
    //     on blur and enables the buttons (a JS click wouldn't move focus, so a native click is used).
    //  4. next-button (now enabled) reveals all the questions in their configured order.
    // version is the capitalised letter used in the ids ("A" / "B"). The reason is a fixed
    // >50-word text (SELECTION_REASON) since the platform rejects shorter input.
    @Step("Select creative version {version} with a reason and continue to the questions")
    public void selectVersionAndContinue(String version, SoftAssert sa) {
        By chooseBtn      = By.id("choose-version" + version);
        By selectedMarker = By.id("selected-version" + version);
        By reasonInput    = By.id("input1");
        By choiceTitle    = By.id("choice-question-title");
        By nextButton     = By.id("next-button");

        // 1. Click the choose button for this version.
        try {
            WebElement btn = new FluentWait<>(driver)
                .withTimeout(20, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(chooseBtn));
            scrollToCenter(btn);
            jsClick(chooseBtn);
        } catch (Exception e) {
            sa.fail("[QAT Selection] choose-version" + version + " not clickable");
            return;
        }

        // 2. Selection is confirmed when the choose button is replaced by selected-version{X}.
        try {
            new WebDriverWait(driver, 20).until(
                ExpectedConditions.invisibilityOfElementLocated(chooseBtn));
            new WebDriverWait(driver, 20).until(
                ExpectedConditions.visibilityOfElementLocated(selectedMarker));
        } catch (Exception e) {
            sa.fail("[QAT Selection] version " + version + " selection not confirmed — " +
                "choose-version" + version + " did not disappear or selected-version" + version +
                " did not appear");
        }

        // 3. Enter the reason (must be > 50 words or the platform rejects it).
        try {
            WebElement input = new WebDriverWait(driver, 15).until(
                ExpectedConditions.visibilityOfElementLocated(reasonInput));
            scrollToCenter(input);
            input.clear();
            input.sendKeys(SELECTION_REASON);
        } catch (Exception e) {
            sa.fail("[QAT Selection] reason field input1 not available: " + e.getMessage());
        }

        // 4. Native click on the title to blur input1 and enable the buttons.
        try {
            click(choiceTitle);
        } catch (Exception e) {
            sa.fail("[QAT Selection] could not click choice-question-title to blur the reason field");
        }

        // 5. Next — enabled once a version and a reason are provided.
        try {
            clickWhenEnabled(nextButton);
        } catch (Exception e) {
            sa.fail("[QAT Selection] next-button did not become enabled/clickable");
            return;
        }
        // Advancing to the survey questions — next-button disappearing confirms the transition.
        try {
            new WebDriverWait(driver, 20).until(
                ExpectedConditions.invisibilityOfElementLocated(nextButton));
        } catch (Exception ignored) {}
    }

    // ── Survey questions (same handling as the msjourney participant form) ───────
    // The questions appear in the order they were configured. Answered by clicking choice
    // options (single-choice-/multiple-choice-{lowercase text}) then Continue, looping until
    // the survey ends (login redirect). Answer texts mirror FormQuestionsSteps; Q1 is answered
    // with the filter answer "Strongly appealing" so the response-filtered Q2 and Q3 appear.

    @Step("Answer all QAT survey questions in order, verifying the displayed creative per question")
    public void answerAllSurveyQuestions(Map<String, String> uploadedSrc, String selectedVersion,
                                         String q1Answer, SoftAssert sa) {
        String previousTitle = null;
        for (int i = 0; i < 8; i++) {  // 3 questions + margin
            if (isLoginPageVisible()) break;
            String title = waitForQuestionTitle(sa, previousTitle);
            if (title == null) break;
            previousTitle = title;
            System.out.println("[QAT Survey] Answering: " + title);
            answerSurveyQuestion(title, uploadedSrc, selectedVersion, q1Answer, sa);
            clickContinue(sa);
        }
    }

    // Each question shows a creative via the shared id "image". Verify it (against the srcs
    // stored at upload time, normalized with creativeKey) BEFORE answering, then click options.
    private void answerSurveyQuestion(String title, Map<String, String> uploadedSrc,
                                      String selectedVersion, String q1Answer, SoftAssert sa) {
        if (title.contains("overall appeal")) {                 // Q1 — all_creatives
            verifyAllCreativesShown(uploadedSrc, sa);
            clickSingleChoice(q1Answer, sa);                    // use actual value (may have been retriggered)
        } else if (title.contains("stand out")) {               // Q2 — top_1_choice
            verifyOnlyChosenCreativeShown(uploadedSrc, selectedVersion, sa);
            clickSingleChoice("Yes, it stood out", sa);
        } else if (title.contains("aspects of this version")) { // Q3 — specific_creative (version "b")
            verifySpecificCreativeShown(uploadedSrc, "b", sa);
            clickMultipleChoice(Arrays.asList("Colour", "Layout"), sa);
        } else {
            sa.fail("[QAT Survey] Unrecognised question title: " + title);
        }
    }

    // all_creatives: BOTH versions must be displayed; extra/ghost images (different urls) ignored.
    private void verifyAllCreativesShown(Map<String, String> uploadedSrc, SoftAssert sa) {
        String ka = QatProjectPage.creativeKey(uploadedSrc.get("a"));
        String kb = QatProjectPage.creativeKey(uploadedSrc.get("b"));
        Set<String> expected = new HashSet<>(Arrays.asList(ka, kb));
        expected.remove(null);
        System.out.println("[QAT Survey/all_creatives] Expected keys: " + expected);
        if (expected.size() < 2) {
            sa.fail("[QAT Survey/all_creatives] missing stored upload src(s) — cannot verify both creatives");
            return;
        }
        if (!waitForCreativeKeys(expected)) {
            System.out.println("[QAT Survey/all_creatives] Displayed keys: " + displayedCreativeKeys());
            sa.fail("[QAT Survey/all_creatives] both creatives not displayed within 20s — expected " +
                expected + ", displayed " + displayedCreativeKeys());
        } else {
            System.out.println("[QAT Survey/all_creatives] MATCHED — both creatives displayed" +
                " (ghost/extra images ignored). Displayed: " + displayedCreativeKeys());
        }
    }

    // top_1_choice: ONLY the version the user picked on the selection page should be shown.
    private void verifyOnlyChosenCreativeShown(Map<String, String> uploadedSrc,
                                               String selectedVersion, SoftAssert sa) {
        String v = selectedVersion == null ? null : selectedVersion.toLowerCase();
        if (v == null) {
            sa.fail("[QAT Survey/top_1_choice] no selected version recorded — cannot verify");
            return;
        }
        verifyExactlyVersionShown("top_1_choice", uploadedSrc, v, sa);
    }

    // specific_creative: the version chosen in form setup ("b") should be shown and match its
    // uploaded url.
    private void verifySpecificCreativeShown(Map<String, String> uploadedSrc,
                                             String version, SoftAssert sa) {
        verifyExactlyVersionShown("specific_creative", uploadedSrc, version, sa);
    }

    // Asserts the expected version's creative is displayed (matching its uploaded url) and the
    // OTHER version is not. Ghost images carry different keys, so they don't trip the "other" check.
    private void verifyExactlyVersionShown(String label, Map<String, String> uploadedSrc,
                                           String version, SoftAssert sa) {
        String expected = QatProjectPage.creativeKey(uploadedSrc.get(version));
        String other    = QatProjectPage.creativeKey(uploadedSrc.get(version.equals("a") ? "b" : "a"));
        System.out.println("[QAT Survey/" + label + "] Expected key (version " + version + "): " + expected);
        System.out.println("[QAT Survey/" + label + "] Other version key (should be absent): " + other);
        if (expected == null) {
            sa.fail("[QAT Survey/" + label + "] no stored upload src for version " + version);
            return;
        }
        if (!waitForCreativeKeys(java.util.Collections.singleton(expected))) {
            System.out.println("[QAT Survey/" + label + "] Displayed keys: " + displayedCreativeKeys());
            sa.fail("[QAT Survey/" + label + "] version " + version + " creative not displayed within 20s — " +
                "expected " + expected + ", displayed " + displayedCreativeKeys());
            return;
        }
        System.out.println("[QAT Survey/" + label + "] MATCHED — version " + version +
            " creative displayed. Displayed keys: " + displayedCreativeKeys());
        if (other != null && displayedCreativeKeys().contains(other)) {
            sa.fail("[QAT Survey/" + label + "] the non-selected version is also displayed: " + other);
        } else {
            System.out.println("[QAT Survey/" + label + "] Correct — other version ('" + other + "') is absent");
        }
    }

    private void clickSingleChoice(String optionText, SoftAssert sa) {
        String lc = optionText.toLowerCase();
        By singleLoc   = By.id("single-choice-" + lc);
        By multipleLoc = By.id("multiple-choice-" + lc);

        // Wait specifically for THIS option's id to appear — not just any choice element.
        // Bubble keeps the previous question's options in the DOM briefly after the title
        // changes, so waiting for "any" choice element resolves immediately against stale
        // options from the prior question before the new ones have rendered.
        WebElement target = null;
        try {
            target = new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(d -> {
                    List<WebElement> s = d.findElements(singleLoc);
                    if (!s.isEmpty()) return s.get(0);
                    List<WebElement> m = d.findElements(multipleLoc);
                    if (!m.isEmpty()) return m.get(0);
                    return null;
                });
        } catch (Exception e) {
            String rendered = driver.findElements(
                    By.cssSelector("[id^='single-choice-'],[id^='multiple-choice-']"))
                .stream().map(el -> el.getAttribute("id")).collect(Collectors.joining(", "));
            sa.fail("[QAT Survey] Single choice option not found/clickable: " + optionText +
                ". Available ids: [" + rendered + "]");
            return;
        }
        try {
            scrollToCenter(target);
            target.click();
            System.out.println("[QAT Survey] CLICKED single choice: " + optionText);
        } catch (Exception e) {
            sa.fail("[QAT Survey] Single choice option not clickable: " + optionText);
        }
    }

    private void clickMultipleChoice(List<String> options, SoftAssert sa) {
        for (String option : options) {
            By locator = By.id("multiple-choice-" + option.toLowerCase());
            // Wait specifically for this option's id — same reason as clickSingleChoice.
            try {
                WebElement el = new FluentWait<>(driver)
                    .withTimeout(15, TimeUnit.SECONDS)
                    .pollingEvery(500, TimeUnit.MILLISECONDS)
                    .ignoring(Exception.class)
                    .until(d -> {
                        List<WebElement> els = d.findElements(locator);
                        return els.isEmpty() ? null : els.get(0);
                    });
                scrollToCenter(el);
                el.click();
                System.out.println("[QAT Survey] CLICKED multiple choice: " + option);
                try { Thread.sleep(500); } catch (InterruptedException i) { Thread.currentThread().interrupt(); }
            } catch (Exception e) {
                String rendered = driver.findElements(By.cssSelector("[id^='multiple-choice-']"))
                    .stream().map(el -> el.getAttribute("id")).collect(Collectors.joining(", "));
                sa.fail("[QAT Survey] Multiple choice option not found/clickable: " + option +
                    ". Available ids: [" + rendered + "]");
            }
        }
    }

    // Polls until the title is non-empty and different from the previous one (so we don't act
    // on a stale title before Bubble.io replaces it after Continue). Returns null on login redirect.
    private String waitForQuestionTitle(SoftAssert sa, String previousTitle) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (isLoginPageVisible()) return null;
            try {
                List<WebElement> els = driver.findElements(QUESTION_TITLE);
                if (!els.isEmpty()) {
                    String text = els.get(0).getText().trim();
                    if (!text.isEmpty() && (previousTitle == null || !text.equals(previousTitle))) {
                        return text;
                    }
                }
            } catch (Exception ignored) {}
            try { Thread.sleep(500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        if (!isLoginPageVisible()) {
            sa.fail("[QAT Survey] Question title did not appear within 30s");
        }
        return null;
    }

    private void clickContinue(SoftAssert sa) {
        try {
            new FluentWait<>(driver)
                .withTimeout(10, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.elementToBeClickable(CONTINUE_BTN));
            jsClick(CONTINUE_BTN);
            try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        } catch (Exception e) {
            sa.fail("[QAT Survey] Continue button not clickable: " + e.getMessage());
        }
    }

    private boolean isLoginPageVisible() {
        return !driver.findElements(LOGIN_MARKER).isEmpty();
    }

    // ── Session management / guest pass ─────────────────────────────────────────

    @Step("Clear cookies and web storage for the current session")
    public void clearSession() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || !currentUrl.contains("platform.aybee.ai")) {
            driver.get(ConfigReader.get("BASE_URL"));
        }
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
            "window.localStorage.clear(); window.sessionStorage.clear();");
        // Navigate away from the domain fully before loading the preview URL so Bubble.io
        // initialises a clean guest session rather than reusing stale SPA state.
        driver.get("about:blank");
        System.out.println("[Session] Cookies and storage cleared");
    }

    // Clears the session then re-opens the preview URL as an unauthenticated participant.
    // Bubble.io's SPA can get stuck on a loading screen after a session clear — if the
    // demographic options don't appear within 15s, refresh once and wait up to 45s more.
    @Step("Clear session and open preview URL as guest")
    public void navigateAsGuest(String previewUrl) {
        clearSession();
        driver.get(previewUrl);
        try {
            new WebDriverWait(driver, 15).until(
                ExpectedConditions.presenceOfElementLocated(anyDemographicOption));
        } catch (Exception e) {
            System.out.println("[Session] Demographic options not yet visible — refreshing page");
            driver.navigate().refresh();
            new WebDriverWait(driver, 45).until(
                ExpectedConditions.presenceOfElementLocated(anyDemographicOption));
        }
    }
}
