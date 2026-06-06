package com.code.atlas.web.service.context.indexed.indexer;

import com.code.atlas.web.domain.DatabaseIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.repository.DatabaseIndexRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class MigrationEntityLinker {

    private final DatabaseIndexRepository databaseIndexRepository;

    public MigrationEntityLinker(DatabaseIndexRepository databaseIndexRepository) {
        this.databaseIndexRepository = databaseIndexRepository;
    }

    public void linkLatestMigrations(Project project) {
        List<DatabaseIndexEntry> entries = databaseIndexRepository.findByProjectId(project.getId());
        List<DatabaseIndexEntry> migrations = entries.stream()
                .filter(entry -> entry.getFilePath().toLowerCase(Locale.ROOT).endsWith(".sql"))
                .sorted(Comparator.comparing(DatabaseIndexEntry::getFilePath).reversed())
                .toList();
        if (migrations.isEmpty()) {
            return;
        }
        for (DatabaseIndexEntry entityEntry : entries) {
            if (entityEntry.getEntity().isBlank() || !entityEntry.getMigration().isBlank()) {
                continue;
            }
            String tableName = entityEntry.getTableName();
            if (tableName.isBlank() || tableName.equals(entityEntry.getFilePath())) {
                continue;
            }
            for (DatabaseIndexEntry migration : migrations) {
                if (matchesTable(migration, tableName)) {
                    entityEntry.setMigration(migration.getFilePath());
                    databaseIndexRepository.save(entityEntry);
                    break;
                }
            }
        }
    }

    private boolean matchesTable(DatabaseIndexEntry migration, String tableName) {
        String normalizedTable = tableName.toLowerCase(Locale.ROOT);
        if (migration.getTableName().equalsIgnoreCase(tableName)) {
            return true;
        }
        String path = migration.getFilePath().toLowerCase(Locale.ROOT);
        if (path.contains(normalizedTable)) {
            return true;
        }
        String underscored = normalizedTable.replace('_', '-');
        return path.contains(underscored);
    }
}
