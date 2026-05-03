package com.ticket.repository;

import com.ticket.model.LearningCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningCourseRepository extends JpaRepository<LearningCourse, Long> {
    List<LearningCourse> findByTagsContainingIgnoreCase(String tag);
}
