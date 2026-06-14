package com.code.atlas.web.repository;

import com.code.atlas.web.domain.PlanModePrompt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanModePromptRepository extends JpaRepository<PlanModePrompt, Long> {

    Optional<PlanModePrompt> findByCode(String code);

    boolean existsByCode(String code);
}
