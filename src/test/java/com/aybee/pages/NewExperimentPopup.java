package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// New-experiment popup for the Marketing & Ads → Questionnaire flow.
public class NewExperimentPopup extends BasePage {

    private final By marketingAndAdsUseCase = By.id("marketing-and-ads-use-case");
    // The Questionnaire test-type card lives in a horizontally-scrolling carousel (Side Scroll),
    // so it is off-screen until scrolled sideways into view before clicking.
    private final By questionnaireSection   = By.id("newexperiment_marketing_ads_questionnaire");

    // WARNING: "btn-add-united states" contains a whitespace character — technically invalid per the
    // HTML spec (id values must not contain ASCII whitespace). By.id() maps to getElementById() which
    // performs a literal string match and handles spaces in most browsers, but CSS #selector syntax
    // cannot represent them. The attribute selector [id='...'] is used here as it correctly matches
    // the full attribute value including any embedded spaces and is portable across all WebDriver
    // implementations.
    private final By unitedStatesButton = By.cssSelector("[id='btn-add-united states']");

    @Step("Select Marketing and Ads use case")
    public NewExperimentPopup selectMarketingAndAdsUseCase() {
        wait.until(ExpectedConditions.presenceOfElementLocated(marketingAndAdsUseCase));
        jsClick(marketingAndAdsUseCase);
        // Selecting the use case reveals the test-type carousel that contains the Questionnaire card.
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.presenceOfElementLocated(questionnaireSection));
        return this;
    }

    // Confirms the Questionnaire card is reachable in the carousel — used by the login/navigation
    // case to assert we have landed in the Marketing & Ads new-experiment section.
    public boolean isQuestionnaireTypeAvailable() {
        try {
            return new WebDriverWait(driver, 30)
                    .until(ExpectedConditions.presenceOfElementLocated(questionnaireSection)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Side-scroll to and select the Questionnaire test type")
    public NewExperimentPopup selectQuestionnaire() {
        WebElement section = wait.until(
                ExpectedConditions.presenceOfElementLocated(questionnaireSection));
        // Horizontal (Side Scroll) carousel: the card lives in a sideways-scrolling container, so
        // centre it on both axes (inline centres it horizontally) before the click so the full
        // click chain fires on a fully-visible target.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center',inline:'center'});", section);
        jsClick(questionnaireSection);
        // The country selector appearing confirms the test type was accepted.
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(unitedStatesButton));
        return this;
    }

    @Step("Select United States as target market")
    public NewExperimentPopup selectUnitedStates() {
        wait.until(ExpectedConditions.presenceOfElementLocated(unitedStatesButton));
        jsClick(unitedStatesButton);
        return this;
    }
}
