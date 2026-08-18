package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.ShopSetupPage;
import com.aybee.utils.ConfigReader;
import io.cucumber.java.en.And;

public class ShopSetupSteps {

    private final ScenarioContext context;
    private final ShopSetupPage shopSetupPage = new ShopSetupPage();

    public ShopSetupSteps(ScenarioContext context) {
        this.context = context;
    }

    @And("I add a new product via ASIN")
    public void iAddANewProductViaAsin() {
        shopSetupPage.clickAddNewProduct()
                     .clickAddViaAsin()
                     .enterAsin(ConfigReader.get("ASIN_CODE"))
                     .clickAsinTitleToEnableGo()
                     .clickGoAndWaitForProducts();
    }

    @And("I add a product variation with updated price")
    public void iAddAProductVariationWithUpdatedPrice() {
        shopSetupPage.waitForAllProductCardsLoaded();
        shopSetupPage.clickAddVariation();

        // captureScenarioAFromPopup() blocks until both name AND price are populated
        // from the ASIN fetch — this is the explicit "wait for Scenario A name" guard.
        // Captures name, price, and image from the popup before any edits are applied.
        context.scenarioAProduct = shopSetupPage.captureScenarioAFromPopup();

        // Edit the popup fields for Scenario B.
        // removeFirstWordFromProductName() verifies the typed name persisted and retries once.
        // We no longer delete the scenario's main picture (applies to all test types) — only edit name/price.
        shopSetupPage.removeFirstWordFromProductName()
                     .setProductPrice(ConfigReader.get("VARIANT_PRICE"));

        context.scenarioAProductName = shopSetupPage.getCapturedScenarioAName();
        context.scenarioBProductName = shopSetupPage.getCapturedScenarioBName();

        // Capture Scenario B from the same popup after all edits — popup is still open here.
        context.scenarioBProduct = shopSetupPage.captureScenarioBFromPopup(context.scenarioAProduct);

        // saveChanges() includes a pre-save guard that re-types the Scenario B name
        // if a Bubble.io reactive event (picture deletion, price entry) cleared it.
        shopSetupPage.saveChanges();
    }

    @And("I proceed to form questions")
    public void iProceedToFormQuestions() {
        shopSetupPage.proceedToFormQuestions();
    }
}
