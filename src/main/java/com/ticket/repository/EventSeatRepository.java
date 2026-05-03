package com.ticket.repository;

import com.ticket.model.Event;
import com.ticket.model.EventSeat;
import com.ticket.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventSeatRepository extends JpaRepository<EventSeat, Long> {
    List<EventSeat> findByEventOrderBySeatCodeAsc(Event event);
    List<EventSeat> findByEventAndStatusOrderBySeatCodeAsc(Event event, String status);
    Optional<EventSeat> findByEventAndSeatCode(Event event, String seatCode);
    List<EventSeat> findByOrder(Order order);
    List<EventSeat> findByEventAndSeatCodeIn(Event event, Collection<String> seatCodes);
    long countByEventAndStatus(Event event, String status);
    List<EventSeat> findByStatusAndReservedTimeBefore(String status, LocalDateTime time);
}
