# Conventions

This document explains **why** this framework is built the way it is.
The code shows *what* to do; this file explains *why*, so the reasoning
doesn't get lost or silently violated as the project grows. Read it once
before writing your first real Page Object.

Every pattern below was learned the hard way, from real bugs on a real
project - not from a textbook.

---

## 1. Explicit Wait only - never `Thread.sleep`

All waiting goes through `WaitUtils`, which wraps Selenium's
`WebDriverWait` + `ExpectedConditions`. `Thread.sleep` always waits the
full duration even when the element appeared instantly, and it's not
long enough on a slow day - explicit wait solves both problems at once.

## 2. Stale elements: retry, don't assume one read is safe

**The bug this prevents:** frameworks like React re-render parts of the
DOM the instant something changes (a click, a filter, a save). If you
read an element's state (`.getAttribute()`, `.getText()`, `.isEnabled()`)
a few milliseconds after triggering that change, you can grab a
reference to a DOM node that's already been replaced -
`StaleElementReferenceException`, intermittently, hard to reproduce.

This shows up most on **pagination controls**, since clicking Next/Prev
immediately re-renders the pagination bar:

```java
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
```

`WaitUtils.clickSafely()` already does this for clicks. If you write a
new method that *reads* an attribute/text right after an action that
changes the DOM, consider whether it needs the same retry treatment.

## 3. Row lookup: identify by data, not by `WebElement`

**Never** store a `WebElement` in a variable and reuse it several steps
later. The moment the DOM re-renders - anywhere - that reference is
dead. Instead, store a **business identifier** (a reference number, a
name, an ID) and re-query the DOM every time you need "that row":

```java
// BAD - row can go stale before you use it again
WebElement row = findRowByFormRefNo(refNo);
clickUpdate(row);
// ... some other action happens, DOM re-renders ...
getStatus(row); // boom, StaleElementReferenceException

// GOOD - always re-fetch fresh
String refNo = "SAMPLE-001";
openEditPopup(refNo);   // internally re-finds the row
getStatusOf(refNo);     // internally re-finds the row again
```

Yes, this means scanning the table again on every call. That's a
worthwhile trade: a bit slower, but immune to staleness bugs that are
maddening to debug because they're intermittent.

This also means row-lookup methods should search **page by page**
(walk back to page 1 first, then click Next until found or until the
last page), since the row you want might not be on whatever page
happens to be showing.

## 4. Custom dropdowns vs real `<select>`

Selenium's `Select` class **only** works on genuine `<select><option>`
HTML. A lot of modern component libraries (react-select, MUI, custom
styled divs) render something that *looks* like a dropdown but is a
`<div>` tree underneath - `Select` will silently fail or throw.

**Always check dev tools first.** If you don't see real `<option>`
elements, use the click-to-open-then-click-the-option-text pattern
instead:

```java
public void selectCategory(String category) {
    waitUtils.clickSafely(categoryDropdown);
    By option = By.xpath("//div[normalize-space()='" + category + "']");
    waitUtils.clickSafely(option);
}
```

## 5. Search-as-you-type fields (async autocomplete)

Fields like react-select's async mode don't have their full option list
in the DOM upfront - typing triggers an API call, shows a loading state,
then renders matching results. Don't assume the option exists
immediately after `sendKeys()`:

```java
public void searchAndSelectAssignee(String keyword) {
    waitUtils.waitForVisible(searchInput).sendKeys(keyword);
    waitUtils.waitForInvisible(By.xpath("//*[text()='Loading...']"));
    By matchingOption = By.xpath("//div[contains(@class,'option') and normalize-space()='" + keyword + "']");
    waitUtils.clickSafely(matchingOption);
}
```

## 6. Native `<input type="date">` vs JS calendar widgets

Two different things that look similar:

- **JS widget** (e.g. react-datepicker): clicking the input adds a new
  calendar element to the DOM that you *can* click through
  (`.react-datepicker__day` cells etc.).
- **Native browser date input**: clicking it opens the browser's own
  OS-level picker UI, which is **not part of the DOM** and Selenium
  cannot (and should not try to) interact with it.

Check dev tools: if clicking the field adds no new element, it's native.
For native inputs, just set the value directly and verify via
`getAttribute("value")` (which normalizes to `yyyy-MM-dd` regardless of
what format you typed).

## 7. Native browser alerts (`window.alert`)

Some validation errors use the browser's native `alert()`, not an inline
toast/message div. A plain `findElement()` won't see these - you must
switch to `Alert`:

```java
public String acceptAlertAndGetText() {
    Alert alert = wait.until(ExpectedConditions.alertIsPresent());
    String text = alert.getText();
    alert.accept();
    return text;
}
```

If a test hangs or throws `UnhandledAlertException` on an action you
didn't expect to trigger a popup, check whether the app is using a
native alert there.

## 8. "Is it displayed" vs "Is it closed" need different methods

```java
public boolean isPopupDisplayed() {
    return waitUtils.waitForVisible(popup).isDisplayed();
}

public boolean isPopupClosed() {
    return waitUtils.waitForInvisible(popup); // NOT waitForVisible + check false
}
```

`waitForVisible` **throws** `TimeoutException` if the element never
becomes visible - it doesn't return `false`. Once a popup has actually
closed and left the DOM, calling `waitForVisible` on it will just hang
until timeout and then fail with the wrong exception. Use a dedicated
`isXxxClosed()` backed by `waitForInvisible` (which returns a real
`boolean`) for any "did this actually go away" assertion.

