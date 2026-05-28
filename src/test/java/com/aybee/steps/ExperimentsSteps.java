package com.aybee.steps;

import com.aybee.context.ScenarioContext;
import com.aybee.pages.ExperimentsPage;
import io.cucumber.java.en.When;

public class ExperimentsSteps {

    private final ScenarioContext context;
    private ExperimentsPage experimentsPage;

    public ExperimentsSteps(ScenarioContext context) {
        this.context = context;
    }

    @When("I navigate to the experiments page")
    public void iNavigateToTheExperimentsPage() {
        experimentsPage = new ExperimentsPage();
        experimentsPage.clickProjectsNavigator();
    }

    @When("I click add new experiment")
    public void iClickAddNewExperiment() {
        experimentsPage.clickAddNewExperiment();
    }
}
