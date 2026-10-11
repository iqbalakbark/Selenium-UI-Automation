package utils;

import java.io.File;
import java.nio.file.Paths;

/**
 * Verifies file downloads by watching the OS Downloads folder's file
 * count - simpler and more portable than trying to intercept the
 * browser's download manager directly.
 */
public class DownloadUtils {

    private static final String DOWNLOAD_DIR =
            Paths.get(System.getProperty("user.home"), "Downloads").toString();

    public static int countDownloadedFiles() {
        File folder = new File(DOWNLOAD_DIR);
        File[] files = folder.listFiles();
        return files == null ? 0 : files.length;
    }

    /**
     * @param initialCount   file count captured before triggering the download
     * @param timeoutSeconds max time to wait
     * @return true if the file count increased within the timeout, false otherwise
     */
    public static boolean waitUntilFileCountIncreases(int initialCount, int timeoutSeconds) {
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            if (countDownloadedFiles() > initialCount) {
                return true;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
