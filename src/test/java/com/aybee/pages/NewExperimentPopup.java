package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class NewExperimentPopup extends BasePage {

    private final By productDevelopmentUseCase    = By.id("product-development-use-case");
    private final By marketplaceSimulationSection = By.id("newexperiment_productdevelopment_marketplacesimulation_section");

    // WARNING: "btn-add-united states" contains a whitespace character — technically invalid per the
    // HTML spec (id values must not contain ASCII whitespace). By.id() maps to getElementById() which
    // performs a literal string match and handles spaces in most browsers, but CSS #selector syntax
    // cannot represent them. The attribute selector [id='...'] is used here as it correctly matches
    // the full attribute value including any embedded spaces and is portable across all WebDriver
    // implementations.
    private final By unitedStatesButton = By.cssSelector("[id='btn-add-united states']");

    @Step("Select Product Development use case")
    public NewExperimentPopup selectProductDevelopmentUseCase() {
        wait.until(ExpectedConditions.presenceOfElementLocated(productDevelopmentUseCase));
        jsClick(productDevelopmentUseCase);
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(marketplaceSimulationSection));
        return this;
    }

    @Step("Select Marketplace Simulation test type")
    public NewExperimentPopup selectMarketplaceSimulation() {
        wait.until(ExpectedConditions.presenceOfElementLocated(marketplaceSimulationSection));
        jsClick(marketplaceSimulationSection);
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
