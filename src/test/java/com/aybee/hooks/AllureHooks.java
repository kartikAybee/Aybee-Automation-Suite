package com.aybee.hooks;

import com.aybee.context.ScenarioContext;
import com.aybee.driver.DriverManager;
import com.aybee.utils.DiagnosticsCollector;
import com.aybee.utils.JamManager;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
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
    public void setUp(Scenario scenario) {
        DriverManager.getDriver();
        long nowMs = System.currentTimeMillis();
        // Derive a short label from the @caseN tag for the recording filename; fall back to "scenario".
        String label = scenario.getSourceTagNames().stream()
            .filter(t -> t.startsWith("@case"))
            .map(t -> t.replace("@", ""))
            .findFirst().orElse("scenario");
        JamManager.startRecording(label);
        DiagnosticsCollector.reset(nowMs);
    }

    // Capture a screenshot at the body step the moment a step fails (hard assert / exception).
    // Runs after every step; only fires when that step just failed, so it lands inline in the
    // scenario body — not in the "Tear down" fixture. Soft-assert failures are captured separately,
    // at fail-time, by ScreenshotSoftAssert.
    @AfterStep
    public void screenshotOnStepFailure(Scenario scenario) {
        if (!scenario.isFailed()) return;
        try {
            byte[] screenshot = ((TakesScreenshot) DriverManager.getDriver())
                    .getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment("Failure Screenshot", new ByteArrayInputStream(screenshot));
        } catch (Exception ignored) {}
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
            // No teardown screenshot: hard-assert failures are captured inline by the @AfterStep
            // hook above, and soft-assert failures by ScreenshotSoftAssert — both at the body step.
            String pageUrl = "";
            try { pageUrl = DriverManager.getDriver().getCurrentUrl(); } catch (Exception ignored) {}
            String diagnostics = DiagnosticsCollector.collectAndFormat(DriverManager.getDriver());
            String jamUrl = JamManager.stopAndUpload(pageUrl, scenario.getName(), diagnostics);
            if (jamUrl != null) {
                Allure.addAttachment("Jam Recording", "text/plain", jamUrl);
            }
        } else {
            JamManager.discardRecording();
        }

        if (softFailure != null) throw softFailure;
    }
}
