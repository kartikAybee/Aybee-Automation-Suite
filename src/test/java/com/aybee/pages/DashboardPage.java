package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class DashboardPage extends BasePage {

    // Profile icon appears in the nav bar after a successful login/activation
    private final By profileIcon         = By.id("profile-btn");
    private final By logoutButton        = By.id("logout-btn");
    private final By companyInfoTab      = By.id("company-info-tab");
    private final By currentCompanyField = By.id("current_company-field");

    @Step("Log out of the application")
    public SignUpPage logout() {
        jsClick(logoutButton);
        SignUpPage page = new SignUpPage();
        page.isLoaded();
        return page;
    }

    @Step("Open profile sidebar")
    public DashboardPage openProfileSidebar() {
        jsClick(profileIcon);
        return this;
    }

    @Step("Open Company Info tab in profile sidebar")
    public DashboardPage openCompanyInfoTab() {
        click(companyInfoTab);
        return this;
    }

    // Reads the company name stored in the user's profile.
    public String getCompanyName() {
        return getText(currentCompanyField);
    }

    // Convenience: opens the profile sidebar → company tab → returns stored company name.
    public String getLoggedInCompanyName() {
        return openProfileSidebar()
                .openCompanyInfoTab()
                .getCompanyName();
    }

    public boolean isLoaded() {
        return isElementVisible(profileIcon);
    }

    // Longer, explicit wait — used after actions where Bubble redirects to the dashboard on a
    // delay (e.g. a password reset), which can exceed the default 15s visibility wait.
    public boolean isLoaded(int timeoutSecs) {
        try {
            new WebDriverWait(driver, timeoutSecs)
                    .until(ExpectedConditions.visibilityOfElementLocated(profileIcon));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // getNotificationText() and isNotificationVisible() are inherited from BasePage
}
