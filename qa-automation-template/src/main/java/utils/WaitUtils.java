package utils;

import config.ConfigReader;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Centralizes all explicit-wait logic. No Thread.sleep anywhere in the
 * framework - every wait here is condition-based via ExpectedConditions.
 */
public class WaitUtils {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public WaitUtils(WebDriver driver, int timeoutInSeconds) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutInSeconds));
    }

    /** Default timeout comes from config.properties (explicit.wait). */
    public WaitUtils(WebDriver driver) {
        this(driver, ConfigReader.getExplicitWait());
    }

    public WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement waitForPresence(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Click with a small retry loop against StaleElementReferenceException.
     * Common on any app where content re-renders (React/Vue/etc.) right
     * as Selenium tries to interact with it - a plain click can grab a
     * reference a moment before the DOM node gets replaced.
     */
    public void clickSafely(By locator) {
        int attempts = 0;
        StaleElementReferenceException lastException = null;

        while (attempts < 3) {
            try {
                waitForClickable(locator).click();
                return;
            } catch (StaleElementReferenceException e) {
                lastException = e;
                attempts++;
            }
        }
        throw new RuntimeException("Element still stale after 3 retries: " + locator, lastException);
    }

    /**
     * Some apps use native browser alerts (window.alert) for validation
     * errors instead of an inline toast/message. Selenium must switch to
     * Alert for these - a regular findElement() won't see them.
     */
    public Alert waitForAlert() {
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    public String acceptAlertAndGetText() {
        Alert alert = waitForAlert();
        String text = alert.getText();
        alert.accept();
        return text;
    }
}
