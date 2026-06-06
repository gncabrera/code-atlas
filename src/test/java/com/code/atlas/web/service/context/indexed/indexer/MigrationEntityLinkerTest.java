package com.code.atlas.web.service.context.indexed.indexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.code.atlas.web.domain.DatabaseIndexEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.repository.DatabaseIndexRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MigrationEntityLinkerTest {

    @Mock
    private DatabaseIndexRepository databaseIndexRepository;

    @InjectMocks
    private MigrationEntityLinker linker;

    @Captor
    private ArgumentCaptor<DatabaseIndexEntry> entryCaptor;

    @Test
    void linksLatestMigrationToEntityByTableName() {
        Project project = new Project();
        project.setId(1L);

        DatabaseIndexEntry entityEntry = new DatabaseIndexEntry();
        entityEntry.setTableName("ai_model");
        entityEntry.setEntity("AIModel");
        entityEntry.setFilePath("src/main/java/AIModel.java");
        entityEntry.setMigration("");

        DatabaseIndexEntry olderMigration = new DatabaseIndexEntry();
        olderMigration.setTableName("src/main/resources/db/migration/V1__init.sql");
        olderMigration.setFilePath("src/main/resources/db/migration/V1__init.sql");

        DatabaseIndexEntry latestMigration = new DatabaseIndexEntry();
        latestMigration.setTableName("ai_model");
        latestMigration.setFilePath("src/main/resources/db/migration/V9__ai_model.sql");

        when(databaseIndexRepository.findByProjectId(1L))
                .thenReturn(List.of(entityEntry, olderMigration, latestMigration));

        linker.linkLatestMigrations(project);

        verify(databaseIndexRepository).save(entryCaptor.capture());
        assertEquals("src/main/resources/db/migration/V9__ai_model.sql", entryCaptor.getValue().getMigration());
    }
}
