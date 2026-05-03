package com.ticket.repository;

import com.ticket.model.GroupPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupPurchaseRepository extends JpaRepository<GroupPurchase, Long> {
    List<GroupPurchase> findByStatusOrderByCreatedAtDesc(String status);
}
