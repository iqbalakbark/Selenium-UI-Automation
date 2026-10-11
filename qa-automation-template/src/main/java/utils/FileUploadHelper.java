package utils;

import java.io.File;

/**
 * Resolves absolute paths for files used in file-upload test steps.
 * Files themselves are supplied by you directly under
 * src/test/resources/upload/ - this class only builds the absolute path
 * that gets passed to input[type=file].sendKeys(...).
 * <p>
 * Uses src/test/resources (not src/test/java/.../resources) since that's
 * Maven's standard resources directory - test data belongs there, not
 * mixed into a java source folder.
 */
public class FileUploadHelper {

    private static final String UPLOAD_FOLDER = "src/test/resources/upload/";

    private FileUploadHelper() {
        // utility class, no instantiation
    }

    public static String getFilePath(String fileName) {
        File file = new File(UPLOAD_FOLDER + fileName);
        if (!file.exists()) {
            throw new RuntimeException("Upload file not found: " + file.getAbsolutePath() +
                    ". Place the file under " + UPLOAD_FOLDER);
        }
        return file.getAbsolutePath();
    }
}
