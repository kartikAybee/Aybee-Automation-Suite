package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.asserts.SoftAssert;

// Preview journey — logged-in owner path (Not Interested / No Buying Intent) AND the guest path.
// A logged-in session only asks gender + age; a guest (cleared session) is asked all 8 demographics.
public class PreviewJourneyPage extends BasePage {

    private final By continueButton          = By.id("continue-button");
    private final By continueStatementButton = By.id("continue-statement");
    private final By helpConfirmButton       = By.id("help-confirm");
    private final By notInterestedButton     = By.id("button-not-interest");

    // Navigates to the captured preview URL without clearing the session, so the logged-in owner
    // is recognised and shown only gender + age (not the full 8 demographics).
    @Step("Navigate to preview URL as logged-in user")
    public boolean navigateAsLoggedInUser(String previewUrl) {
        // Clean full load so Bubble re-initialises the preview from scratch (about:blank first avoids
        // an in-app route being reused). The journey opens on the demographics questions (then consent).
        driver.get("about:blank");
        driver.get(previewUrl);
        By anyDemographicOption = By.cssSelector("[id^='answer-Option-']");
        // Returns true once the demographic options render; reloads past Bubble's occasional first-load
        // ghost/blank page.
        return waitForLandmarkElseReload(anyDemographicOption, 20, 2);
    }

    // ── Demographic questions ─────────────────────────────────────────────────

    private String lastAnsweredDemographicOption = null;

    // Three-phase: wait for options fully rendered → click option → click continue → advance.
    private void answerDemographicQuestion(String optionValue, SoftAssert sa) {
        By anyOption     = By.cssSelector("[id^='answer-Option-']");
        By optionLocator = By.id("answer-Option-" + optionValue);

        if (lastAnsweredDemographicOption != null) {
            By prevLocator = By.id("answer-Option-" + lastAnsweredDemographicOption);
            try {
                new WebDriverWait(driver, 10).until(
                    ExpectedConditions.invisibilityOfElementLocated(prevLocator));
            } catch (Exception e) {
                System.out.println("[DemographicQ] Previous option [" + lastAnsweredDemographicOption
                    + "] still visible — page may not have advanced");
            }
        }

        try {
            new WebDriverWait(driver, 15).until(ExpectedConditions.elementToBeClickable(anyOption));
            WebElement option = new WebDriverWait(driver, 15).until(
                ExpectedConditions.elementToBeClickable(optionLocator));
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", option);
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", option);
            lastAnsweredDemographicOption = optionValue;
        } catch (Exception e) {
            sa.fail("[DemographicQ] Option not found or not clickable: " + optionValue);
            return;
        }

        try {
            new WebDriverWait(driver, 15).until(ExpectedConditions.elementToBeClickable(continueButton));
            jsClick(continueButton);
        } catch (Exception e) {
            sa.fail("[DemographicQ] Continue button not clickable after selecting: " + optionValue);
            return;
        }

        try {
            new WebDriverWait(driver, 15).until(
                ExpectedConditions.invisibilityOfElementLocated(optionLocator));
        } catch (Exception e) {
            sa.fail("[DemographicQ] Page did not advance after answering: " + optionValue);
        }
    }

    // Logged-in preview — platform only asks gender and age when a session is already active.
    @Step("Answer demographic questions — gender (Male) and age (25 to 34) only")
    public PreviewJourneyPage answerDemographicsGenderAndAge() {
        lastAnsweredDemographicOption = null;
        SoftAssert sa = new SoftAssert();
        answerDemographicQuestion("Male", sa);
        answerDemographicQuestion("25 to 34", sa);
        sa.assertAll();
        return this;
    }

