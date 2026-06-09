package com.multilingual.chat.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.multilingual.chat.app.entity.Message;
import com.multilingual.chat.app.entity.User;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("""
            SELECT m FROM Message m
            WHERE (m.sender.id = :user1Id AND m.receiver.id = :user2Id)
               OR (m.sender.id = :user2Id AND m.receiver.id = :user1Id)
            ORDER BY m.timestamp ASC
            """)
    List<Message> findChatHistory(Long user1Id, Long user2Id);

    /**
     * Returns the latest message exchanged between the current user and each other participant.
     * Used to populate the conversation list in the sidebar.
     *
     * For each unique conversation partner, we want only the most recent message.
     * The subquery finds the max timestamp per conversation pair, then we join back
     * to get the full Message row for that timestamp.
     */
    @Query("""
            SELECT m FROM Message m
            WHERE (m.sender = :me OR m.receiver = :me)
            AND m.timestamp = (
                SELECT MAX(m2.timestamp) FROM Message m2
                WHERE (m2.sender = :me AND m2.receiver = m.receiver)
                   OR (m2.receiver = :me AND m2.sender = m.sender)
                   OR (m2.sender = :me AND m2.receiver = m.sender)
                   OR (m2.receiver = :me AND m2.sender = m.receiver)
            )
            ORDER BY m.timestamp DESC
            """)
    List<Message> findLatestMessagePerConversation(User me);

    @Query(value = """
            SELECT DISTINCT CASE WHEN sender_id = :userId THEN receiver_id ELSE sender_id END
            FROM messages
            WHERE sender_id = :userId OR receiver_id = :userId
            """, nativeQuery = true)
    List<Long> findConversationPartnerIds(@Param("userId") Long userId);

}
