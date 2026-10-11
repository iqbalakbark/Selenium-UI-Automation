package base;

import config.ConfigReader;
import factory.DriverFactory;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import utils.ScreenshotUtils;

import java.lang.reflect.Method;

/**
 * Every Test class extends this. Keeps driver lifecycle in one place so
 * individual test classes stay focused on scenarios, not plumbing.
 */
@Listeners(TestListener.class)
public class BaseTest {

    // ThreadLocal, same reasoning as DriverFactory: safe if tests ever run in parallel.
    private static final ThreadLocal<String> currentTestName = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void setUp(Method method) {
        // TestNG injects the Method being run - remembered here so
        // captureEvidence() can file screenshots under the right test
        // name without every test having to pass its own name in.
        currentTestName.set(method.getName());

        DriverFactory.initDriver();
        DriverFactory.getDriver().get(ConfigReader.getBaseUrl());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        // alwaysRun = true: the screenshot listener still needs the driver
        // alive when a test fails, so quit happens only after that hook runs.
        DriverFactory.quitDriver();
        currentTestName.remove();
    }

    /** Convenience accessor so Test classes don't call DriverFactory directly. */
    protected WebDriver driver() {
        return DriverFactory.getDriver();
    }

    /** Takes an evidence screenshot labelled "Initial" - the common "bukti awal" case. */
    protected void captureEvidence() {
        captureEvidence("Initial");
    }

    /**
     * Takes an on-demand evidence screenshot, filed next to this test's
     * automatic PASS/FAIL shot (see ScreenshotUtils for the folder layout).
     * <p>
     * Never fails the test: if the screenshot can't be taken, a warning is
     * printed and the test carries on - evidence is supporting material,
     * not something worth turning a passing test red over.
     * <p>
     * Call it BEFORE an action that opens a native alert: while a native
     * alert is open, the browser refuses screenshots.
     *
     * @param label short description, e.g. "Before Edit" - becomes part of the file name
     */
    protected void captureEvidence(String label) {
        try {
            String testName = currentTestName.get() != null ? currentTestName.get() : "UnknownTest";
            String path = ScreenshotUtils.captureEvidence(driver(), testName, label);
            if (path != null) {
                System.out.println("[EVIDENCE] " + label + " -> " + path);
            }
        } catch (RuntimeException e) {
            System.err.println("Could not capture evidence '" + label + "': " + e.getMessage());
        }
    }
}
