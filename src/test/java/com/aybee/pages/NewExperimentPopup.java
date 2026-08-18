package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// New-experiment popup for the Marketing & Ads → PDP Simulation flow.
public class NewExperimentPopup extends BasePage {

    private final By marketingAndAdsUseCase = By.id("marketing-and-ads-use-case");
    // The template card's element id now equals the template's NAME, so it is chosen by the required
    // TEMPLATE_NAME config (no code fallback). The use-case grouping above (Marketing & Ads) stays
    // fixed — only the template under it is configurable. Located via [id='...'] so names with
    // spaces/special chars still match.
    private final By pdpSimulationSection   = By.cssSelector(
            "[id='" + ConfigReader.get("TEMPLATE_NAME") + "']");

    // WARNING: "btn-add-united states" contains a whitespace character — technically invalid per the
    // HTML spec (id values must not contain ASCII whitespace). By.id() maps to getElementById() which
    // performs a literal string match and handles spaces in most browsers, but CSS #selector syntax
    // cannot represent them. The attribute selector [id='...'] is used here as it correctly matches
    // the full attribute value including any embedded spaces and is portable across all WebDriver
    // implementations.
    private final By unitedStatesButton = By.cssSelector("[id='btn-add-united states']");

    // After the target market is selected, the experiment is only actually created once this
    // Create button is clicked — the country selection alone does not create it.
    private final By createButton = By.id("create-btn");

    @Step("Select Marketing and Ads use case")
    public NewExperimentPopup selectMarketingAndAdsUseCase() {
        wait.until(ExpectedConditions.presenceOfElementLocated(marketingAndAdsUseCase));
        jsClick(marketingAndAdsUseCase);
        // Selecting the use case reveals the test-type carousel that contains the PDP Simulation card.
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.presenceOfElementLocated(pdpSimulationSection));
        return this;
    }

    @Step("Scroll sideways to and select the PDP Simulation test type")
    public NewExperimentPopup selectPdpSimulation() {
        WebElement section = wait.until(
                ExpectedConditions.presenceOfElementLocated(pdpSimulationSection));
        // Horizontal scroll: the card lives in a sideways-scrolling carousel, so centre it on
        // both axes (inline centres it horizontally) before the click so the full click chain fires.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center',inline:'center'});", section);
        jsClick(pdpSimulationSection);
        // The country selector appearing confirms the test type was accepted.
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(unitedStatesButton));
        return this;
    }

    @Step("Select United States as target market")
    public NewExperimentPopup selectUnitedStates() {
        wait.until(ExpectedConditions.presenceOfElementLocated(unitedStatesButton));
        jsClick(unitedStatesButton);
        // Selecting the country is not enough — the experiment is only created after clicking Create.
        WebElement create = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.elementToBeClickable(createButton));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center',inline:'center'});", create);
        jsClick(createButton);
        return this;
    }
}
