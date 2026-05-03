package com.ticket.repository;

import com.ticket.model.ClassroomGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassroomGroupRepository extends JpaRepository<ClassroomGroup, Long> {
    List<ClassroomGroup> findByTeacherUsername(String teacherUsername);
}
