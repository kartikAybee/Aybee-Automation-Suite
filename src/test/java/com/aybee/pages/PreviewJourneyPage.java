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

    // Logged-in preview — platform only asks gender and age when a session is already active.
    @Step("Answer demographic questions — gender (Male) and age (25 to 34) only")
    public PreviewJourneyPage answerDemographicsGenderAndAge() {
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
        By agreeBtn = By.id("agree-statement-button");
        WebElement btn = new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(agreeBtn));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", btn);
        jsClick(agreeBtn);
        // continueStatementButton activates asynchronously after the agree click —
        // retry up to 3 times (5 s each) before clicking, matching the D2C consent pattern.
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                new WebDriverWait(driver, 5).until(
                    ExpectedConditions.elementToBeClickable(continueStatementButton));
                break;
            } catch (Exception e) {
                System.out.println("[Consent] Continue not ready after agree click attempt "
                    + attempt + " — retrying");
            }
        }
        jsClick(continueStatementButton);
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.invisibilityOfElementLocated(continueStatementButton));
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
        System.out.println("[Session] Cookies and storage cleared");
    }

    // Clears the session then navigates to the preview URL as an unauthenticated participant.
    // Bubble.io's SPA never fires window.onload so driver.get() with a large timeout would
    // block; we wait for the first demographic question element to confirm the page is ready.
    @Step("Clear session and navigate to preview URL as guest")
    public void navigateAsGuest(String previewUrl) {
        clearSession();
        driver.get(previewUrl);
        // Wait for the first answer option to appear — continue-button only renders AFTER
        // an option is selected, so it cannot serve as a page-ready indicator here.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[id^='answer-Option-']")));
    }

    // After the first not-interested redirects to shop setup, the stored preview URL is
    // spent — revisiting it just redirects back to shop setup again. Instead, click next
    // on the shop setup page to reach the form questions editor, then re-click the preview
    // button to open a fresh session in a new tab, exactly as D2C does.
    @Step("Retrigger preview from shop setup: next → form questions → preview button → new tab")
    public void retriggerPreviewFromShopSetup() {
        By shopSetupNextBtn = By.id("marketplacesimulation_shopsetup_next_button");
        By previewBtn       = By.id("newproject_formquestions_previewjourney_button");

        // Advance from shop setup to form questions.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(shopSetupNextBtn));
        jsClick(shopSetupNextBtn);

        // Wait for the preview button — confirms form questions page is loaded.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.elementToBeClickable(previewBtn));

        // Click preview — opens a new tab with a fresh Bubble.io session.
        String mainWindow = driver.getWindowHandle();
        jsClick(previewBtn);

        // Switch to the new preview tab.
        new WebDriverWait(driver, 30).until(d -> d.getWindowHandles().size() > 1);
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(mainWindow)) {
                driver.switchTo().window(handle);
                break;
            }
        }

        // Wait for the first demographic option — gender + age are shown for logged-in users.
        new WebDriverWait(driver, 30).until(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[id^='answer-Option-']")));
    }

    // ── Opener question ───────────────────────────────────────────────────────

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
