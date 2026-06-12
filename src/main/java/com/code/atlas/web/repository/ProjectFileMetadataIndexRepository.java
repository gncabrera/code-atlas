package com.code.atlas.web.repository;

import com.code.atlas.web.domain.ProjectFileMetadataIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectFileMetadataIndexRepository extends JpaRepository<ProjectFileMetadataIndex, Long> {

    Optional<ProjectFileMetadataIndex> findByFileId(Long fileIndexId);

    List<ProjectFileMetadataIndex> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);

    void deleteByProjectIdAndFileIdNotIn(Long projectId, List<Long> longStream);
}
