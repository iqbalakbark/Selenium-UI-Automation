package utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures screenshots and stores them as:
 * <pre>
 * test-output/screenshots/
 * └── yyyy-MM-dd/
 *     └── TestCaseName/
 *         ├── yyyyMMdd_HHmmss_PASS_or_FAIL_TestCaseName.png          (automatic, via TestListener)
 *         └── yyyyMMdd_HHmmss_SSS_EVIDENCE_Label_TestCaseName.png    (on demand, via BaseTest.captureEvidence())
 * </pre>
 * PASS/FAIL shots are wired up through TestListener - nothing to call
 * from inside a test. EVIDENCE shots are the opposite: you decide when
 * to take them (e.g. the initial state before an action) by calling
 * captureEvidence() from your test, and they land in the same folder as
 * that test's PASS/FAIL shot so everything for one test sits together.
 */
public class ScreenshotUtils {

    private static final String ROOT_FOLDER = "test-output/screenshots";
    private static final DateTimeFormatter DATE_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    // Milliseconds included so two evidence shots taken within the same
    // second (same label, even) can't collide on the file name.
    private static final DateTimeFormatter EVIDENCE_FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotUtils() {
        // utility class, no instantiation
    }

    /**
     * @param driver       active WebDriver instance
     * @param testCaseName usually the test method name
     * @param status       "PASS" or "FAIL"
     * @return absolute path of the saved screenshot, or null if capture failed
     */
    public static String capture(WebDriver driver, String testCaseName, String status) {
        String sanitizedTestName = sanitize(testCaseName);
        String timestamp = LocalDateTime.now().format(FILE_NAME_FORMAT);
        String fileName = String.format("%s_%s_%s.png", timestamp, status.toUpperCase(), sanitizedTestName);
        return save(driver, sanitizedTestName, fileName, testCaseName);
    }

    /**
     * Takes an on-demand "evidence" screenshot (e.g. the initial state
     * before an action), saved next to the test's PASS/FAIL shot.
     *
     * @param driver       active WebDriver instance
     * @param testCaseName usually the test method name
     * @param label        short description, e.g. "Initial" or "Before Edit" - becomes part of the file name
     * @return absolute path of the saved screenshot, or null if capture failed
     */
    public static String captureEvidence(WebDriver driver, String testCaseName, String label) {
        String sanitizedTestName = sanitize(testCaseName);
        String timestamp = LocalDateTime.now().format(EVIDENCE_FILE_NAME_FORMAT);
        String fileName = String.format("%s_EVIDENCE_%s_%s.png", timestamp, sanitize(label), sanitizedTestName);
        return save(driver, sanitizedTestName, fileName, testCaseName);
    }

    private static String save(WebDriver driver, String sanitizedTestName, String fileName, String testCaseNameForLog) {
        try {
            String dateFolder = LocalDate.now().format(DATE_FOLDER_FORMAT);

            Path targetDir = Paths.get(ROOT_FOLDER, dateFolder, sanitizedTestName);
            Files.createDirectories(targetDir);

            File source = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Path targetFile = targetDir.resolve(fileName);
            Files.copy(source.toPath(), targetFile);

            return targetFile.toAbsolutePath().toString();
        } catch (IOException e) {
            System.err.println("Failed to capture screenshot for [" + testCaseNameForLog + "]: " + e.getMessage());
            return null;
        }
    }

    private static String sanitize(String rawName) {
        return rawName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
