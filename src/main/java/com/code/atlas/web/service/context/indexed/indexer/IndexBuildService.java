package com.code.atlas.web.service.context.indexed.indexer;

import com.code.atlas.web.domain.DatabaseIndexEntry;
import com.code.atlas.web.domain.EndpointIndexEntry;
import com.code.atlas.web.domain.FrontendIndexEntry;
import com.code.atlas.web.domain.GraphEdgeEntry;
import com.code.atlas.web.domain.Project;
import com.code.atlas.web.domain.ProjectFileIndex;
import com.code.atlas.web.domain.SymbolIndexEntry;
import com.code.atlas.web.repository.DatabaseIndexRepository;
import com.code.atlas.web.repository.EndpointIndexRepository;
import com.code.atlas.web.repository.FrontendIndexRepository;
import com.code.atlas.web.repository.GraphEdgeRepository;
import com.code.atlas.web.repository.ProjectFileIndexRepository;
import com.code.atlas.web.repository.SymbolIndexRepository;
import com.code.atlas.web.service.context.indexed.ContextPipelineLogger;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class IndexBuildService {

    private final ProjectFileIndexRepository projectFileIndexRepository;
    private final SymbolIndexRepository symbolIndexRepository;
    private final EndpointIndexRepository endpointIndexRepository;
    private final GraphEdgeRepository graphEdgeRepository;
    private final DatabaseIndexRepository databaseIndexRepository;
    private final FrontendIndexRepository frontendIndexRepository;
    private final List<LanguageIndexer> languageIndexers;
    private final EntityManager entityManager;
    private final ContextPipelineLogger pipelineLogger;
    private final MigrationEntityLinker migrationEntityLinker;

    public IndexBuildService(
            ProjectFileIndexRepository projectFileIndexRepository,
            SymbolIndexRepository symbolIndexRepository,
            EndpointIndexRepository endpointIndexRepository,
            GraphEdgeRepository graphEdgeRepository,
            DatabaseIndexRepository databaseIndexRepository,
            FrontendIndexRepository frontendIndexRepository,
            List<LanguageIndexer> languageIndexers,
            EntityManager entityManager,
            ContextPipelineLogger pipelineLogger,
            MigrationEntityLinker migrationEntityLinker
    ) {
        this.projectFileIndexRepository = projectFileIndexRepository;
        this.symbolIndexRepository = symbolIndexRepository;
        this.endpointIndexRepository = endpointIndexRepository;
        this.graphEdgeRepository = graphEdgeRepository;
        this.databaseIndexRepository = databaseIndexRepository;
        this.frontendIndexRepository = frontendIndexRepository;
        this.languageIndexers = languageIndexers;
        this.entityManager = entityManager;
        this.pipelineLogger = pipelineLogger;
        this.migrationEntityLinker = migrationEntityLinker;
    }

    @Transactional
    public void rebuildProject(Project project) {
        rebuildProject(project, "index");
    }

    @Transactional
    public void rebuildProject(Project project, String phase) {
        List<ProjectFileIndex> entries = projectFileIndexRepository.findByProjectId(project.getId());
        if (entries.isEmpty()) {
            purgeStructuralIndices(project.getId());
            pipelineLogger.message(project, phase, "No indexed files — structural indices cleared");
            return;
        }
        purgeStructuralIndices(project.getId());
        flushStructuralDeletes();
        Path projectRoot = Path.of(project.getPath()).normalize();
        int indexedFiles = 0;
        for (ProjectFileIndex entry : entries) {
            if (indexFile(project, projectRoot, entry)) {
                indexedFiles++;
            }
        }
        pipelineLogger.message(project, phase, "Structural indices rebuilt for " + indexedFiles + " of " + entries.size() + " files");
        migrationEntityLinker.linkLatestMigrations(project);
    }

    private boolean indexFile(Project project, Path projectRoot, ProjectFileIndex entry) {
        Path filePath = projectRoot.resolve(entry.getFilePath()).normalize();
        if (!Files.isRegularFile(filePath)) {
            return false;
        }
        LanguageIndexer indexer = resolveIndexer(entry.getFileExtension());
        if (indexer == null) {
            return false;
        }
        try {
            String content = Files.readString(filePath);
            IndexerOutput output = indexer.index(new IndexFileInput(entry.getFilePath(), entry.getFileExtension(), content));
            persistOutput(project, entry.getFilePath(), output);
            return true;
        } catch (IOException ex) {
            // Skip unreadable files.
            return false;
        }
    }

    private LanguageIndexer resolveIndexer(String extension) {
        for (LanguageIndexer indexer : languageIndexers) {
            if (indexer.supports(extension)) {
                return indexer;
            }
        }
        return null;
    }

    private void persistOutput(Project project, String filePath, IndexerOutput output) {
        Set<String> seenSymbols = new HashSet<>();
        for (SymbolRow row : output.symbols()) {
            String symbolKey = row.symbol() + "|" + row.line();
            if (!seenSymbols.add(symbolKey)) {
                continue;
            }
            SymbolIndexEntry entity = new SymbolIndexEntry();
            entity.setProject(project);
            entity.setSymbol(row.symbol());
            entity.setKind(row.kind());
            entity.setFilePath(filePath);
            entity.setLine(row.line());
            symbolIndexRepository.save(entity);
        }
        for (EndpointRow row : output.endpoints()) {
            if (endpointIndexRepository.findByProjectIdAndHttpMethodAndPath(
                    project.getId(),
                    row.httpMethod(),
                    row.path()
            ).isPresent()) {
                continue;
            }
            EndpointIndexEntry entity = new EndpointIndexEntry();
            entity.setProject(project);
            entity.setHttpMethod(row.httpMethod());
            entity.setPath(row.path());
            entity.setController(row.controller());
            entity.setService(row.service());
            entity.setFilePath(filePath);
            endpointIndexRepository.save(entity);
        }
        for (GraphEdgeRow row : output.graphEdges()) {
            if (graphEdgeRepository.findByProjectIdAndSourceAndTargetAndRelation(
                    project.getId(),
                    row.source(),
                    row.target(),
                    row.relation()
            ).isPresent()) {
                continue;
            }
            GraphEdgeEntry entity = new GraphEdgeEntry();
            entity.setProject(project);
            entity.setSource(row.source());
            entity.setTarget(row.target());
            entity.setRelation(row.relation());
            entity.setSourceFilePath(filePath);
            graphEdgeRepository.save(entity);
        }
        for (DatabaseRow row : output.databaseRows()) {
            String tableName = row.tableName().isBlank() ? filePath : row.tableName();
            if (databaseIndexRepository.findByProjectIdAndTableName(project.getId(), tableName).isPresent()) {
                continue;
            }
            DatabaseIndexEntry entity = new DatabaseIndexEntry();
            entity.setProject(project);
            entity.setTableName(tableName);
            entity.setEntity(row.entity());
            entity.setRepository(row.repository());
            entity.setMigration(row.migration());
            entity.setFilePath(filePath);
            databaseIndexRepository.save(entity);
        }
        for (FrontendRow row : output.frontendRows()) {
            String endpoint = row.endpoint() == null ? "" : row.endpoint();
            if (frontendIndexRepository.findByProjectIdAndComponentAndEndpoint(
                    project.getId(),
                    row.component(),
                    endpoint
            ).isPresent()) {
                continue;
            }
            FrontendIndexEntry entity = new FrontendIndexEntry();
            entity.setProject(project);
            entity.setComponent(row.component());
            entity.setService(row.service());
            entity.setEndpoint(endpoint);
            entity.setFilePath(filePath);
            frontendIndexRepository.save(entity);
        }
    }

    private void flushStructuralDeletes() {
        entityManager.flush();
    }

    private void purgeStructuralIndices(Long projectId) {
        symbolIndexRepository.deleteByProjectId(projectId);
        endpointIndexRepository.deleteByProjectId(projectId);
        graphEdgeRepository.deleteByProjectId(projectId);
        databaseIndexRepository.deleteByProjectId(projectId);
        frontendIndexRepository.deleteByProjectId(projectId);
    }
}
