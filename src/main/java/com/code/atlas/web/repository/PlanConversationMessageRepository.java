package com.code.atlas.web.repository;

import com.code.atlas.web.domain.PlanConversationMessage;
import com.code.atlas.web.domain.PlanThreadType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanConversationMessageRepository extends JpaRepository<PlanConversationMessage, Long> {

    List<PlanConversationMessage> findBySessionIdAndThreadTypeOrderByCreatedAtAsc(
            Long sessionId, PlanThreadType threadType);

    void deleteBySessionId(Long sessionId);
}
