package com.aybee.hooks;

import com.aybee.context.GlobalTestState;
import com.aybee.context.ScenarioContext;
import com.aybee.driver.DriverManager;
import com.aybee.utils.JamManager;
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
        JamManager.startRecording();
        // Restore persistent cross-scenario state (preview URL, product snapshots, etc.)
        // into the fresh PicoContainer-scoped context so subsequent feature files can use it.
        GlobalTestState.restoreInto(context);
    }

    @After
    public void tearDown(Scenario scenario) {
        // Persist context state so the next feature file's scenario can read it.
        GlobalTestState.saveFrom(context);

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

            String jamUrl = JamManager.stopAndGetLink();
            if (jamUrl != null) {
                Allure.addAttachment("Jam Recording", "text/plain", jamUrl);
            }
        } else {
            JamManager.discardRecording();
        }

        // Do NOT call DriverManager.quitDriver() — browser is reused; JVM shutdown hook handles cleanup.
        if (softFailure != null) throw softFailure;
    }
}
