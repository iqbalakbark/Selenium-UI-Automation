package flows;

import config.ConfigReader;
import org.openqa.selenium.WebDriver;
import pages.LoginPage;

/**
 * Login business process, shared across all roles.
 * <p>
 * PATTERN TO KEEP: one shared LoginFlow for every role, with
 * loginAs(role) pulling that role's credentials from config.properties.
 * Avoids one LoginFlow per role for something that's otherwise identical.
 */
public class LoginFlow {

    private final LoginPage loginPage;

    public LoginFlow(WebDriver driver, int timeoutInSeconds) {
        this.loginPage = new LoginPage(driver, timeoutInSeconds);
    }

    /** Default timeout comes from config.properties. */
    public LoginFlow(WebDriver driver) {
        this.loginPage = new LoginPage(driver);
    }

    /**
     * @param role matches a "{role}.username" / "{role}.password" pair in config.properties, e.g. "admin"
     */
    public void loginAs(String role) {
        String username = ConfigReader.getUsername(role);
        String password = ConfigReader.getPassword(role);

        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        loginPage.clickLogin();
    }
}
