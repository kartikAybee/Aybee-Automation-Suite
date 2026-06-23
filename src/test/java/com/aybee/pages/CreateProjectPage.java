package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CreateProjectPage extends BasePage {

    // CTR experiments skip Experiment Settings and land directly on shop setup.
    // The add-product button visibility confirms the redirect completed and Bubble.io
    // has fully rendered the shop setup page — used instead of URL change detection
    // because Bubble.io may complete multiple intermediate redirects.
    private static final org.openqa.selenium.By landmarkElement =
            org.openqa.selenium.By.id("marketplacesimulation_shopsetup_addnewproduct_button");

    // Waits up to 45 s — covers server processing time after country selection
    // plus any intermediate redirects Bubble.io performs before rendering shop setup.
    @Step("Wait for create project page to load (CTR — shop setup)")
    public boolean waitUntilLoaded() {
        try {
            new WebDriverWait(driver, 45)
                    .until(ExpectedConditions.visibilityOfElementLocated(landmarkElement));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isLoaded() {
        return isElementVisible(landmarkElement);
    }
}
