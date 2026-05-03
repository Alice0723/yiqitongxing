package com.ticket.repository;

import com.ticket.model.Homework;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {
    List<Homework> findByTeacherUsernameOrderByDueAtDesc(String teacherUsername);
    List<Homework> findByStatusAndDueAtAfterOrderByDueAtAsc(String status, LocalDateTime dueAt);
}
