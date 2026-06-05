package com.code.atlas.web.repository;

import com.code.atlas.web.domain.PatternIndexEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatternIndexRepository extends JpaRepository<PatternIndexEntry, Long> {

    List<PatternIndexEntry> findByProjectId(Long projectId);

    Optional<PatternIndexEntry> findByProjectIdAndPatternIgnoreCase(Long projectId, String pattern);

    void deleteByProjectId(Long projectId);
}
