package com.ticket.repository;

import com.ticket.model.TheaterSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TheaterScheduleRepository extends JpaRepository<TheaterSchedule, Long> {
    List<TheaterSchedule> findByTheaterNameOrderByStartTimeDesc(String theaterName);
}
