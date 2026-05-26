package com.code.atlas.web.repository;

import com.code.atlas.web.domain.PromptOptimizerMode;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptOptimizerModeRepository extends JpaRepository<PromptOptimizerMode, Long> {

    Optional<PromptOptimizerMode> findByCode(String code);

    boolean existsByCode(String code);

    List<PromptOptimizerMode> findAllByHiddenFalseOrderByNameAsc();
}
