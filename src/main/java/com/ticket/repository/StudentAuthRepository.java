package com.ticket.repository;

import com.ticket.model.StudentAuth;
import com.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StudentAuthRepository extends JpaRepository<StudentAuth, Long> {
    Optional<StudentAuth> findByUser(User user);
    Optional<StudentAuth> findByUserAndStatus(User user, String status);
    List<StudentAuth> findByStatus(String status);
    List<StudentAuth> findByStatusOrderByApplyTimeAsc(String status);
}
