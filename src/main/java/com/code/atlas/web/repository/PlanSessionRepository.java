package com.code.atlas.web.repository;

import com.code.atlas.web.domain.PlanSession;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanSessionRepository extends JpaRepository<PlanSession, Long> {

    List<PlanSession> findAllByOrderByCreatedAtDesc();
}
