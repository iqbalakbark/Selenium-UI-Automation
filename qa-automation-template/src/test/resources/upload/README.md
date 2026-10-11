# Upload test data

Put files your tests upload (via `FileUploadHelper.getFilePath("your-file.xlsx")`)
in this folder.

This folder is intentionally excluded from git (see `.gitignore`) since
test data files tend to be large and project-specific. Each teammate
should drop their own copies here locally, or your CI pipeline should
fetch them from wherever your team keeps shared test assets.
