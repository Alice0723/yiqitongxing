package com.ticket.repository;

import com.ticket.model.PointRecord;
import com.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointRecordRepository extends JpaRepository<PointRecord, Long> {
    List<PointRecord> findByUserOrderByCreatedAtDesc(User user);
}
