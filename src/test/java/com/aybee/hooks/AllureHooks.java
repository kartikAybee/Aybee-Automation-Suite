package com.aybee.hooks;

import com.aybee.context.ScenarioContext;
import com.aybee.driver.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.ByteArrayInputStream;

public class AllureHooks {

    private final ScenarioContext context;

    public AllureHooks(ScenarioContext context) {
        this.context = context;
    }

    @Before
    public void setUp() {
        DriverManager.getDriver();
    }

    @After
    public void tearDown(Scenario scenario) {
        AssertionError softFailure = null;
        try {
            context.softAssert.assertAll();
        } catch (AssertionError e) {
            softFailure = e;
        }

        if (scenario.isFailed() || softFailure != null) {
            try {
                byte[] screenshot = ((TakesScreenshot) DriverManager.getDriver())
                        .getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment("Failure Screenshot", new ByteArrayInputStream(screenshot));
            } catch (Exception ignored) {}
        }

        if (softFailure != null) throw softFailure;
    }
}
