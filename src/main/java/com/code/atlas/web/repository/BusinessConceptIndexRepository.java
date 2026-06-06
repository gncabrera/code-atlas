package com.code.atlas.web.repository;

import com.code.atlas.web.domain.BusinessConceptIndexEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessConceptIndexRepository extends JpaRepository<BusinessConceptIndexEntry, Long> {

    List<BusinessConceptIndexEntry> findByProjectId(Long projectId);

    Optional<BusinessConceptIndexEntry> findByProjectIdAndConceptIgnoreCase(Long projectId, String concept);

    void deleteByProjectId(Long projectId);
}
