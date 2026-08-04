package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class NewExperimentPopup extends BasePage {

    private final By productDevelopmentUseCase = By.id("product-development-use-case");

    // Bundle + the default template inside it are HARD-CODED (their element ids equal their names).
    // The default CTR template lives inside the "Packaging Optimization" bundle.
    private static final String BUNDLE_NAME              = "Packaging Optimization";
    private static final String DEFAULT_TEMPLATE_IN_BUNDLE = "Competitive Packaging Optimization";

    // TEMPLATE_NAME is the ONLY selection config and is empty by default:
    //   empty  (not overridden) → use the hard-coded default template INSIDE the bundle → the bundle
    //                             is opened first, then that template is clicked.
    //   set    (overridden, e.g. -DTEMPLATE_NAME="...") → that template is a top-level card → it is
    //                             clicked DIRECTLY and the bundle is SKIPPED.
    private final String overrideTemplateName = ConfigReader.get("TEMPLATE_NAME", "").trim();
    private final boolean templateOverridden  = !overrideTemplateName.isEmpty();
    private final By packagingBundle          = By.cssSelector("[id='" + BUNDLE_NAME + "']");
    private final By templateCard             = By.cssSelector("[id='"
            + (templateOverridden ? overrideTemplateName : DEFAULT_TEMPLATE_IN_BUNDLE) + "']");

    // WARNING: "btn-add-united states" contains a whitespace character — the attribute selector
    // [id='...'] is used because CSS #selector syntax cannot represent embedded spaces.
    private final By unitedStatesButton = By.cssSelector("[id='btn-add-united states']");

    // Scroll to the element (both axes, to also handle horizontal/side carousels) BEFORE clicking so a
    // hidden/off-screen card never causes a missed or intercepted click.
    private void scrollToAndClick(By locator) {
        WebElement el = new WebDriverWait(driver, 30)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior:'instant',block:'center',inline:'center'});", el);
        jsClick(locator);
    }

    @Step("Select Product Development use case")
    public NewExperimentPopup selectProductDevelopmentUseCase() {
        scrollToAndClick(productDevelopmentUseCase);
        // Default → the bundle card appears; overridden → the template card appears at the top level.
        new WebDriverWait(driver, 30).until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(templateCard),
                ExpectedConditions.presenceOfElementLocated(packagingBundle)));
        return this;
    }

    @Step("Open the Packaging Optimization bundle (skipped when TEMPLATE_NAME is overridden)")
    public NewExperimentPopup selectPackagingOptimizationSection() {
        if (templateOverridden) {
            System.out.println("[NewExperiment] TEMPLATE_NAME overridden ('" + overrideTemplateName
                    + "') — selecting directly, skipping the bundle");
            return this;
        }
        // Default: open the bundle, then wait for the hard-coded in-bundle template to appear.
        scrollToAndClick(packagingBundle);
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.presenceOfElementLocated(templateCard));
        return this;
    }

    @Step("Select CTR Optimization test type")
    public NewExperimentPopup selectCtrOptimization() {
        scrollToAndClick(templateCard);
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(unitedStatesButton));
        return this;
    }

    @Step("Select United States as target market")
    public NewExperimentPopup selectUnitedStates() {
        scrollToAndClick(unitedStatesButton);
        return this;
    }
}
