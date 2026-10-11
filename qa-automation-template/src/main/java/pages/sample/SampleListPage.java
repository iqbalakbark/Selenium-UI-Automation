package pages.sample;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.WaitUtils;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * EXAMPLE PAGE OBJECT - not meant to be used as-is. This demonstrates
 * the reusable patterns from CONVENTIONS.md on a generic "list + filter +
 * edit popup" page, which is a very common shape across CMS-style admin
 * panels (a table of records, a filter bar, and a modal to edit a row).
 * <p>
 * Delete this file (and pages.sample / flows.sample / test.sample as a
 * whole) once you've copied the patterns you need into your real Page
 * Objects - it's a reference, not a dependency.
 * <p>
 * NOTE: unlike your real Page Objects (where every method should end up
 * exercised by a test - see the dead-code check in CONVENTIONS.md),
 * several methods here exist purely to show a pattern's shape and are
 * intentionally NOT all wired into SampleListTest. That's fine here;
 * it would NOT be fine in your actual project.
 */
public class SampleListPage {

    private final WebDriver driver;
    private final WaitUtils waitUtils;

    public SampleListPage(WebDriver driver, int timeoutInSeconds) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver, timeoutInSeconds);
    }

    public SampleListPage(WebDriver driver) {
        this.driver = driver;
        this.waitUtils = new WaitUtils(driver);
    }

    // =========================================================
    // PATTERN 1: Custom dropdown (NOT a real <select>)
    // Many modern component libraries (react-select, MUI Select, custom
    // divs) render dropdowns that Selenium's Select class can't drive -
    // Select only works on genuine <select><option> HTML. Always check
    // dev tools first: if you don't see <option> tags, use this pattern
    // instead: click to open -> click the option matching the text.
    // =========================================================

    private final By categoryDropdown = By.xpath("//*[normalize-space()='Select Category']");

    public void selectCategory(String category) {
        waitUtils.clickSafely(categoryDropdown);
        By option = By.xpath("//div[normalize-space()='" + category + "']");
        waitUtils.clickSafely(option);
    }

    public List<String> getCategoryOptions() {
        waitUtils.clickSafely(categoryDropdown);
        // Async-loaded option lists often show a loading placeholder first -
        // wait for it to disappear before reading options, or you'll race it.
        waitUtils.waitForInvisible(By.xpath("//*[text()='Loading...']"));

        return driver.findElements(By.xpath("//div[contains(@class,'menu')]//div"))
                .stream()
                .map(WebElement::getText)
                .filter(text -> !text.isBlank())
                .distinct()
                .toList();
    }

    // =========================================================
    // PATTERN 2: Search-as-you-type field (async autocomplete)
    // e.g. react-select in async mode: typing triggers an API call, a
    // "Loading..." state, then a filtered option list - NOT a static
    // dropdown with everything pre-rendered. Type, wait for loading to
    // clear, then click the matching suggestion.
    // =========================================================

    private final By searchAssigneeInput = By.id("assignee-search");

    public void searchAndSelectAssignee(String keyword) {
        waitUtils.waitForVisible(searchAssigneeInput).sendKeys(keyword);
        waitUtils.waitForInvisible(By.xpath("//*[text()='Loading...']"));
        By matchingOption = By.xpath("//div[contains(@class,'option') and normalize-space()='" + keyword + "']");
        waitUtils.clickSafely(matchingOption);
    }

    // =========================================================
    // PATTERN 3: Native <input type="date">
    // Some date fields are the browser's OWN native date picker, not a
    // JS calendar widget (e.g. react-datepicker). You can tell by
    // inspecting: if clicking the field doesn't add any new element to
    // the DOM, it's native - Selenium can't (and shouldn't try to) drive
    // the native picker UI. Just set the value directly and verify via
    // getAttribute("value") afterward.
    // =========================================================

    private final By startDateInput = By.cssSelector("input[type='date']");

    public void inputStartDate(String date) {
        WebElement field = waitUtils.waitForVisible(startDateInput);
        field.clear();
        field.sendKeys(date);
    }

    // =========================================================
    // Filter actions
    // =========================================================

    private final By searchButton = By.xpath("//button[normalize-space()='Search']");
    private final By resetButton = By.xpath("//button[normalize-space()='Reset']");

    public void clickSearch() {
        waitUtils.clickSafely(searchButton);
    }

    public void clickReset() {
        waitUtils.clickSafely(resetButton);
    }

    // =========================================================
    // PATTERN 4: Table rows + row-scoped lookups
    // =========================================================

    private final By dataRows = By.cssSelector("table tbody tr");

    public List<WebElement> getDataRows() {
        waitUtils.waitForVisible(dataRows);
        return driver.findElements(dataRows);
    }

    /** Row-scoped: relative XPath (".//...") so it only searches WITHIN this row, not the whole document. */
    public String getStatusOf(WebElement row) {
        return row.findElement(By.cssSelector("td.status")).getText();
    }

    public void clickEditButton(WebElement row) {
        row.findElement(By.xpath(".//button[normalize-space()='Edit']")).click();
    }

    // =========================================================
    // PATTERN 5: Pagination with a stale-safe disabled check
    // React (and similar frameworks) re-render pagination controls
    // immediately after a Next/Prev click. Reading .getAttribute() a
    // moment too late grabs a reference to an already-replaced DOM node
    // -> StaleElementReferenceException, intermittently. Retry instead
    // of assuming the first read is safe.
    // =========================================================

    private final By nextPageButton = By.xpath("//button[normalize-space()='Next']");
    private final By prevPageButton = By.xpath("//button[normalize-space()='Prev']");

    public void clickNextPage() {
        waitUtils.clickSafely(nextPageButton);
    }

    public void clickPrevPage() {
        waitUtils.clickSafely(prevPageButton);
    }

    public boolean isNextPageDisabled() {
        return isButtonDisabled(nextPageButton);
    }

    public boolean isPrevPageDisabled() {
        return isButtonDisabled(prevPageButton);
    }

    private boolean isButtonDisabled(By locator) {
        int attempts = 0;
        while (true) {
            try {
                return waitUtils.waitForPresence(locator).getAttribute("disabled") != null;
            } catch (StaleElementReferenceException e) {
                attempts++;
                if (attempts >= 3) throw e;
            }
        }
    }

    // =========================================================
    // Popup - Edit
    // =========================================================

    private final By editPopupContainer = By.xpath("//div[contains(@class,'modal')]");
    private final By editNameInput = By.id("edit-name");
    private final By editSaveButton = By.xpath("//button[normalize-space()='Save']");
    private final By editCloseButton = By.xpath("//div[contains(@class,'modal')]//button[contains(@class,'close')]");

    public boolean isEditPopupDisplayed() {
        return waitUtils.waitForVisible(editPopupContainer).isDisplayed();
    }

    /**
     * Use this (not isEditPopupDisplayed) to confirm the popup has
     * CLOSED - waitForVisible would throw a TimeoutException instead of
     * returning false once the element is gone, which isn't useful for
     * a "did it close" assertion.
     */
    public boolean isEditPopupClosed() {
        return waitUtils.waitForInvisible(editPopupContainer);
    }

    public void inputEditName(String name) {
        WebElement field = waitUtils.waitForVisible(editNameInput);
        field.clear();
        field.sendKeys(name);
    }

    public String getEditNameValue() {
        return waitUtils.waitForVisible(editNameInput).getAttribute("value");
    }

    public void clickSave() {
        waitUtils.clickSafely(editSaveButton);
    }

    public boolean isSaveButtonEnabled() {
        return waitUtils.waitForPresence(editSaveButton).isEnabled();
    }

    public void closeEditPopup() {
        waitUtils.clickSafely(editCloseButton);
    }

    // =========================================================
    // Native browser alert
    // =========================================================

    public String acceptAlertAndGetText() {
        return waitUtils.acceptAlertAndGetText();
    }

    public void refreshPage() {
        driver.navigate().refresh();
    }
}
