package com.ticket.repository;

import com.ticket.model.AppNotification;
import com.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByUserOrderByCreatedAtDesc(User user);
}
