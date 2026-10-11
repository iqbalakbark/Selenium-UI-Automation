package utils;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * JS-based scrolling helpers - useful when a page/panel scrolls inside
 * its own container (not the whole document), which Selenium's native
 * scroll-into-view doesn't always handle well, or when you need to
 * force-scroll an element into view before interacting with it.
 */
public class ScrollUtils {

    private final WebDriver driver;

    public ScrollUtils(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Scrolls a specific scrollable container (identified by its HTML id)
     * to the bottom. Common in apps where the main content area is its
     * own scrolling div rather than the page body.
     *
     * @param containerId the container's HTML id, without "#"
     */
    public void scrollContainerToBottom(String containerId) {
        ((JavascriptExecutor) driver).executeScript(
                "const el = document.getElementById(arguments[0]);" +
                        "if (el) { el.scrollTop = el.scrollHeight; }",
                containerId);
    }

    public void scrollContainerToTop(String containerId) {
        ((JavascriptExecutor) driver).executeScript(
                "const el = document.getElementById(arguments[0]);" +
                        "if (el) { el.scrollTop = 0; }",
                containerId);
    }

    /** Scrolls the given element into view, centered in the viewport. */
    public void scrollElementIntoView(By locator) {
        WebElement element = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});", element);
    }
}
