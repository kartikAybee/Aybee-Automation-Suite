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

    @And("I add a product variation for CTR optimization")
    public void iAddAProductVariationForCtrOptimization() {
        shopSetupPage.waitForAllProductCardsLoaded();
        shopSetupPage.clickAddVariation();

        // Capture Scenario A before any edits — blocks until both name and price are populated.
        context.scenarioAProduct = shopSetupPage.captureScenarioAFromPopup();

        // Delete only the first picture (the visual variable for CTR tests).
        // Bug: deleting the picture clears all popup fields on Bubble.io.
        // restoreScenarioAFields() re-enters exact Scenario A values so only the image differs.
        shopSetupPage.deleteMainPicture()
                     .restoreScenarioAFields(context.scenarioAProduct);

        // Both scenarios share the same name — store identically for competitor detection.
        context.scenarioAProductName = shopSetupPage.getCapturedScenarioAName();
        context.scenarioBProductName = shopSetupPage.getCapturedScenarioBName();

        // Capture Scenario B after restore — same name + price as A, different image src.
        context.scenarioBProduct = shopSetupPage.captureScenarioBFromPopup(context.scenarioAProduct);

        shopSetupPage.saveChanges();
    }

    @And("I proceed to form questions")
    public void iProceedToFormQuestions() {
        shopSetupPage.proceedToFormQuestions();
    }
}
