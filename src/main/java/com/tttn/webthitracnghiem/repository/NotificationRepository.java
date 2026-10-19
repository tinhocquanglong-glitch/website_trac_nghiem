package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    boolean existsByRecipientIdAndExamId(String recipientId, Integer examId);

    @Modifying
    @Query("delete from Notification n where n.recipient.id = :userId")
    void deleteByRecipientId(@Param("userId") String userId);
}
