package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.ExperimentSettingsPage;
import io.cucumber.java.en.And;

public class ExperimentSettingsSteps {

    private static final String STUDY_OBJECTIVE = "Evaluate consumer purchase intent and price sensitivity for our new D2C product line across key demographic segments in the US marketplace to inform go-to-market strategy.";

    private final ScenarioContext context;
    private final ExperimentSettingsPage settingsPage = new ExperimentSettingsPage();

    public ExperimentSettingsSteps(ScenarioContext context) {
        this.context = context;
    }

    @And("I enter the study objective")
    public void iEnterTheStudyObjective() {
        settingsPage.enterStudyObjective(STUDY_OBJECTIVE);
    }

    @And("I click continue to generate business questions")
    public void iClickContinueToGenerateBusinessQuestions() {
        settingsPage.clickContinue();
    }

    @And("I add all business questions")
    public void iAddAllBusinessQuestions() {
        settingsPage.addAllBusinessQuestions();
    }

    @And("I proceed to shop setup")
    public void iProceedToShopSetup() {
        settingsPage.proceedToShopSetup();
    }
}
