package com.code.atlas.web.repository;

import com.code.atlas.web.domain.DeterministicFileIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeterministicFileIndexRepository extends JpaRepository<DeterministicFileIndex, Long> {

    List<DeterministicFileIndex> findByProjectId(Long projectId);

    Optional<DeterministicFileIndex> findByProjectIdAndFilePath(Long projectId, String filePath);

    void deleteByProjectIdAndFilePathNotIn(Long projectId, List<String> filePaths);

    void deleteByProjectId(Long projectId);
}