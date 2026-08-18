package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.NewExperimentPopup;
import com.aybee.pages.ShopSetupPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.testng.Assert;

// Step definitions for the Marketing & Ads → PDP Simulation experiment flow.
// The navigation steps ("I navigate to the experiments page", "I click add new experiment")
// live in ExperimentsSteps — Cucumber shares step definitions globally across glue classes.
public class PdpSimulationSteps {

    private final ScenarioContext context;
    private final NewExperimentPopup popup = new NewExperimentPopup();

    public PdpSimulationSteps(ScenarioContext context) {
        this.context = context;
    }

    @And("I select the Marketing and Ads use case")
    public void iSelectTheMarketingAndAdsUseCase() {
        popup.selectMarketingAndAdsUseCase();
    }

    @And("I scroll to and select the PDP Simulation test type")
    public void iScrollToAndSelectThePdpSimulationTestType() {
        popup.selectPdpSimulation();
    }

    @And("I select United States as the target market")
    public void iSelectUnitedStatesAsTheTargetMarket() {
        popup.selectUnitedStates();
    }

    @Then("the shop setup step should be loaded with the Add New Product button clickable")
    public void theShopSetupStepShouldBeLoaded() {
        ShopSetupPage shopSetup = new ShopSetupPage();
        Assert.assertTrue(shopSetup.isAddNewProductButtonClickable(),
                "Shop Setup step (PDP Simulation step 1) did not load — "
                        + "the Add New Product button never became clickable");
    }
}
