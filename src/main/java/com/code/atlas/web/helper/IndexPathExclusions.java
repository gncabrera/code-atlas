package com.code.atlas.web.helper;

import java.util.Set;

public final class IndexPathExclusions {

    private static final Set<String> EXCLUDED_PATH_SEGMENTS = Set.of(
        // Package managers
        "node_modules",
        "bower_components",
        "jspm_packages",

        // Java
        "target",
        ".mvn",
        ".gradle",

        // Kotlin / Android
        ".kotlin",
        ".android",

        // JS/TS
        ".next",
        ".nuxt",
        ".svelte-kit",
        ".angular",
        ".turbo",
        ".vite",
        ".cache",
        ".parcel-cache",

        // Build outputs
        "build",
        "dist",
        "out",
        "bin",
        "obj",
        "release",
        "debug",

        // Coverage
        "coverage",
        ".nyc_output",
        "htmlcov",

        // Python
        "__pycache__",
        ".pytest_cache",
        ".mypy_cache",
        ".ruff_cache",
        ".tox",
        ".venv",
        "venv",
        "env",
        ".env",

        // Ruby
        ".bundle",
        "vendor",

        // Go
        "pkg",

        // Terraform
        ".terraform",

        // Docker
        ".docker",

        // Git
        ".git",

        // SVN / Mercurial
        ".svn",
        ".hg",

        // IDEs
        ".idea",
        ".vscode",
        ".fleet",
        ".settings",
        ".metadata",

        // Eclipse
        ".classpath",
        ".project",

        // NetBeans
        "nbproject",

        // Logs
        "logs",
        "log",

        // Temp
        "tmp",
        "temp",

        // OS
        ".DS_Store",
        "__MACOSX",
        "System Volume Information",

        // Generated docs
        "site",
        "docs/build",

        // Package outputs
        "vendor/bundle",

        // CI/CD caches
        ".github/cache",
        ".circleci/cache",

        // Generated code
        "generated",
        "generated-sources",
        "generated-test-sources",

        // Static analysis
        ".sonar",
        ".scannerwork",

        // Misc caches
        ".cache-loader",
        ".sass-cache",
        ".eslintcache"
);

    private IndexPathExclusions() {
    }

    public static boolean containsExcludedSegment(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String normalized = relativePath.replace('\\', '/').replaceAll("^/+", "");
        for (String segment : normalized.split("/")) {
            if (EXCLUDED_PATH_SEGMENTS.contains(segment)) {
                return true;
            }
        }
        return false;
    }
}
