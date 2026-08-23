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

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreviewJourneyPage extends BasePage {

    private final By nextOpenerQuestionBtn   = By.id("next-open-product");
    private final By continueButton          = By.id("continue-button");
    private final By continueStatementButton = By.id("continue-statement");

    // ── Demographic questions (Q1–Q9) ─────────────────────────────────────────

    // Three-phase: find+click option, click continue, wait for page to advance.
    // Each phase is independently wrapped — failure in any phase soft-asserts and
    // returns without throwing, so the caller can continue to the next question.
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
            sa.fail("[DemographicQ] Option not found or not clickable: " + optionValue);
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
            sa.fail("[DemographicQ] Continue button not clickable after selecting: " + optionValue);
            return;
        }

        try {
            new FluentWait<>(driver)
                .withTimeout(15, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)
                .ignoring(Exception.class)
                .until(ExpectedConditions.invisibilityOfElementLocated(optionLocator));
        } catch (Exception e) {
            sa.fail("[DemographicQ] Page did not advance after answering: " + optionValue);
        }
    }

    @Step("Answer demographic questions Q1–Q9")
    public PreviewJourneyPage answerDemographicQuestions() {
        SoftAssert sa = new SoftAssert();
        answerDemographicQuestion("Male", sa);
        answerDemographicQuestion("25 to 34", sa);
        answerDemographicQuestion("Full-Time Employee", sa);
        answerDemographicQuestion("Single", sa);
        answerDemographicQuestion("Homeowner", sa);
        answerDemographicQuestion("1", sa);
        answerDemographicQuestion("<50k", sa);
        answerDemographicQuestion("Master’s degree or higher", sa);
        sa.assertAll();
        return this;
    }

    @Step("Answer demographic questions Q3–Q8 (starting from Full-Time Employee)")
    public PreviewJourneyPage answerDemographicQuestionsFromQ3() {
        SoftAssert sa = new SoftAssert();
        answerDemographicQuestion("Full-Time Employee", sa);
        answerDemographicQuestion("Single", sa);
        answerDemographicQuestion("Homeowner", sa);
        answerDemographicQuestion("1", sa);
        answerDemographicQuestion("<50k", sa);
        answerDemographicQuestion("Master’s degree or higher", sa);
        sa.assertAll();
        return this;
    }

    // Logged-in preview shows only gender + age before the consent page — the platform
    // skips the remaining 6 demographic questions for authenticated users.
    @Step("Answer demographic questions for logged-in user (gender + age only)")
    public PreviewJourneyPage answerDemographicQuestionsLoggedIn() {
        SoftAssert sa = new SoftAssert();
        answerDemographicQuestion("Male", sa);
        answerDemographicQuestion("25 to 34", sa);
        sa.assertAll();
        return this;
    }

    // ── Consent statement (Q10) ───────────────────────────────────────────────

    // Agree path — participant consents and is directed to the product list as an
    // active participant whose responses will count toward experiment results.
    // Waits for the agree button itself rather than a specific title-question-N ID,
    // since Bubble.io assigns non-sequential IDs to question titles.
    @Step("Agree to consent statement and wait for marketplace")
    public PreviewJourneyPage agreeToConsentStatement() {
        new FluentWait<>(driver)
            .withTimeout(20, TimeUnit.SECONDS)
            .pollingEvery(500, TimeUnit.MILLISECONDS)
            .ignoring(Exception.class)
            .until(ExpectedConditions.presenceOfElementLocated(By.id("agree-statement-button")));
        jsClick(By.id("agree-statement-button"));
        jsClick(continueStatementButton);
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector("[id='results-page-product-name']")));
        return this;
    }

    // Decline path — participant is filtered out and redirected. Since the preview session
    // has no saved login, the final redirect lands on the login page (toggle-sign-in).
    @Step("Decline consent statement and verify redirect to login page as filtered-out participant")
    public PreviewJourneyPage declineConsentStatement() {
        new FluentWait<>(driver)
            .withTimeout(20, TimeUnit.SECONDS)
            .pollingEvery(500, TimeUnit.MILLISECONDS)
            .ignoring(Exception.class)
            .until(ExpectedConditions.presenceOfElementLocated(By.id("decline-statement-button")));
        jsClick(By.id("decline-statement-button"));
        jsClick(continueStatementButton);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("toggle-sign-in")));
        return this;
    }

    // ── Session management ────────────────────────────────────────────────────

    // Wipes all cookies and web storage for the current origin.
    // Must be called while the browser is already on the target domain — deleteAllCookies()
    // is scoped to the origin currently loaded in the driver. Navigates to BASE_URL first
    // so the call always lands on the correct domain even if the browser is mid-SPA-flow.
    // Exposed publicly so the retry loop in PreviewJourneySteps can call it explicitly
    // at the start of each attempt, making the intent visible at the call site.
    @Step("Clear cookies and web storage for current session")
    public void clearSession() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || !currentUrl.contains("platform.aybee.ai")) {
            driver.get(ConfigReader.get("BASE_URL"));
        }
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
            "window.localStorage.clear(); window.sessionStorage.clear();");
        // Navigate away from the domain so Bubble.io initialises a clean guest session
        // rather than reusing stale SPA state when the preview URL is loaded next.
        driver.get("about:blank");
        System.out.println("[Session] Cookies and storage cleared");
    }

    // Clears the session then navigates to the preview URL as an unauthenticated participant.
    // Bubble.io's SPA never fires window.onload so driver.get() with a large timeout would
    // block; we wait for the first demographic question element to confirm the page is ready.
    @Step("Clear session and navigate to preview URL as guest")
    public void navigateAsGuest(String previewUrl) {
        clearSession();
        driver.get("about:blank");
        driver.get(previewUrl);
        // Wait for the first answer option to appear — continue-button only renders AFTER
        // an option is selected, so it cannot serve as a page-ready indicator here. Reload past
        // Bubble's occasional ghost/blank first render.
        waitForLandmarkElseReload(By.cssSelector("[id^='answer-Option-']"), 20, 2);
    }

    // Clears the session then navigates to the preview URL as a guest participant.
    // Handles the Bubble.io infinite loading bug — if demographic options don't appear
    // within 15 s, refreshes once and waits up to 45 s more.
    @Step("Clear session and navigate to preview URL as guest (CTR — with infinite loading retry)")
    public void navigateAsGuestCtr(String previewUrl) {
        clearSession();
        driver.get("about:blank");
        driver.get(previewUrl);
        By anyDemographicOption = By.cssSelector("[id^='answer-Option-']");
        waitForLandmarkElseReload(anyDemographicOption, 20, 2);
    }

    // CTR preview navigates to the URL without clearing session — the logged-in user
    // sees the demographics flow including scenario selection on the consent page.
    @Step("Navigate to preview URL as logged-in user (no session clearing)")
    public void navigateAsLoggedInUser(String previewUrl) {
        driver.get("about:blank");
        driver.get(previewUrl);
        waitForLandmarkElseReload(By.cssSelector("[id^='answer-Option-']"), 20, 2);
    }

    // ── CTR consent ───────────────────────────────────────────────────────────

    // Waits for the agree button, clicks agree + continue, and waits for the info popup
    // (help-confirm) to confirm the product list page has loaded. Used for both logged-in
    // and guest CTR participants — scenario assignment is handled by the platform.
    @Step("Agree to CTR consent statement and wait for product list")
    public PreviewJourneyPage agreeToConsentCtr() {
        new FluentWait<>(driver)
            .withTimeout(20, TimeUnit.SECONDS)
            .pollingEvery(500, TimeUnit.MILLISECONDS)
            .ignoring(Exception.class)
            .until(ExpectedConditions.presenceOfElementLocated(By.id("agree-statement-button")));
        jsClick(By.id("agree-statement-button"));
        jsClick(continueStatementButton);
        new WebDriverWait(driver, 30)
            .until(ExpectedConditions.presenceOfElementLocated(By.id("help-confirm")));
        return this;
    }

    // ── CTR product confirmation and opener question ──────────────────────────

    // After clicking a product card on the CTR marketplace list, Confirm-choice-CTA appears.
    // Clicking it dismisses the confirmation and triggers the opener question popup.
    @Step("Confirm product selection with Confirm-choice-CTA")
    public PreviewJourneyPage confirmProductSelectionCtr() {
        new WebDriverWait(driver, 30)
            .until(ExpectedConditions.presenceOfElementLocated(By.id("Confirm-choice-CTA")));
        jsClick(By.id("Confirm-choice-CTA"));
        return this;
    }

    // After confirming product selection on CTR, an opener-question popup appears.
    // Selects the first available option and clicks next. Waits for the participant
    // form questions page to load (final-questions-title) instead of product-title,
    // since CTR routes directly to the form questions after the opener question.
    @Step("Select first opener question option and proceed to CTR participant form")
    public PreviewJourneyPage answerOpenerQuestionCtr() {
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(nextOpenerQuestionBtn));
        List<WebElement> options = new FluentWait<>(driver)
            .withTimeout(15, TimeUnit.SECONDS)
            .pollingEvery(500, TimeUnit.MILLISECONDS)
            .ignoring(Exception.class)
            .until(d -> {
                List<WebElement> els = d.findElements(By.cssSelector("[id^='open-product-']"));
                return els.isEmpty() ? null : els;
            });
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", options.get(0));
        jsClick(nextOpenerQuestionBtn);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(nextOpenerQuestionBtn));
        // After the opener the journey either lands on the participant form (final-questions-title —
        // when there are questions to answer, default and/or manual) or, when there are none (e.g.
        // DEFAULT_QUESTIONS=no with no manual questions), proceeds straight to the completion redirect
        // (toggle-sign-in). Accept whichever appears so we never fake-timeout expecting a form that the
        // config says won't exist.
        new WebDriverWait(driver, 30).until(ExpectedConditions.or(
            ExpectedConditions.presenceOfElementLocated(By.id("final-questions-title")),
            ExpectedConditions.presenceOfElementLocated(By.id("toggle-sign-in"))));
        return this;
    }

    // ── Opener question (msjourney / non-CTR) ─────────────────────────────────

    // After selecting a product on the marketplace list, an opener-question popup appears.
    // Options have IDs in the form open-product-{text}; we select the first available one
    // and click next. Waits for the popup to disappear before returning so the next step
    // can safely interact with product detail page elements.
    @Step("Select first opener question option and proceed")
    public PreviewJourneyPage answerOpenerQuestion() {
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(nextOpenerQuestionBtn));
        // FluentWait for options — popup may appear before its options finish rendering.
        List<WebElement> options = new FluentWait<>(driver)
            .withTimeout(15, TimeUnit.SECONDS)
            .pollingEvery(500, TimeUnit.MILLISECONDS)
            .ignoring(Exception.class)
            .until(d -> {
                List<WebElement> els = d.findElements(By.cssSelector("[id^='open-product-']"));
                return els.isEmpty() ? null : els;
            });
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", options.get(0));
        jsClick(nextOpenerQuestionBtn);
        // Wait for the popup to close, then for the product detail page title to confirm
        // the page is ready for subsequent interactions.
        wait.until(ExpectedConditions.invisibilityOfElementLocated(nextOpenerQuestionBtn));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("product-title")));
        return this;
    }
}
