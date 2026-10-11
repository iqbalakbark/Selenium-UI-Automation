package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads config.properties once and exposes typed getters.
 * Kept as a simple static-holder singleton - no need for a DI framework
 * for a single flat properties file.
 */
public class ConfigReader {

    private static final String CONFIG_PATH = "config.properties";
    private static Properties properties;

    private ConfigReader() {
        // utility class, no instantiation
    }

    private static Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            try (FileInputStream fis = new FileInputStream(CONFIG_PATH)) {
                properties.load(fis);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load " + CONFIG_PATH +
                        ". Copy config.properties.example to config.properties and fill it in.", e);
            }
        }
        return properties;
    }

    public static String get(String key) {
        String value = getProperties().getProperty(key);
        if (value == null) {
            throw new RuntimeException("Missing config key: " + key);
        }
        return value.trim();
    }

    public static String getBaseUrl() {
        return get("base.url");
    }

    public static String getBrowser() {
        return get("browser");
    }

    public static int getImplicitWait() {
        return Integer.parseInt(get("implicit.wait"));
    }

    public static int getExplicitWait() {
        return Integer.parseInt(get("explicit.wait"));
    }

    public static int getPageLoadTimeout() {
        return Integer.parseInt(get("page.load.timeout"));
    }

    /**
     * Convenience accessor for role-based credentials,
     * e.g. ConfigReader.getUsername("admin") reads "admin.username"
     */
    public static String getUsername(String role) {
        return get(role.toLowerCase() + ".username");
    }

    public static String getPassword(String role) {
        return get(role.toLowerCase() + ".password");
    }
}
