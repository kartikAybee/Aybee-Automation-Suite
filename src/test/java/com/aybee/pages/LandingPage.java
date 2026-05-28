package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class LandingPage extends BasePage {

    private final By signInToggle = By.id("toggle-sign-in");

    @Step("Navigate to Aybee platform landing page")
    public LandingPage navigateTo() {
        // Navigate to domain first so deleteAllCookies() is scoped to the correct origin.
        // Clearing cookies while on about:blank has no effect — cookies are domain-scoped.
        driver.get(ConfigReader.get("BASE_URL"));
        driver.manage().deleteAllCookies();
        // Reload after clearing so the page renders in a signed-out state.
        driver.navigate().refresh();
        wait.until(d -> isElementPresent(signInToggle));
        return this;
    }

    @Step("Click toggle-sign-in to switch to the login form")
    public SignInPage clickSignInToggle() {
        // jsClick() bypasses elementToBeClickable — Bubble.io toggle Text elements can have
        // width:0px, causing a regular click() to silently miss the hit target.
        jsClick(signInToggle);
        SignInPage page = new SignInPage();
        page.isLoaded();
        return page;
    }
}
