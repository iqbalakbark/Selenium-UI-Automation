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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Owns the WebDriver lifecycle (create / fetch / quit).
 *
 * Uses ThreadLocal so this safely supports parallel test execution later
 * without any changes needed here.
 *
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
                options.setAcceptInsecureCerts(true);

                // 1. Matikan Password Manager via Preferences
                Map<String, Object> prefs = new HashMap<>();
                prefs.put("credentials_enable_service", false);
                prefs.put("profile.password_manager_enabled", false);
                prefs.put("profile.password_manager_leak_detection", false); // Matikan khusus deteksi breach
                options.setExperimentalOption("prefs", prefs);

                // 2. Argument Tambahan untuk Mematikan Feature & Bubble Prompt Chrome
                options.addArguments("--disable-save-password-bubble");
                options.addArguments("--disable-notifications");
                options.addArguments("--disable-popup-blocking");

                // Matikan fitur Password Leak Detection & Credential Provider di tingkat browser Engine
                options.addArguments("--disable-features=PasswordManager,PasswordLeakDetection,PasswordCheck");

                // Sembunyikan pesan "Chrome is being controlled by automated test software"
                options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation", "enable-logging"});

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