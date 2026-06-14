package com.code.atlas.web.repository;

import com.code.atlas.web.domain.PlanResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanResultRepository extends JpaRepository<PlanResult, Long> {

    Optional<PlanResult> findBySessionId(Long sessionId);
}
