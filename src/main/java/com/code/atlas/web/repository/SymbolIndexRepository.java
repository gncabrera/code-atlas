package com.code.atlas.web.repository;

import com.code.atlas.web.domain.SymbolIndexEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SymbolIndexRepository extends JpaRepository<SymbolIndexEntry, Long> {

    List<SymbolIndexEntry> findByProjectId(Long projectId);

    List<SymbolIndexEntry> findByProjectIdAndSymbolIgnoreCase(Long projectId, String symbol);

    void deleteByProjectIdAndFilePath(Long projectId, String filePath);

    void deleteByProjectIdAndFilePathNotIn(Long projectId, List<String> filePaths);

    void deleteByProjectId(Long projectId);
}
