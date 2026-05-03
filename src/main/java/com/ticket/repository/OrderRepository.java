package com.ticket.repository;

import com.ticket.model.Order;
import com.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreateTimeDesc(User user);
    List<Order> findByUserAndStatusOrderByCreateTimeDesc(User user, String status);
}
