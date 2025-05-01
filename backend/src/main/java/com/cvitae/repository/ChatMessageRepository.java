package com.cvitae.repository;

import com.cvitae.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    
    List<ChatMessage> findByResumeIdOrderByCreatedAtAsc(UUID resumeId);
    
    void deleteBySessionId(String sessionId);
}
