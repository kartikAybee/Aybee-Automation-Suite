package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.CreateProjectPage;
import com.aybee.pages.NewExperimentPopup;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import org.testng.Assert;

public class NewExperimentPopupSteps {

    private final ScenarioContext context;
    private final NewExperimentPopup popup = new NewExperimentPopup();

    public NewExperimentPopupSteps(ScenarioContext context) {
        this.context = context;
    }

    @And("I select the Product Development use case")
    public void iSelectTheProductDevelopmentUseCase() {
        popup.selectProductDevelopmentUseCase();
    }

    @And("I select the D2C test type")
    public void iSelectTheD2CTestType() {
        popup.selectD2C();
    }

    @And("I select United States as the target market")
    public void iSelectUnitedStatesAsTheTargetMarket() {
        popup.selectUnitedStates();
    }

    @Then("I should be on the create project page")
    public void iShouldBeOnTheCreateProjectPage() {
        CreateProjectPage createProjectPage = new CreateProjectPage();
        Assert.assertTrue(createProjectPage.waitUntilLoaded(),
                "Create project page (Stage 1 — Experiment Settings) did not load within timeout");
    }
}