    // Guest preview — no prior account data, so the platform shows all 8 demographic questions.
    // Same set/order as the d2c guest journey.
    @Step("Answer all 8 guest demographic questions")
    public PreviewJourneyPage answerAllDemographicQuestions() {
        lastAnsweredDemographicOption = null;
        SoftAssert sa = new SoftAssert();
        answerDemographicQuestion("Male", sa);
        answerDemographicQuestion("25 to 34", sa);
        answerDemographicQuestion("Full-Time Employee", sa);
        answerDemographicQuestion("Single", sa);
        answerDemographicQuestion("Homeowner", sa);
        answerDemographicQuestion("1", sa);
        answerDemographicQuestion("<50k", sa);
        answerDemographicQuestion("Less than high school", sa);
        sa.assertAll();
        return this;
    }

    // ── Session management (guest) ──────────────────────────────────────────────

    // Wipes cookies + web storage for the current origin (must be on the platform domain first),
    // exactly like the d2c guest flow, so Bubble.io initialises a clean unauthenticated session.
    @Step("Clear cookies and web storage for current session")
    public void clearSession() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null || !currentUrl.contains("platform.aybee.ai")) {
            driver.get(ConfigReader.get("BASE_URL"));
        }
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript(
            "window.localStorage.clear(); window.sessionStorage.clear();");
        driver.get("about:blank");
        System.out.println("[Session] Cookies and storage cleared");
    }

    // Clears the session then loads the preview URL as a guest. Handles Bubble.io's occasional
    // infinite-loading by refreshing once if demographic options don't appear within 15s.
    @Step("Clear session and open preview URL as guest")
    public void navigateAsGuest(String previewUrl) {
        clearSession();
        driver.get(previewUrl);
        By anyDemographicOption = By.cssSelector("[id^='answer-Option-']");
        // Same Bubble first-load hang as the logged-in path — reload and retry until options render.
        if (!waitForLandmarkElseReload(anyDemographicOption, 15, 2)) {
            throw new org.openqa.selenium.TimeoutException(
                "Guest demographic options never appeared after opening the preview URL (even after reloads)");
        }
    }

    // ── Consent statement ─────────────────────────────────────────────────────

    // Agree path — participant consents and proceeds into the journey.
    @Step("Agree to consent statement and proceed")
    public PreviewJourneyPage agreeToConsentStatement() {
        By agreeBtn = By.id("agree-statement-button");
        new WebDriverWait(driver, 30).until(ExpectedConditions.presenceOfElementLocated(agreeBtn));
        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement btn = driver.findElement(agreeBtn);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center'});", btn);
            jsClick(agreeBtn);
            try {
                new WebDriverWait(driver, 5).until(
                    ExpectedConditions.elementToBeClickable(continueStatementButton));
                break;
            } catch (Exception e) {
                System.out.println("[Consent] Continue not ready after agree click attempt " + attempt + " — retrying");
            }
        }
        jsClick(continueStatementButton);
        new WebDriverWait(driver, 15).until(
            ExpectedConditions.invisibilityOfElementLocated(continueStatementButton));
        return this;
    }

    // ── Help popup ────────────────────────────────────────────────────────────

    // After consent the platform redirects to the product page and a help popup appears
    // (help-confirm). Dismiss it before proceeding. No-op if it never shows.
    @Step("Wait for help popup (help-confirm) and dismiss it")
    public PreviewJourneyPage dismissHelpPopupIfPresent() {
        // If not-interested is already visible, the product page is ready — popup already gone.
        if (!driver.findElements(notInterestedButton).isEmpty()
                && driver.findElement(notInterestedButton).isDisplayed()) {
            System.out.println("[Preview] Not-interested already visible — help popup already dismissed");
            return this;
        }
        boolean present = !driver.findElements(helpConfirmButton).isEmpty()
                && driver.findElement(helpConfirmButton).isDisplayed();
        if (!present) {
            System.out.println("[Preview] Help popup not present — reloading once");
            driver.navigate().refresh();
        }
        try {
            new WebDriverWait(driver, 30).until(
                ExpectedConditions.visibilityOfElementLocated(helpConfirmButton));
            jsClick(helpConfirmButton);
            new WebDriverWait(driver, 10).until(
                ExpectedConditions.invisibilityOfElementLocated(helpConfirmButton));
        } catch (Exception e) {
            System.out.println("[Preview] help-confirm did not appear — proceeding");
        }
        return this;
    }
}
