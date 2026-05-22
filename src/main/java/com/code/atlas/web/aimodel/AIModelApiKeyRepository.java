package com.code.atlas.web.aimodel;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIModelApiKeyRepository extends JpaRepository<AIModelApiKey, Long> {

    List<AIModelApiKey> findByIsActiveTrueOrderByNameAsc();
}
