package com.code.atlas.web.service.context.indexed.indexer;

import com.code.atlas.web.domain.IndexerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ThymeleafStaticIndexer implements LanguageIndexer {

    @Override
    public IndexerProfile profile() {
        return IndexerProfile.THYMELEAF;
    }

    @Override
    public boolean supports(String extension) {
        String normalized = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return "html".equals(normalized) || "js".equals(normalized);
    }

    @Override
    public IndexerOutput index(IndexFileInput input) {
        String relativePath = input.relativePath().replace('\\', '/');
        String extension = input.extension().toLowerCase(Locale.ROOT);
        if ("html".equals(extension)) {
            return indexTemplate(relativePath);
        }
        if ("js".equals(extension)) {
            return indexScript(relativePath);
        }
        return IndexerOutput.empty();
    }

    private IndexerOutput indexTemplate(String relativePath) {
        if (!relativePath.toLowerCase(Locale.ROOT).contains("/templates/")) {
            return IndexerOutput.empty();
        }
        String component = baseName(relativePath, ".html");
        if (component.isBlank()) {
            return IndexerOutput.empty();
        }
        String jsPath = inferJsPath(relativePath, component);
        return new IndexerOutput(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new FrontendRow(component, jsPath, ""))
        );
    }

    private IndexerOutput indexScript(String relativePath) {
        String lower = relativePath.toLowerCase(Locale.ROOT);
        if (!lower.contains("/static/js/") || lower.contains("/vendor/")) {
            return IndexerOutput.empty();
        }
        String component = baseName(relativePath, ".js");
        if (component.isBlank()) {
            return IndexerOutput.empty();
        }
        return new IndexerOutput(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new FrontendRow(component, relativePath, relativePath))
        );
    }

    static String inferJsPath(String htmlPath, String component) {
        int templatesIndex = htmlPath.indexOf("templates/");
        if (templatesIndex < 0) {
            return "static/js/" + component + ".js";
        }
        String prefix = htmlPath.substring(0, templatesIndex);
        return prefix + "static/js/" + component + ".js";
    }

    private static String baseName(String path, String suffix) {
        int slash = path.lastIndexOf('/');
        String fileName = slash >= 0 ? path.substring(slash + 1) : path;
        if (!fileName.endsWith(suffix)) {
            return "";
        }
        return fileName.substring(0, fileName.length() - suffix.length());
    }
}
