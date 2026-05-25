package com.code.atlas.web.repository;

import java.util.List;
import java.util.Optional;

import com.code.atlas.web.domain.ProjectFileIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectFileIndexRepository extends JpaRepository<ProjectFileIndex, Long> {

    List<ProjectFileIndex> findByProjectId(Long projectId);

    Optional<ProjectFileIndex> findByProjectIdAndFilePath(Long projectId, String filePath);

    void deleteByProjectIdAndFilePathNotIn(Long projectId, List<String> filePaths);

    void deleteByProjectId(Long projectId);
}
