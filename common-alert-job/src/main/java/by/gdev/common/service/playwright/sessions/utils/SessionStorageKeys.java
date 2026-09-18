package by.gdev.common.service.playwright.sessions.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SessionStorageKeys {

    private SessionStorageKeys() {
    }

    static String sanitize(String s) {
        if (s == null || s.isBlank()) {
            return "unknown";
        }
        return s.replaceAll("[^a-zA-Z0-9._@-]", "_");
    }

    public static String memoryKey(String userUuid, String siteName, String login) {
        return sanitize(userUuid) + "/" + siteName + "/" + sanitize(login);
    }

    public static Path sessionFilePath(String baseDir, String userUuid, String siteName, String login) {
        return Paths.get(baseDir)
                .resolve(sanitize(userUuid))
                .resolve(siteName)
                .resolve(sanitize(login) + ".json");
    }

    public static Path mmapSessionFilePath(String baseDir, String userUuid, String siteName, String login) {
        return Paths.get(baseDir)
                .resolve(sanitize(userUuid))
                .resolve(siteName)
                .resolve(sanitize(login) + ".mmap");
    }
}
