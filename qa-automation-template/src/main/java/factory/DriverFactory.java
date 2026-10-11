package factory;

import config.ConfigReader;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the WebDriver lifecycle (create / fetch / quit).
 * <p>
 * Uses ThreadLocal so this safely supports parallel test execution later
 * without any changes needed here.
 * <p>
 * Browser is picked at runtime from config.properties ("browser=chrome/
 * edge/firefox") rather than hardcoded - swap browsers without touching
 * code. Selenium 4.6+'s built-in Selenium Manager auto-resolves the
 * correct driver binary, so no WebDriverManager dependency or manual
 * driver .exe placement is needed.
 */
public class DriverFactory {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private DriverFactory() {
        // utility class, no instantiation
    }

    public static void initDriver() {
        // Quiet down Selenium's noisy default logging
        Logger.getLogger("org.openqa.selenium").setLevel(Level.SEVERE);

        WebDriver driver = createDriver(ConfigReader.getBrowser());

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(ConfigReader.getImplicitWait()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigReader.getPageLoadTimeout()));
        driver.manage().window().maximize();

        driverThreadLocal.set(driver);
    }

    private static WebDriver createDriver(String browser) {
        return switch (browser.toLowerCase()) {
            case "chrome" -> {
                ChromeOptions options = new ChromeOptions();
                // Useful on DEV/UAT environments with self-signed certs -
                // remove if your environment doesn't need it.
                options.setAcceptInsecureCerts(true);
                // options.addArguments("--headless=new"); // uncomment for headless/CI runs
                yield new ChromeDriver(options);
            }
            case "edge" -> {
                EdgeOptions options = new EdgeOptions();
                options.setAcceptInsecureCerts(true);
                options.addArguments("--ignore-certificate-errors", "--allow-insecure-localhost");
                // options.addArguments("--headless=new");
                yield new EdgeDriver(options);
            }
            case "firefox" -> {
                FirefoxOptions options = new FirefoxOptions();
                options.setAcceptInsecureCerts(true);
                // options.addArguments("--headless");
                yield new FirefoxDriver(options);
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported browser: '" + browser + "'. Use chrome, edge, or firefox.");
        };
    }

    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException("Driver has not been initialized. Call initDriver() first.");
        }
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                System.err.println("Failed to quit driver: " + e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }
}
