package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.ExperimentSettingsPage;
import io.cucumber.java.en.And;

// Study Objective + Business Questions — step 1 of the Questionnaire experiment setup.
// Mirrors the msjourney Marketplace Simulation settings flow (same element IDs); the only
// divergence is that proceeding advances straight to the Form Questions step.
public class ExperimentSettingsSteps {

    // A few interchangeable objective sentences — one is picked at random per run and given a
    // random reference token so each run enters a genuinely different Study Objective.
    private static final String[] OBJECTIVE_TEMPLATES = {
        "Evaluate consumer purchase intent and price sensitivity for our new product line across key US demographic segments to inform go-to-market strategy.",
        "Understand how target shoppers perceive product value, quality, and brand trust in order to prioritise messaging for the upcoming US launch.",
        "Measure the appeal of alternative product presentations and identify the attributes that most strongly drive repeat-purchase decisions.",
        "Assess overall satisfaction, likelihood to recommend, and the decision factors that influence buying this product again in the US market."
    };

    private final ScenarioContext context;
    private final ExperimentSettingsPage settingsPage = new ExperimentSettingsPage();

    public ExperimentSettingsSteps(ScenarioContext context) {
        this.context = context;
    }

    @And("I enter a random study objective")
    public void iEnterARandomStudyObjective() {
        int pick = (int) (Math.random() * OBJECTIVE_TEMPLATES.length);
        String ref = Long.toString(Math.abs((long) (Math.random() * 1_000_000)));
        String objective = OBJECTIVE_TEMPLATES[pick] + " (Study ref " + ref + ")";
        settingsPage.enterStudyObjective(objective);
    }

    @And("I continue to generate business questions")
    public void iContinueToGenerateBusinessQuestions() {
        settingsPage.clickContinue();
    }

    @And("I add one business question")
    public void iAddOneBusinessQuestion() {
        settingsPage.addBusinessQuestions();
    }

    @And("I proceed to the form questions step")
    public void iProceedToTheFormQuestionsStep() {
        settingsPage.proceedToFormQuestions();
    }
}
