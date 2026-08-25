package com.aybee.pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class TeamPage extends BasePage {

    private final By teamNavButton    = By.id("navigator_team_button");
    private final By inviteSidebarBtn = By.id("invite-sidebar-trigger");
    private final By emailField       = By.id("invite-email-field");
    private final By firstNameField   = By.id("invite-firstname-field");
    private final By lastNameField    = By.id("invite-lastname-field");
    private final By userTypeDropdown = By.id("invite-usertype-dropdown");
    private final By inviteButton     = By.id("manual-invite-trigger");
    private final By copyLinkButton   = By.id("share-invite-url");

    @Step("Navigate to Team tab")
    public TeamPage navigateToTeamTab() {
        jsClick(teamNavButton);
        // Wait for the invite sidebar trigger to appear — confirms team tab content is loaded.
        wait.until(ExpectedConditions.presenceOfElementLocated(inviteSidebarBtn));
        return this;
    }

    @Step("Open invite sidebar")
    public TeamPage openInviteSidebar() {
        jsClick(inviteSidebarBtn);
        // Wait for the email field to become visible — confirms the sidebar is fully open.
        wait.until(ExpectedConditions.visibilityOfElementLocated(emailField));
        return this;
    }

    // role: "admin" | "user" (creator) | "viewer"
    @Step("Send email invite to {email}")
    public TeamPage sendEmailInvite(String email, String firstName, String lastName, String role) {
        type(emailField, email);
        type(firstNameField, firstName);
        type(lastNameField, lastName);
        selectUserType(role);
        click(inviteButton);
        return this;
    }

    @Step("Copy team invite link to clipboard and return URL")
    public String copyInviteLinkAndRead() {
        wait.until(ExpectedConditions.elementToBeClickable(copyLinkButton));
        dismissToastIfPresent();
        click(copyLinkButton);

        // Wait for Bubble.io's "Copied" confirmation toast — natural sync point for the
        // clipboard write completing. Falls through if no toast appears within 5 s.
        try {
            new WebDriverWait(driver, 5)
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("toast-animate-in")));
        } catch (Exception ignored) {}

        // Read whatever Bubble.io just wrote. clipboard-read permission is pre-granted
        // for this origin in BrowserSetup prefs so readText() resolves without a prompt.
        Object url = ((JavascriptExecutor) driver).executeAsyncScript(
                "var done = arguments[arguments.length - 1];" +
                "navigator.clipboard.readText().then(done).catch(function() { done(null); });");

        if (url instanceof String && ((String) url).startsWith("http")) {
            System.out.println("[TeamPage] invite URL: " + url);
            return (String) url;
        }
        throw new RuntimeException("Copy link did not produce a valid URL. Got: " + url);
    }

    public boolean isLoaded() {
        return isElementVisible(teamNavButton);
    }

    // ── Join-request approval (creator/admin only) ───────────────────────────────
    // A pending join request renders per-user approve/decline controls on the Team panel:
    //   approve  → id "<NAME>-approve-user"   decline → id "<NAME>-decline-user"
    // <NAME> is the requester's uppercased "First Last". The id sits on a DIV overlaying the
    // button (not the button itself), so we target the div directly by xpath — a bare [id=...]
    // could otherwise match a co-id'd, non-clickable button.
    private static By approveDiv(String userName) {
        return By.xpath("//div[@id='" + userName + "-approve-user']");
    }

    private static By declineDiv(String userName) {
        return By.xpath("//div[@id='" + userName + "-decline-user']");
    }

    public boolean waitForRequestActions(String userName, int secs) {
        try {
            new WebDriverWait(driver, secs).until(
                    ExpectedConditions.presenceOfElementLocated(approveDiv(userName)));
            return isElementPresent(declineDiv(userName));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean waitForRequestActionsGone(String userName, int secs) {
        try {
            return new WebDriverWait(driver, secs).until(
                    ExpectedConditions.invisibilityOfElementLocated(approveDiv(userName)));
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Approve join request from {userName}")
    public TeamPage approveUser(String userName) {
        jsClick(approveDiv(userName));
        return this;
    }

    @Step("Decline join request from {userName}")
    public TeamPage declineUser(String userName) {
        jsClick(declineDiv(userName));
        return this;
    }

    // Reload clears any ghost ids Bubble leaves behind after a withdraw/decline before we
    // re-check the approve/decline controls.
    @Step("Reload the Team panel")
    public TeamPage reloadTeamPanel() {
        driver.navigate().refresh();
        navigateToTeamTab();
        return this;
    }

    // Option values are &quot;admin&quot; / &quot;user&quot; / &quot;viewer&quot; in HTML.
    // Match by includes() so quote-encoding differences never cause a mismatch.
    private void selectUserType(String role) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(userTypeDropdown));
        ((JavascriptExecutor) driver).executeScript(
                "var s = arguments[0], r = arguments[1];" +
                "for (var i = 0; i < s.options.length; i++) {" +
                "  if (s.options[i].value.includes(r)) {" +
                "    s.selectedIndex = i;" +
                "    s.dispatchEvent(new Event('change', {bubbles: true}));" +
                "    break;" +
                "  }" +
                "}", element, role);
    }
}
