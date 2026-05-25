package com.code.atlas.web.repository;

import java.util.List;

import com.code.atlas.web.domain.AIModelApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIModelApiKeyRepository extends JpaRepository<AIModelApiKey, Long> {

    List<AIModelApiKey> findByIsActiveTrueOrderByNameAsc();
}
