package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class CreateProjectPage extends BasePage {

    // Stage 1 (Experiment Settings) is the first thing rendered on the create-project page.
    // Its section ID is used as the redirect-confirmation landmark — element visibility proves
    // both that the redirect completed AND that Bubble.io has finished rendering the page.
    // Element-based detection is used instead of URL change because Bubble.io may complete
    // multiple intermediate redirects and the DOM may not be ready when the URL first updates.
    private static final org.openqa.selenium.By landmarkElement =
            ExperimentSettingsPage.STAGE_LANDMARK;

    // Waits up to 45 s — covers server processing time after country selection
    // plus any intermediate redirects Bubble.io performs before rendering stage 1.
    @Step("Wait for create project page to load (stage 1 — Experiment Settings)")
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
