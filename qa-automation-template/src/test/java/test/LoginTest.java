package test;

import base.BaseTest;
import flows.LoginFlow;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * EXAMPLE TEST - replace the assertion with something real once your
 * app's post-login page is known (a dashboard heading, a URL fragment,
 * a specific element only visible when logged in, etc.).
 */
public class LoginTest extends BaseTest {

    @Test(groups = {"smoke"}, description = "Validates login succeeds and redirects away from the login page")
    public void login_withValidCredentials_shouldRedirectToDashboard() {
        new LoginFlow(driver()).loginAs("admin");

        Assert.assertFalse(driver().getCurrentUrl().contains("login"),
                "Expected to be redirected away from the login page after a successful login");
    }
}
