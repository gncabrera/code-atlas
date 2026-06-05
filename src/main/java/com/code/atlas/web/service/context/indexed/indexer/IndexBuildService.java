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
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    public IndexBuildService(
            ProjectFileIndexRepository projectFileIndexRepository,
            SymbolIndexRepository symbolIndexRepository,
            EndpointIndexRepository endpointIndexRepository,
            GraphEdgeRepository graphEdgeRepository,
            DatabaseIndexRepository databaseIndexRepository,
            FrontendIndexRepository frontendIndexRepository,
            List<LanguageIndexer> languageIndexers
    ) {
        this.projectFileIndexRepository = projectFileIndexRepository;
        this.symbolIndexRepository = symbolIndexRepository;
        this.endpointIndexRepository = endpointIndexRepository;
        this.graphEdgeRepository = graphEdgeRepository;
        this.databaseIndexRepository = databaseIndexRepository;
        this.frontendIndexRepository = frontendIndexRepository;
        this.languageIndexers = languageIndexers;
    }

    @Transactional
    public void rebuildProject(Project project) {
        List<ProjectFileIndex> entries = projectFileIndexRepository.findByProjectId(project.getId());
        if (entries.isEmpty()) {
            purgeStructuralIndices(project.getId());
            return;
        }
        Path projectRoot = Path.of(project.getPath()).normalize();
        List<String> activePaths = new ArrayList<>();
        for (ProjectFileIndex entry : entries) {
            activePaths.add(entry.getFilePath());
            indexFile(project, projectRoot, entry);
        }
        purgeOrphanPaths(project.getId(), activePaths);
    }

    private void indexFile(Project project, Path projectRoot, ProjectFileIndex entry) {
        Path filePath = projectRoot.resolve(entry.getFilePath()).normalize();
        if (!Files.isRegularFile(filePath)) {
            deleteIndicesForFile(project.getId(), entry.getFilePath());
            return;
        }
        LanguageIndexer indexer = resolveIndexer(entry.getFileExtension());
        if (indexer == null) {
            deleteIndicesForFile(project.getId(), entry.getFilePath());
            return;
        }
        try {
            String content = Files.readString(filePath);
            IndexerOutput output = indexer.index(new IndexFileInput(entry.getFilePath(), entry.getFileExtension(), content));
            deleteIndicesForFile(project.getId(), entry.getFilePath());
            persistOutput(project, entry.getFilePath(), output);
        } catch (IOException ex) {
            deleteIndicesForFile(project.getId(), entry.getFilePath());
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
        for (SymbolRow row : output.symbols()) {
            SymbolIndexEntry entity = new SymbolIndexEntry();
            entity.setProject(project);
            entity.setSymbol(row.symbol());
            entity.setKind(row.kind());
            entity.setFilePath(filePath);
            entity.setLine(row.line());
            symbolIndexRepository.save(entity);
        }
        for (EndpointRow row : output.endpoints()) {
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
            GraphEdgeEntry entity = new GraphEdgeEntry();
            entity.setProject(project);
            entity.setSource(row.source());
            entity.setTarget(row.target());
            entity.setRelation(row.relation());
            entity.setSourceFilePath(filePath);
            graphEdgeRepository.save(entity);
        }
        for (DatabaseRow row : output.databaseRows()) {
            DatabaseIndexEntry entity = new DatabaseIndexEntry();
            entity.setProject(project);
            entity.setTableName(row.tableName().isBlank() ? filePath : row.tableName());
            entity.setEntity(row.entity());
            entity.setRepository(row.repository());
            entity.setMigration(row.migration());
            entity.setFilePath(filePath);
            databaseIndexRepository.save(entity);
        }
        for (FrontendRow row : output.frontendRows()) {
            FrontendIndexEntry entity = new FrontendIndexEntry();
            entity.setProject(project);
            entity.setComponent(row.component());
            entity.setService(row.service());
            entity.setEndpoint(row.endpoint());
            entity.setFilePath(filePath);
            frontendIndexRepository.save(entity);
        }
    }

    private void deleteIndicesForFile(Long projectId, String filePath) {
        symbolIndexRepository.deleteByProjectIdAndFilePath(projectId, filePath);
        endpointIndexRepository.deleteByProjectIdAndFilePath(projectId, filePath);
        graphEdgeRepository.deleteByProjectIdAndSourceFilePath(projectId, filePath);
        databaseIndexRepository.deleteByProjectIdAndFilePath(projectId, filePath);
        frontendIndexRepository.deleteByProjectIdAndFilePath(projectId, filePath);
    }

    private void purgeOrphanPaths(Long projectId, List<String> activePaths) {
        if (activePaths.isEmpty()) {
            purgeStructuralIndices(projectId);
            return;
        }
        symbolIndexRepository.deleteByProjectIdAndFilePathNotIn(projectId, activePaths);
        endpointIndexRepository.deleteByProjectIdAndFilePathNotIn(projectId, activePaths);
        graphEdgeRepository.deleteByProjectIdAndSourceFilePathNotIn(projectId, activePaths);
        databaseIndexRepository.deleteByProjectIdAndFilePathNotIn(projectId, activePaths);
        frontendIndexRepository.deleteByProjectIdAndFilePathNotIn(projectId, activePaths);
    }

    private void purgeStructuralIndices(Long projectId) {
        symbolIndexRepository.deleteByProjectId(projectId);
        endpointIndexRepository.deleteByProjectId(projectId);
        graphEdgeRepository.deleteByProjectId(projectId);
        databaseIndexRepository.deleteByProjectId(projectId);
        frontendIndexRepository.deleteByProjectId(projectId);
    }
}
