package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class ExperimentsPage extends BasePage {

    private final By navigatorProjectsButton = By.id("navigator_projects_button");
    private final By addNewExperimentButton  = By.id("experiments-page-add-new-experiment");
    // productDevelopmentUseCase, marketplaceSimulationSection, and unitedStatesButton live in NewExperimentPopup.

    @Step("Click Projects Navigator to open the experiments page")
    public ExperimentsPage clickProjectsNavigator() {
        // Navigator sidebar items don't follow the filled-button colour pattern that
        // clickWhenEnabled() relies on — jsClick dispatches the event directly.
        jsClick(navigatorProjectsButton);
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.visibilityOfElementLocated(addNewExperimentButton));
        return this;
    }

    @Step("Click Add New Experiment")
    public ExperimentsPage clickAddNewExperiment() {
        jsClick(addNewExperimentButton);
        return this;
    }
}
