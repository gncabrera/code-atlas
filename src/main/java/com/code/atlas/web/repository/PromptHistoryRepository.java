package com.code.atlas.web.repository;

import java.util.List;

import com.code.atlas.web.domain.PromptHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromptHistoryRepository extends JpaRepository<PromptHistory, Long> {

    @EntityGraph(attributePaths = {"project", "aiModel"})
    List<PromptHistory> findAllByOrderByCreatedAtDesc();
}
