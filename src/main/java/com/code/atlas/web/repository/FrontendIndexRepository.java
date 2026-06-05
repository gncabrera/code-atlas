package com.code.atlas.web.repository;

import com.code.atlas.web.domain.FrontendIndexEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrontendIndexRepository extends JpaRepository<FrontendIndexEntry, Long> {

    List<FrontendIndexEntry> findByProjectId(Long projectId);

    List<FrontendIndexEntry> findByProjectIdAndEndpointContainingIgnoreCase(Long projectId, String endpointFragment);

    void deleteByProjectIdAndFilePath(Long projectId, String filePath);

    void deleteByProjectIdAndFilePathNotIn(Long projectId, List<String> filePaths);

    void deleteByProjectId(Long projectId);
}
