package com.aybee.hooks;

import com.aybee.context.GlobalTestState;
import com.aybee.context.ScenarioContext;
import com.aybee.driver.DriverManager;
import com.aybee.utils.DiagnosticsCollector;
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
    public void setUp(Scenario scenario) {
        DriverManager.getDriver();
        long nowMs = System.currentTimeMillis();
        // Derive a short label from the @caseN tag for the recording filename.
        String label = scenario.getSourceTagNames().stream()
            .filter(t -> t.startsWith("@case"))
            .map(t -> t.replace("@", ""))
            .findFirst().orElse("scenario");
        JamManager.startRecording(label);
        DiagnosticsCollector.reset(nowMs);
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

        // Do NOT call DriverManager.quitDriver() — browser is reused; JVM shutdown hook handles cleanup.
        if (softFailure != null) throw softFailure;
    }
}
