# QA Automation Template

A Selenium + TestNG + Java automation framework using Page Object Model
(POM) + a Flow layer for business logic. Built and battle-tested against
a real multi-role CMS - see [CONVENTIONS.md](CONVENTIONS.md) for the
reasoning behind every non-obvious decision in here.

## What's in the box

| Layer | What it is | Do you touch it? |
|---|---|---|
| `config/`, `factory/`, `utils/`, `base/` | Generic framework plumbing (driver lifecycle, waits, screenshots, config) | Rarely - it's designed to work as-is |
| `pages/`, `flows/`, `test/` (root) | `LoginPage`/`LoginFlow`/`LoginTest` example | Adapt to your app's real login |
| `pages/sample/`, `flows/sample/`, `test/sample/` | Pattern reference for a generic "list + filter + edit popup" page | Copy patterns from it, then delete it |
| `CONVENTIONS.md` | Why the framework is built this way | Read once, refer back as needed |

## Quick start

### 1. Prerequisites
- Java 17+
- Maven
- Chrome, Edge, or Firefox installed (Selenium 4.6+'s built-in Selenium
  Manager auto-downloads the matching driver - no manual driver setup)

### 2. Set up your config
```bash
cp config.properties.example config.properties
```
Fill in `config.properties` with your app's real URL, browser choice,
and credentials per role. This file is gitignored - never commit it.

### 3. Rename the project
In `pom.xml`, change `groupId`/`artifactId` to your project's name.

### 4. Adapt login
Open `src/main/java/pages/LoginPage.java` and `LoginFlow.java`, swap the
placeholder locators for your app's real login form. Update
`test/LoginTest.java`'s assertion to check something real about your
app's post-login state.

### 5. Build your first real page
Read through `pages/sample/SampleListPage.java` and
`flows/sample/SampleListFlow.java` - each numbered pattern comment maps
to a section in `CONVENTIONS.md`. Copy whichever patterns your first
real page needs (custom dropdown? native date input? pagination?) into
a new `pages/yourarea/YourPage.java`, following the same
Page → Flow → Test structure.

Once you're comfortable with the patterns, delete `pages/sample/`,
`flows/sample/`, `test/sample/` - they're a reference, nothing else
depends on them.

### 6. Add more roles (if your app has them)
Create a new package per role (`pages/admin/`, `pages/requestor/`, etc.)
rather than branching on role inside shared files - see CONVENTIONS.md
§12 for why. Same class names across role packages is fine and expected.

### 7. Run
```bash
mvn test                                          # runs testng-smoke.xml (default)
mvn test -DsuiteXmlFile=testng-regression.xml     # full regression
```

Screenshots (pass and fail) land in
`test-output/screenshots/{date}/{testName}/` automatically - nothing to
call from inside a test.

### Test evidence ("bukti awal")

For evidence you want to take at a moment of your choosing (typically
the initial state before an action), call one of these from any test
that extends `BaseTest`:

```java
captureEvidence();                 // labelled "Initial"
captureEvidence("Before Edit");    // your own label
```

The shot is filed in the same folder as that test's PASS/FAIL
screenshot, as `{timestamp}_EVIDENCE_{label}_{testName}.png`. You can
call it as many times as you like per test. It never fails the test - if
a screenshot can't be taken, a warning is printed and the test carries
on. Call it *before* an action that opens a native alert, since the
browser refuses screenshots while one is open.

## Project structure

```
src/main/java/
├── config/ConfigReader.java       # reads config.properties
├── factory/DriverFactory.java     # WebDriver lifecycle, multi-browser
├── utils/
│   ├── WaitUtils.java             # all explicit-wait logic, stale-retry
│   ├── ScreenshotUtils.java       # auto screenshot on pass/fail + on-demand evidence
│   ├── DownloadUtils.java         # verify a download happened
│   ├── FileUploadHelper.java      # resolve paths for upload test files
│   └── ScrollUtils.java           # JS-based scroll helpers
├── pages/                         # locators + element interaction only
└── flows/                         # business logic, built on top of pages/

src/test/java/
├── base/
│   ├── BaseTest.java              # driver setup/teardown for every test
│   └── TestListener.java          # wires up automatic screenshots
└── test/                          # scenario + assertion only

src/test/resources/upload/         # test data files for upload tests (gitignored)
```

## Design philosophy

- **Page**: locators + raw interaction. No business logic.
- **Flow**: business processes ("find this row", "fill this form and
  submit"). Built on top of Page methods.
- **Test**: scenario + assertion. Everything goes through a Flow -
  no direct Selenium calls in a test.

See [CONVENTIONS.md](CONVENTIONS.md) for the full reasoning, including
real bugs this structure prevents (stale elements, custom dropdown
pitfalls, native date input handling, and more).
