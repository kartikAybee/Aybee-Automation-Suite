package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

// The company-selection popup shown after OTP activation for a NEW registration, BEFORE onboarding.
// The user either creates a company (create-company-btn) or requests to join an existing same-domain
// company. Dynamic IDs embed the company/user name, so they are matched via attribute-CSS / xpath
// (handles spaces and special characters that By.id cannot).
public class CompanySelectionPage extends BasePage {

    private final By createCompanyButton = By.id("create-company-btn");

    private static By byId(String id) {
        return By.cssSelector("[id='" + id + "']");
    }

    // The popup is present once the create-company button is on the page.
    public boolean isLoaded() {
        return isElementPresent(createCompanyButton);
    }

    @Step("Wait for the company-selection popup")
    public CompanySelectionPage waitUntilLoaded() {
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.presenceOfElementLocated(createCompanyButton));
        return this;
    }

    // Clicks Create Company. Bubble takes a moment to register the company and then redirects to the
    // onboarding questions — wait for onboarding_question1 so the caller can complete onboarding.
    @Step("Create a new company and wait for onboarding")
    public void createCompany() {
        jsClick(createCompanyButton);
        new WebDriverWait(driver, 30)
                .until(ExpectedConditions.presenceOfElementLocated(By.id("onboarding_question1")));
    }

    // ── Request-to-join surface ──────────────────────────────────────────────────

    // Scrolls the target company's request button into view and clicks it. The SAME button toggles
    // between "request" and "withdraw", so this is also how a pending request is withdrawn.
    @Step("Click request/withdraw for company '{companyName}'")
    public CompanySelectionPage clickRequestButton(String companyName) {
        By target = byId(companyName + "-request-btn");
        WebElement btn = scrollUntilPresent(target);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
        jsClick(target);
        return this;
    }

    // The company list (id "company-list") is long/lazy — the target button isn't in the DOM until
    // we scroll down to it. Poll for up to 30s: every 500ms scroll the list down a step and re-check
    // for the button. Simple and robust — it keeps scrolling (and lazy-loading) until the button
    // appears or the timeout elapses. Returns the target for centring + clicking.
    private WebElement scrollUntilPresent(By locator) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            java.util.List<WebElement> els = driver.findElements(locator);
            if (!els.isEmpty()) return els.get(0);
            js.executeScript(
                    "var l=document.getElementById('company-list');" +
                    "if(l){l.scrollBy(0,500);}else{window.scrollBy(0,500);}");
            try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        java.util.List<WebElement> els = driver.findElements(locator);
        if (!els.isEmpty()) return els.get(0);
        return new WebDriverWait(driver, 5)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public boolean isPending(String companyName) {
        return waitForPresent(byId(companyName + "-pending-status"), 20);
    }

    public boolean isPendingGone(String companyName) {
        return waitForAbsent(byId(companyName + "-pending-status"), 20);
    }

    // After a deny the requester's popup shows a rejected label; the request is terminal.
    public boolean isRejected(String companyName) {
        return waitForPresent(byId(companyName + "-rejected-status"), 20);
    }

    // The request button's second span carries the "Withdraw" text only while a request is pending;
    // it disappears once the request is withdrawn.
    private By withdrawSpan(String companyName) {
        return By.xpath("//button[@id='" + companyName + "-request-btn']/span[2]");
    }

    public boolean isWithdrawShown(String companyName) {
        return waitForPresent(withdrawSpan(companyName), 20);
    }

    public boolean isWithdrawGone(String companyName) {
        return waitForAbsent(withdrawSpan(companyName), 20);
    }

    private boolean waitForPresent(By locator, int secs) {
        try {
            new WebDriverWait(driver, secs).until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitForAbsent(By locator, int secs) {
        try {
            return new WebDriverWait(driver, secs)
                    .until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (Exception e) {
            return false;
        }
    }
}
