package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class NewExperimentPopup extends BasePage {

    private final By productDevelopmentUseCase = By.id("product-development-use-case");
    // A single direct click on the QAT option — no separate segment step is needed.
    private final By quickAssetTestingLabel    = By.id("newexperiment_productdevelopment_qat_qat_section");

    // WARNING: "btn-add-united states" contains a whitespace character — technically invalid per the
    // HTML spec. The attribute selector [id='...'] is used here because CSS #selector syntax
    // cannot represent embedded spaces, while the attribute selector matches the full value safely.
    private final By unitedStatesButton = By.cssSelector("[id='btn-add-united states']");

    @Step("Select Product Development use case")
    public NewExperimentPopup selectProductDevelopmentUseCase() {
        wait.until(ExpectedConditions.presenceOfElementLocated(productDevelopmentUseCase));
        jsClick(productDevelopmentUseCase);
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(quickAssetTestingLabel));
        return this;
    }

    @Step("Select Quick Asset Testing (QAT) test type")
    public NewExperimentPopup selectQuickAssetTesting() {
        wait.until(ExpectedConditions.presenceOfElementLocated(quickAssetTestingLabel));
        jsClick(quickAssetTestingLabel);
        // The target-market country chip appearing confirms the test type was selected.
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
