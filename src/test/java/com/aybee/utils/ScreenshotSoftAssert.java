package com.aybee.utils;

import com.aybee.driver.DriverManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.asserts.IAssert;
import org.testng.asserts.SoftAssert;

import java.io.ByteArrayInputStream;
import java.util.concurrent.atomic.AtomicInteger;

// SoftAssert that captures a screenshot the INSTANT each assertion fails — while the browser is
// still on the page that produced the failure — instead of only at assertAll() time (in the @After
// hook), by which point the driver has already navigated away. Each screenshot is attached to the
// Allure report at the failing body step and labelled with the failing assertion's message and the
// current URL, so it is immediately clear which page each failure came from.
//
// SoftAssert.doAssert() records the error in its own error map AFTER calling onAssertFailure(), so
// overriding onAssertFailure() here does not interfere with assertAll() still throwing at the end.
public class ScreenshotSoftAssert extends SoftAssert {

    private static final AtomicInteger FAILURE_COUNT = new AtomicInteger(0);

    @Override
    public void onAssertFailure(IAssert<?> assertCommand, AssertionError ex) {
        int n = FAILURE_COUNT.incrementAndGet();
        String message = assertCommand.getMessage();

        String url = "";
        try { url = DriverManager.getDriver().getCurrentUrl(); } catch (Exception ignored) {}

        String title = "Assertion Failure #" + n
                + (message != null && !message.isEmpty() ? " — " + message : "")
                + (url != null && !url.isEmpty() ? "  [" + url + "]" : "");

        try {
            byte[] screenshot = ((TakesScreenshot) DriverManager.getDriver())
                    .getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(title, new ByteArrayInputStream(screenshot));
        } catch (Exception e) {
            System.out.println("[SoftAssert] Could not capture screenshot for failure #" + n
                    + ": " + e.getMessage());
        }

        System.out.println("[SoftAssert] FAILURE #" + n + " at [" + url + "]: " + message);

        // Preserve base behaviour (records/reports the error via the normal SoftAssert path).
        super.onAssertFailure(assertCommand, ex);
    }
}
