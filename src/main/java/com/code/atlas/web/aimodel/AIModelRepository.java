package com.code.atlas.web.aimodel;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIModelRepository extends JpaRepository<AIModel, Long> {

    @EntityGraph(attributePaths = {"aiModelApiKey"})
    @Override
    List<AIModel> findAll();

    @EntityGraph(attributePaths = {"aiModelApiKey"})
    @Override
    Optional<AIModel> findById(Long id);

    @EntityGraph(attributePaths = {"aiModelApiKey"})
    List<AIModel> findByEnabledTrue();

    long countByAiModelApiKey_Id(Long apiKeyId);
}
