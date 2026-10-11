package pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import utils.WaitUtils;

/**
 * Login page - shared across all roles (Admin/User/etc.), since it's
 * usually the same form regardless of who's logging in.
 * <p>
 * EXAMPLE FILE - replace the locators below with your app's real ones.
 * This is here to establish the pattern (see LoginFlow.java too): one
 * shared Page/Flow for login, with per-role credentials read from
 * config.properties via ConfigReader.getUsername(role)/getPassword(role).
 */
public class LoginPage {

    private final WebDriver driver;
    private final WaitUtils waitUtils;

    // TODO: replace with your app's real locators
    private final By usernameInput = By.name("username");
    private final By passwordInput = By.name("password");
    private final By loginButton = By.xpath("//button[normalize-space()='Log In']");
    private final By loginErrorMessage = By.cssSelector(".login-error-message");

    public LoginPage(WebDriver driver, int timeoutInSeconds) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver, timeoutInSeconds);
    }

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver);
    }

    public void enterUsername(String username) {
        waitUtils.waitForVisible(usernameInput).sendKeys(username);
    }

    public void enterPassword(String password) {
        waitUtils.waitForVisible(passwordInput).sendKeys(password);
    }

    public void clickLogin() {
        waitUtils.waitForClickable(loginButton).click();
    }

    public String getLoginErrorMessage() {
        return waitUtils.waitForVisible(loginErrorMessage).getText();
    }
}
