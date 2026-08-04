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
    // The template card's element id now equals the template's NAME, so it is chosen by the required
    // TEMPLATE_NAME config (no code fallback) instead of being hard-coded. The use-case grouping above
    // (Product Development) stays fixed — only the template under it is configurable. Located via
    // [id='...'] so names with spaces/commas/special chars still match.
    private final By templateCard = By.cssSelector(
            "[id='" + ConfigReader.get("TEMPLATE_NAME") + "']");

    // WARNING: "btn-add-united states" contains a whitespace character — technically invalid per the
    // HTML spec (id values must not contain ASCII whitespace). By.id() maps to getElementById() which
    // performs a literal string match and handles spaces in most browsers, but CSS #selector syntax
    // cannot represent them. The attribute selector [id='...'] is used here as it correctly matches
    // the full attribute value including any embedded spaces and is portable across all WebDriver
    // implementations.
    private final By unitedStatesButton = By.cssSelector("[id='btn-add-united states']");

    // Scrolls the element to the centre of the viewport (both axes, to also handle horizontal/side
    // carousels) BEFORE clicking, so a hidden/off-screen card never causes a missed or intercepted
    // click. Every template/use-case selection goes through this.
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
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(templateCard));
        return this;
    }

    @Step("Select D2C test type (by TEMPLATE_NAME)")
    public NewExperimentPopup selectD2C() {
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
