package com.ticket.repository;

import com.ticket.model.StudyReport;
import com.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyReportRepository extends JpaRepository<StudyReport, Long> {
    List<StudyReport> findByUserOrderBySubmittedAtDesc(User user);
    List<StudyReport> findByStatusOrderBySubmittedAtAsc(String status);
}
