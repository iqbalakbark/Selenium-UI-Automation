package flows;

import config.ConfigReader;
import org.openqa.selenium.WebDriver;
import pages.LoginPage;


public class LoginFlow {

    private final LoginPage loginPage;

    public LoginFlow(WebDriver driver, int timeoutInSeconds) {
        this.loginPage = new LoginPage(driver, timeoutInSeconds);
    }

    public LoginFlow(WebDriver driver) {
        this.loginPage = new LoginPage(driver);
    }

    public void loginAs(String role) {
        String username = ConfigReader.getUsername(role);
        String password = ConfigReader.getPassword(role);

        loginPage.enterUsername(username);
        loginPage.enterPassword(password);
        loginPage.clickLogin();
    }

    public String getLoginErrorMessages() {
        return loginPage.getLoginErrorMessage();
    }

    public void logOut () {
        loginPage.clickBurgerMenu();
        loginPage.clickLogoutBtn();
    }

    public String acceptAlertAndGetText() {
        return loginPage.acceptAlertAndGetText();
    }
}
