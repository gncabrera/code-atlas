package com.code.atlas.web.prompt;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptHistoryRepository extends JpaRepository<PromptHistory, Long> {

    @EntityGraph(attributePaths = {"project", "aiModel"})
    List<PromptHistory> findAllByOrderByCreatedAtDesc();
}
