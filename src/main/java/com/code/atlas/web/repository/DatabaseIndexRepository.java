package com.code.atlas.web.repository;

import com.code.atlas.web.domain.DatabaseIndexEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatabaseIndexRepository extends JpaRepository<DatabaseIndexEntry, Long> {

    List<DatabaseIndexEntry> findByProjectId(Long projectId);

    List<DatabaseIndexEntry> findByProjectIdAndTableNameContainingIgnoreCase(Long projectId, String tableFragment);

    void deleteByProjectIdAndFilePath(Long projectId, String filePath);

    void deleteByProjectIdAndFilePathNotIn(Long projectId, List<String> filePaths);

    void deleteByProjectId(Long projectId);
}
