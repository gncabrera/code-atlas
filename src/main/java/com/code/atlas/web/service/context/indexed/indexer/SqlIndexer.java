package com.code.atlas.web.service.context.indexed.indexer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SqlIndexer implements LanguageIndexer {

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([A-Za-z0-9_\"']+)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean supports(String extension) {
        return "sql".equalsIgnoreCase(extension);
    }

    @Override
    public IndexerOutput index(IndexFileInput input) {
        String relativePath = input.relativePath();
        if (!isMigrationSql(relativePath)) {
            return IndexerOutput.empty();
        }
        List<DatabaseRow> rows = new ArrayList<>();
        rows.add(new DatabaseRow(relativePath, "", "", relativePath));
        Set<String> seenTables = new LinkedHashSet<>();
        Matcher matcher = CREATE_TABLE_PATTERN.matcher(input.content());
        while (matcher.find()) {
            String tableName = normalizeTableName(matcher.group(1));
            if (tableName.isBlank() || !seenTables.add(tableName)) {
                continue;
            }
            rows.add(new DatabaseRow(tableName, "", "", relativePath));
        }
        return new IndexerOutput(List.of(), List.of(), List.of(), rows, List.of());
    }

    static boolean isMigrationSql(String relativePath) {
        String lower = relativePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        return lower.endsWith(".sql")
                && (lower.contains("/migration/") || lower.contains("/migrations/") || lower.contains("flyway") || lower.contains("liquibase"));
    }

    private static String normalizeTableName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\"", "").replace("'", "").trim();
    }
}
