package com.code.atlas.web.repository;

import com.code.atlas.web.domain.GraphEdgeEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GraphEdgeRepository extends JpaRepository<GraphEdgeEntry, Long> {

    List<GraphEdgeEntry> findByProjectId(Long projectId);

    List<GraphEdgeEntry> findByProjectIdAndSource(Long projectId, String source);

    List<GraphEdgeEntry> findByProjectIdAndTarget(Long projectId, String target);

    void deleteByProjectIdAndSourceFilePath(Long projectId, String sourceFilePath);

    void deleteByProjectIdAndSourceFilePathNotIn(Long projectId, List<String> filePaths);

    void deleteByProjectId(Long projectId);
}
