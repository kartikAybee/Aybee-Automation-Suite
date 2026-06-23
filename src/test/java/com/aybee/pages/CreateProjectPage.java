package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CreateProjectPage extends BasePage {

    // RGshop appearing confirms the QAT project page loaded after country selection.
    static final By STAGE_LANDMARK = By.id("RGshop");

    @Step("Wait for QAT create project page to load (RGshop landmark)")
    public boolean waitUntilLoaded() {
        try {
            new WebDriverWait(driver, 45)
                    .until(ExpectedConditions.visibilityOfElementLocated(STAGE_LANDMARK));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLoaded() {
        return isElementVisible(STAGE_LANDMARK);
    }
}
