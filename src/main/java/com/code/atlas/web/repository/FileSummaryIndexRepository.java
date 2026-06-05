package com.code.atlas.web.repository;

import com.code.atlas.web.domain.FileSummaryIndexEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileSummaryIndexRepository extends JpaRepository<FileSummaryIndexEntry, Long> {

    List<FileSummaryIndexEntry> findByProjectId(Long projectId);

    Optional<FileSummaryIndexEntry> findByProjectIdAndFilePath(Long projectId, String filePath);

    void deleteByProjectId(Long projectId);
}
