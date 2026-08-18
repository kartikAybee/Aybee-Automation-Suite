package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.FormQuestionsPage;
import com.aybee.pages.ShopSetupPage;
import com.aybee.utils.ConfigReader;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.Assert;

public class ShopSetupSteps {

    private final ScenarioContext context;
    private final ShopSetupPage shopSetup = new ShopSetupPage();

    public ShopSetupSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I add a product to Scenario A via ASIN")
    public void iAddAProductToScenarioAViaAsin() {
        shopSetup.addProductViaAsin(1, ConfigReader.get("ASIN_CODE"));
    }

    @And("I add a second scenario")
    public void iAddASecondScenario() {
        Assert.assertTrue(shopSetup.addScenarioAndWaitForEdit(2),
                "Scenario B did not appear (2-edit-product-shop-setup) after adding a scenario");
    }

    @And("I add a third scenario")
    public void iAddAThirdScenario() {
        Assert.assertTrue(shopSetup.addScenarioAndWaitForEdit(3),
                "Scenario C did not appear (3-edit-product-shop-setup) after adding a scenario");
    }

    @And("I delete the third scenario")
    public void iDeleteTheThirdScenario() {
        shopSetup.deleteScenario(3);
    }

    @And("I edit the second scenario to trim its product name")
    public void iEditTheSecondScenario() {
        shopSetup.openScenarioForEdit(2);
        // We no longer delete the scenario's main picture (applies to all test types) — only trim the name.
        shopSetup.removeFirstWordFromProductName(2);
    }

    @And("I save the scenario changes")
    public void iSaveTheScenarioChanges() {
        shopSetup.saveChanges();
    }

    // Capture both scenarios' product details (name/price/brand/main-image/prime) from their edit
    // popups and store them so the guest product-detail journey can compare against them.
    @And("I capture both scenario product details")
    public void iCaptureBothScenarioProductDetails() {
        context.scenarioAProduct = shopSetup.captureScenarioSnapshot(1);
        context.scenarioBProduct = shopSetup.captureScenarioSnapshot(2);
        if (context.scenarioAProduct != null) context.scenarioAProductName = context.scenarioAProduct.truncatedName;
        if (context.scenarioBProduct != null) context.scenarioBProductName = context.scenarioBProduct.truncatedName;
    }

    @And("I proceed to the form questions step")
    public void iProceedToTheFormQuestionsStep() {
        shopSetup.proceedToFormQuestions();
    }

    @Then("the form questions step should be loaded with the Add Question button clickable")
    public void theFormQuestionsStepShouldBeLoaded() {
        Assert.assertTrue(new FormQuestionsPage().isAddQuestionButtonClickable(),
                "Form Questions step (PDP Simulation step 2) did not load — "
                        + "the Add Question button never became clickable");
    }
}