## 9. Dual-path assertions for unconfirmed behavior

Sometimes you genuinely don't know, ahead of time, whether the app
disables a button on invalid input **or** lets you click it and shows a
validation alert. Rather than guessing (and having the test fail for the
wrong reason when you guessed wrong), check the state and branch:

```java
if (flow.isSaveButtonEnabled()) {
    flow.save();
    String alertText = flow.acceptAlertAndGetText();
    Assert.assertFalse(alertText.isEmpty(), "Expected a validation error");
} else {
    Assert.assertFalse(flow.isSaveButtonEnabled(), "Expected Save to stay disabled");
}
```

Once you've actually run the test once and seen which branch fires,
feel free to simplify down to the confirmed path - this pattern is a
starting point for unknowns, not a permanent crutch.

## 10. Before/after assertions for edits

When testing "does an edit persist", read the value **before**,
make the change, save, **refresh the page** (many apps don't
auto-refresh a list view after a modal closes), then read again and
compare - printing both values so a failure is debuggable straight from
the console log, no debugger attach needed:

```java
String before = flow.getFieldValue(id);
flow.openEditPopup(id);
flow.changeField("new value");
flow.save();
flow.refreshPage();

String after = flow.getFieldValue(id);
System.out.println("[testName] Before: " + before + " | After: " + after);
Assert.assertEquals(after, "new value");
```

## 11. Page Object vs Flow layer - what goes where

- **Page** (`pages/`): locators + raw element interaction only. No
  business logic, no "which row/which page" decisions.
- **Flow** (`flows/`): business processes. This is where "find the row
  with this ID", "fill these 5 fields then submit", "loop until the last
  page" logic lives. A Flow method should read like a sentence describing
  what a user does, not how Selenium does it.
- **Test**: scenario + assertion only. No locators, no raw Selenium
  calls - everything goes through a Flow.

If you're tempted to write `driver.findElement(...)` inside a Test or a
Flow, that's a signal the locator belongs in a Page method instead.

## 12. Role separation: separate files, not `if (role == ...)`

If your app has multiple roles with different permissions/pages
(Admin, standard User, etc.), give each role **its own Page/Flow/Test
files**, even when they're 90% identical - `pages/admin/XyzPage.java`
and `pages/requestor/XyzPage.java`, not one shared file with role
conditionals scattered through it.

This is safe in Java: two classes with the same simple name in different
packages never collide, as long as no single file imports both. Keeping
them separate means a change for one role can never accidentally break
another, and each file stays readable without tracking which branches
apply to which role.

## 13. Config-driven, not hardcoded

Base URL, browser, timeouts, and credentials all come from
`config.properties` via `ConfigReader` - never hardcode a URL or
credential in a Page/Flow/Test. Swapping environments or browsers should
never require touching Java code.

## 14. `CONFIRMED` / `PENDING` locator status

While building out a new Page Object against a real app, mark each
section with its verification status:

```java
// CONFIRMED - exercised by a passing test (TC_04-TC_21)
// PENDING   - locator is a placeholder guess, not yet run against the real app
```

This makes it obvious, at a glance, which parts of a large Page Object
are safe to build on top of and which still need attention - especially
useful when handing work between people or picking a project back up
after a break.

## 15. Dead-code check before calling a Page/Flow "done"

Before considering a Page/Flow/Test trio finished, verify every public
method in the Page is called from the Flow, and every public method in
the Flow is called from the Test (directly, or internally by another
Flow method). A quick way:

```bash
for m in $(grep -oP '(?<=public ).*?\w+(?=\()' YourPage.java | awk '{print $NF}' | sort -u); do
  grep -q "\.$m(" YourFlow.java || echo "possibly unused: $m"
done
```

Methods flagged as "unused" that turn out to be called internally by
another method in the same file (a helper used by `findRowById`, say)
are fine to keep - genuinely orphaned methods should be deleted.

## 16. Evidence screenshots: automatic vs on demand

Two kinds of screenshots, deliberately handled differently:

- **PASS/FAIL**: taken automatically by `TestListener` after every test.
  Tests never call anything for these.
- **EVIDENCE**: taken by the test itself via `captureEvidence()` /
  `captureEvidence("label")` (inherited from `BaseTest`), at the moment
  you choose - typically the initial state before an action, as proof of
  what the screen looked like before the test changed anything.

Both land in the same `{date}/{testName}/` folder, so everything for one
test sits together. Design points worth keeping if you extend this:

- **`BaseTest` remembers the running test's name** (a `ThreadLocal`
  filled in `setUp(Method)`), so a test calls `captureEvidence()` without
  passing its own name - one less thing to get wrong and one less
  argument to copy-paste.
- **Evidence never fails a test.** A screenshot is supporting material;
  if one can't be taken, `captureEvidence` prints a warning and the test
  continues. Don't turn a passing test red over a missing picture.
- **Evidence file names include milliseconds**, so two shots with the
  same label in the same second can't collide.
- **Take it before a native alert opens.** While a native `alert()` is
  showing, the browser refuses screenshots (see section 7), so capture
  first and trigger the alert afterward.

---

## What to delete once you've internalized these patterns

`pages/sample/`, `flows/sample/`, `test/sample/` exist purely to show
the shapes above in one place. Once you've copied what you need into
your real Page Objects, delete that trio - it's a reference, not a
dependency anything else relies on.
