package com.code.atlas.web.service.context;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class ContextFileSupport {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "java", "sql", "xml", "yml", "yaml", "properties", "md", "html", "js", "ts", "json"
    );
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git", ".idea", ".cursor", "target", "build", "node_modules", ".mvn", ".gradle"
    );

    private ContextFileSupport() {
    }

    public static boolean isRelevantFile(Path filePath) {
        String extension = extensionOf(filePath.getFileName().toString());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return false;
        }
        for (Path part : filePath) {
            if (EXCLUDED_DIRECTORIES.contains(part.toString().toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    public static String extensionOf(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public static String languageByExtension(String extension) {
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "java" -> "java";
            case "sql" -> "sql";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "yml", "yaml" -> "yaml";
            case "xml" -> "xml";
            case "html" -> "html";
            case "json" -> "json";
            default -> "text";
        };
    }
}
