package com.aybee.driver;

import org.openqa.selenium.WebDriver;

// Manages the two concurrent browser sessions used by the join-request flow:
//   • admin     — the existing default session, logged in as the shared account (approves/denies)
//   • requester — a fresh, independent browser that registers and requests to join
// Switching just points DriverManager at the chosen session; page objects created afterward bind
// to it, so the pattern is: switch → `new SomePage()` → interact.
public class SessionManager {

    private static WebDriver adminDriver;
    private static WebDriver requesterDriver;

    // The already-running default session becomes the admin session.
    public static void useAdmin() {
        if (adminDriver == null) adminDriver = DriverManager.getDriver();
        DriverManager.setDriver(adminDriver);
    }

    // A fresh, independent browser for the requester (own temp profile, logged out).
    public static void useRequester() {
        if (requesterDriver == null) requesterDriver = DriverManager.newDriver();
        DriverManager.setDriver(requesterDriver);
    }

    // Closes the requester browser and hands control back to the admin session.
    public static void closeRequester() {
        if (requesterDriver != null) {
            try { requesterDriver.quit(); } catch (Exception ignored) {}
            requesterDriver = null;
        }
        useAdmin();
    }

    public static boolean hasRequester() {
        return requesterDriver != null;
    }
}
