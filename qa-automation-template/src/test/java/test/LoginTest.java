package test;

import base.BaseTest;
import flows.LoginFlow;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class LoginTest extends BaseTest {

    private LoginFlow loginFlow;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void loginAsAdmin() {
        loginFlow = new LoginFlow(driver());
    }

    @Test(groups = {"smoke"}, description = "TS01: Validates login succeeds and redirects away from the login page")
    public void login_withValidCredentials_shouldRedirectToDashboard() {
        new LoginFlow(driver()).loginAs("admin");

        Assert.assertFalse(driver().getCurrentUrl().contains("login"),
                "Expected to be redirected away from the login page after a successful login");
    }
    @Test(groups = {"smoke"}, description = "TS02: Login Failed with Invalid Password")
    public void login_withInvalidPasswordLogin() {
        new LoginFlow(driver()).loginAs("wrongpassword");
        Assert.assertTrue(loginFlow.getLoginErrorMessages().contains("Username and password do not match"));
    }


    @Test(groups = {"smoke"}, description = "TS03:Login Failed with Locked Out User")
    public void login_withInvalidCredentials_shouldgeterrormessages() {
        new LoginFlow(driver()).loginAs("invalid");

        Assert.assertTrue(loginFlow.getLoginErrorMessages().contains("Sorry, this user has been locked out."));
    }

    @Test(groups = {"smoke"}, description = "TS04: Successful Logout")
    public void logOut_Successful() {
        new LoginFlow(driver()).loginAs("admin");
        loginFlow.logOut();

        Assert.assertTrue(driver().getCurrentUrl().contains("saucedemo.com"));    }


}
