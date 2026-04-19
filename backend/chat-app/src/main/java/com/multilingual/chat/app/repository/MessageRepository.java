package com.multilingual.chat.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.multilingual.chat.app.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            WHERE (m.sender.id = :user1Id AND m.receiver.id = :user2Id)
               OR (m.sender.id = :user2Id AND m.receiver.id = :user1Id)
            ORDER BY m.timestamp ASC
            """)
    List<Message> findChatHistory(Long user1Id, Long user2Id);

}
