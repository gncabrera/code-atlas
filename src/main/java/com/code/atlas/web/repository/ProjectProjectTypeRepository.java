package com.code.atlas.web.repository;

import com.code.atlas.web.domain.ProjectProjectType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectProjectTypeRepository extends JpaRepository<ProjectProjectType, Long> {

    @EntityGraph(attributePaths = {"projectType"})
    List<ProjectProjectType> findByProjectIdOrderByProjectTypeNameAsc(Long projectId);

    @EntityGraph(attributePaths = {"projectType"})
    List<ProjectProjectType> findByProjectIdInOrderByProjectIdAscProjectTypeNameAsc(Collection<Long> projectIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProjectProjectType ppt WHERE ppt.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    boolean existsByProjectTypeId(Long projectTypeId);
}
