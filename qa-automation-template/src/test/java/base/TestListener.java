package base;

import factory.DriverFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.ScreenshotUtils;

/**
 * Cross-cutting concern: capture a screenshot whenever a test finishes,
 * whether it passed or failed. Kept separate from BaseTest so the
 * pass/fail hook logic doesn't clutter setup/teardown.
 */
public class TestListener implements ITestListener {

    @Override
    public void onTestSuccess(ITestResult result) {
        captureScreenshot(result, "PASS");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        captureScreenshot(result, "FAIL");
    }

    private void captureScreenshot(ITestResult result, String status) {
        try {
            String testCaseName = result.getMethod().getMethodName();
            String path = ScreenshotUtils.capture(DriverFactory.getDriver(), testCaseName, status);
            if (path != null) {
                System.out.println("[" + status + "] Screenshot saved: " + path);
            }
        } catch (IllegalStateException e) {
            // Driver was never initialized or already quit - nothing to capture
            System.err.println("Could not capture screenshot: " + e.getMessage());
        }
    }
}
