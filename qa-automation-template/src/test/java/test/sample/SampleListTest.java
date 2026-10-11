package test.sample;

import base.BaseTest;
import flows.LoginFlow;
import flows.sample.SampleListFlow;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * EXAMPLE TEST CLASS - demonstrates the assertion patterns from
 * CONVENTIONS.md on top of SampleListFlow. Delete this whole
 * pages.sample/flows.sample/test.sample trio once you've copied what
 * you need into your real tests.
 */
public class SampleListTest extends BaseTest {

    private SampleListFlow sampleListFlow;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void loginAsAdmin() {
        new LoginFlow(driver()).loginAs("admin");
        sampleListFlow = new SampleListFlow(driver());
    }

    // ============================================================
    // PATTERN 7: Before/after edit assertion
    // Read the value BEFORE editing, make the change, save, refresh
    // (many apps don't auto-update a list view without a reload), then
    // read again and compare. Printing both makes failures easy to
    // debug from the console without re-running in a debugger.
    // ============================================================
    @Test(groups = {"regression"}, description = "Validates a name change persists after saving")
    public void editRecord_changeName_shouldPersistAfterSave() {
        String recordId = "SAMPLE-001"; // an identifier known to exist in your test data
        String newName = "Updated Name";

        captureEvidence(); // "bukti awal" - the state before touching anything

        sampleListFlow.openEditPopup(recordId);
        sampleListFlow.inputEditName(newName);
        sampleListFlow.save();
        sampleListFlow.refreshPage();

        sampleListFlow.openEditPopup(recordId);
        captureEvidence("After Save"); // extra labelled evidence; PASS/FAIL shot still happens automatically
        String nameAfter = sampleListFlow.getEditNameValue();

        System.out.println("[editRecord] Record: " + recordId + " | Expected: " + newName + " | After: " + nameAfter);

        Assert.assertEquals(nameAfter, newName, "Expected the name change to persist after saving");
    }

    // ============================================================
    // PATTERN 8: Dual-path assertion for unconfirmed behavior
    // When you don't yet know whether validation disables a button OR
    // shows an error alert on click, check the button's state first and
    // handle whichever behavior is actually present - rather than
    // guessing and having the test fail for the wrong reason.
    // ============================================================
    @Test(groups = {"regression"}, description = "Validates a required field is enforced somehow (disabled button OR alert)")
    public void saveButton_withRequiredFieldEmpty_shouldPreventSaving() {
        String recordId = "SAMPLE-001";

        sampleListFlow.openEditPopup(recordId);
        sampleListFlow.inputEditName(""); // required field left empty on purpose

        if (sampleListFlow.isSaveButtonEnabled()) {
            sampleListFlow.save();
            String alertText = sampleListFlow.acceptAlertAndGetText();
            System.out.println("Behavior: validation alert on save | Alert: " + alertText);
            Assert.assertFalse(alertText.isEmpty(), "Expected a validation error when the required field is empty");
        } else {
            System.out.println("Behavior: Save button disabled");
            Assert.assertFalse(sampleListFlow.isSaveButtonEnabled(),
                    "Expected Save button to stay disabled when the required field is empty");
        }
    }

    // ============================================================
    // PATTERN 9: Popup-closed assertion (not popup-displayed)
    // waitForVisible() throws instead of returning false once an
    // element is gone - use a dedicated isXxxClosed() (backed by
    // waitForInvisible) for "did this actually close" assertions.
    // ============================================================
    @Test(groups = {"regression"}, description = "Validates the Close button closes the edit popup")
    public void closeEditPopup_shouldCloseWithoutSaving() {
        String recordId = "SAMPLE-001";

        sampleListFlow.openEditPopup(recordId);
        sampleListFlow.closeEditPopup();

        Assert.assertTrue(sampleListFlow.isEditPopupClosed(), "Expected the edit popup to close after clicking Close");
    }

    // ============================================================
    // PATTERN 10: Pagination navigation, verified via state change
    // (not "some rows exist") - Prev being enabled/disabled is a much
    // more meaningful signal that a page change actually happened than
    // just checking the table still has rows.
    // ============================================================
    @Test(groups = {"smoke"}, description = "Validates Next actually navigates to a new page")
    public void nextPageButton_shouldNavigateToNextPage() {
        sampleListFlow.goToFirstPage();
        Assert.assertTrue(sampleListFlow.isPrevPageDisabled(), "Precondition: should start on page 1");

        sampleListFlow.goToNextPage();

        Assert.assertFalse(sampleListFlow.isPrevPageDisabled(),
                "Expected to have moved off page 1 after clicking Next");
    }
}
