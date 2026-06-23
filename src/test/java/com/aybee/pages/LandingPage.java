package com.aybee.pages;

import com.aybee.utils.ConfigReader;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class LandingPage extends BasePage {

    private final By signInToggle = By.id("toggle-sign-in");

    @Step("Navigate to Aybee platform landing page")
    public LandingPage navigateTo() {
        driver.get(ConfigReader.get("BASE_URL"));
        driver.manage().deleteAllCookies();
        driver.navigate().refresh();
        wait.until(d -> isElementPresent(signInToggle));
        return this;
    }

    @Step("Click toggle-sign-in to switch to the login form")
    public SignInPage clickSignInToggle() {
        jsClick(signInToggle);
        SignInPage page = new SignInPage();
        page.isLoaded();
        return page;
    }
}
