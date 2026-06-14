package com.code.atlas.web.repository;

import com.code.atlas.web.domain.PlanDiscovery;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanDiscoveryRepository extends JpaRepository<PlanDiscovery, Long> {

    Optional<PlanDiscovery> findBySessionId(Long sessionId);
}
