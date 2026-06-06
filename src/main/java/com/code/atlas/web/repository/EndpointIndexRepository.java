package com.code.atlas.web.repository;

import com.code.atlas.web.domain.EndpointIndexEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EndpointIndexRepository extends JpaRepository<EndpointIndexEntry, Long> {

    List<EndpointIndexEntry> findByProjectId(Long projectId);

    List<EndpointIndexEntry> findByProjectIdAndPathContainingIgnoreCase(Long projectId, String pathFragment);

    Optional<EndpointIndexEntry> findByProjectIdAndHttpMethodAndPath(
            Long projectId,
            String httpMethod,
            String path
    );

    void deleteByProjectIdAndFilePath(Long projectId, String filePath);

    void deleteByProjectIdAndFilePathNotIn(Long projectId, List<String> filePaths);

    void deleteByProjectId(Long projectId);
}
