package flows.sample;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import pages.sample.SampleListPage;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * EXAMPLE FLOW - see SampleListPage's class comment. Business logic
 * ("which row am I acting on") lives here, not in the Page - the Page
 * only knows how to read/click elements once a row is already found.
 */
public class SampleListFlow {

    private final SampleListPage sampleListPage;

    public SampleListFlow(WebDriver driver, int timeoutInSeconds) {
        this.sampleListPage = new SampleListPage(driver, timeoutInSeconds);
    }

    public SampleListFlow(WebDriver driver) {
        this.sampleListPage = new SampleListPage(driver);
    }

    public void filterByCategory(String category) {
        sampleListPage.selectCategory(category);
        sampleListPage.clickSearch();
    }

    public void resetFilter() {
        sampleListPage.clickReset();
    }

    // =========================================================
    // PATTERN 6: Find a row by a business identifier (String), NEVER
    // hold onto the WebElement itself across steps.
    // <p>
    // Why: a WebElement reference goes stale the moment the DOM
    // re-renders (a click, a filter, a save - anything). If you store
    // "the row" as a variable and use it again several steps later,
    // you're gambling on nothing having re-rendered in between. Storing
    // the identifier (an ID, a name, a reference number) and re-querying
    // the DOM every time you need it is slower per-call but immune to
    // staleness - always re-fetch fresh.
    // <p>
    // This also searches page by page (via goToFirstPage() + Next),
    // since the row you want might not be on whatever page happens to
    // be showing.
    // =========================================================

    public WebElement findRowById(String identifier) {
        goToFirstPage();

        while (true) {
            try {
                return sampleListPage.getDataRows().stream()
                        .filter(row -> row.getText().contains(identifier))
                        .findFirst()
                        .orElseThrow();
            } catch (NoSuchElementException e) {
                if (sampleListPage.isNextPageDisabled()) {
                    throw new NoSuchElementException("No row found with identifier: " + identifier);
                }
                sampleListPage.clickNextPage();
            }
        }
    }

    /** No "go to page 1" control usually exists in the UI - walk back via Prev until it's disabled. */
    public void goToFirstPage() {
        while (!sampleListPage.isPrevPageDisabled()) {
            sampleListPage.clickPrevPage();
        }
    }

    public void goToNextPage() {
        sampleListPage.clickNextPage();
    }

    public void goToPrevPage() {
        sampleListPage.clickPrevPage();
    }

    public boolean isNextPageDisabled() {
        return sampleListPage.isNextPageDisabled();
    }

    public boolean isPrevPageDisabled() {
        return sampleListPage.isPrevPageDisabled();
    }

    public String getStatusOf(String identifier) {
        return sampleListPage.getStatusOf(findRowById(identifier));
    }

    public void openEditPopup(String identifier) {
        sampleListPage.clickEditButton(findRowById(identifier));
    }

    public boolean isEditPopupDisplayed() {
        return sampleListPage.isEditPopupDisplayed();
    }

    public boolean isEditPopupClosed() {
        return sampleListPage.isEditPopupClosed();
    }

    public void inputEditName(String name) {
        sampleListPage.inputEditName(name);
    }

    public String getEditNameValue() {
        return sampleListPage.getEditNameValue();
    }

    public void save() {
        sampleListPage.clickSave();
    }

    public boolean isSaveButtonEnabled() {
        return sampleListPage.isSaveButtonEnabled();
    }

    public void closeEditPopup() {
        sampleListPage.closeEditPopup();
    }

    public String acceptAlertAndGetText() {
        return sampleListPage.acceptAlertAndGetText();
    }

    public void refreshPage() {
        sampleListPage.refreshPage();
    }
}
