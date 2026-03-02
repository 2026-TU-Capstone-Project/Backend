package com.example.Capstone_project.repository;

import com.example.Capstone_project.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 해당 사용자의 최근 메시지 최대 20개 (최신 순).
     * 서비스에서 역순으로 정렬해 오래된 순으로 contents 구성.
     */
    List<ChatMessage> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
