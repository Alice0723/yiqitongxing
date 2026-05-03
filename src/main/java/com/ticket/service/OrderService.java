package com.ticket.service;

import com.ticket.dto.OrderRequest;
import com.ticket.model.Event;
import com.ticket.model.Order;
import com.ticket.model.PointRecord;
import com.ticket.model.User;
import com.ticket.repository.OrderRepository;
import com.ticket.repository.PointRecordRepository;
import com.ticket.repository.StudentAuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventSeatService eventSeatService;

    @Autowired
    private StudentAuthRepository studentAuthRepository;

    @Autowired
    private PointRecordRepository pointRecordRepository;

    // 创建订单（状�?= PENDING�?    @Transactional
    public Order createOrder(User user, OrderRequest request) {
        Event event = eventService.getEventById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        List<String> seatCodes = request.getSeatCodes() == null ? Collections.emptyList() : request.getSeatCodes();
        if (seatCodes.isEmpty()) {
            throw new RuntimeException("请选择座位");
        }
        if (request.getQuantity() == null || request.getQuantity() != seatCodes.size()) {
            throw new RuntimeException("购票数量与选座数量不一致");
        }
        if (event.getRemainingTickets() < seatCodes.size()) {
            throw new RuntimeException("余票不足");
        }
        Order order = new Order();
        order.setUser(user);
        order.setEvent(event);
        order.setQuantity(seatCodes.size());
        order.setSeatCodes(String.join(",", seatCodes));
        boolean studentVerified = studentAuthRepository.findByUserAndStatus(user, "APPROVED").isPresent();
        BigDecimal unitPrice = studentVerified ? event.getPrice().multiply(new BigDecimal("0.7")) : event.getPrice();
        order.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(seatCodes.size())));
        order.setStatus("PENDING");
        order.setCreateTime(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        boolean reserved = eventSeatService.reserveSeats(event, savedOrder, seatCodes);
        if (!reserved) {
            throw new RuntimeException("座位已被占用，请重新选择");
        }

        event.setRemainingTickets(event.getRemainingTickets() - seatCodes.size());
        eventService.saveEvent(event);
        return savedOrder;
    }

    // 支付订单：确认座位，更新状�?    @Transactional
    public boolean payOrder(Long orderId) {
        eventSeatService.releaseExpiredSeatLocks();
        Order order = orderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!"PENDING".equals(order.getStatus())) {
            return false;
        }
        long remaining = eventSeatService.getRemainingLockSeconds(order);
        if (remaining <= 0) {
            expirePendingOrder(order);
            return false;
        }
        boolean success = eventSeatService.markSeatsPaid(order);
        if (!success) {
            return false;
        }
        order.setStatus("PAID");
        orderRepository.save(order);

        User user = order.getUser();
        if ("ROLE_USER".equals(user.getRole())) {
            int earnPoints = Math.max(1, order.getQuantity() * 5);
            user.setPoints((user.getPoints() == null ? 0 : user.getPoints()) + earnPoints);
            PointRecord pointRecord = new PointRecord();
            pointRecord.setUser(user);
            pointRecord.setType("EARN");
            pointRecord.setPoints(earnPoints);
            pointRecord.setDescription("购票支付奖励");
            pointRecord.setCreatedAt(LocalDateTime.now());
            pointRecordRepository.save(pointRecord);
        }
        return true;
    }

    // 退票：仅允许已支付订单，成功后回补票数并更新状�?    @Transactional
    public boolean refundOrder(Long orderId) {
        Order order = orderRepository.findById(Objects.requireNonNull(orderId))
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (!"PAID".equals(order.getStatus())) {
            return false;
        }
        Event event = order.getEvent();
        boolean seatsReleased = eventSeatService.releaseSeats(order);
        boolean success = eventService.increaseRemainingTickets(event.getId(), order.getQuantity());
        if (seatsReleased && success) {
            order.setStatus("REFUNDED");
            orderRepository.save(order);

            User user = order.getUser();
            if ("ROLE_USER".equals(user.getRole())) {
                int deduction = Math.max(1, order.getQuantity() * 5);
                int currentPoints = user.getPoints() == null ? 0 : user.getPoints();
                int deducted = Math.min(currentPoints, deduction);
                user.setPoints(currentPoints - deducted);

                PointRecord pointRecord = new PointRecord();
                pointRecord.setUser(user);
                pointRecord.setType("SPEND");
                pointRecord.setPoints(deducted);
                pointRecord.setDescription("退票扣除积分");
                pointRecord.setCreatedAt(LocalDateTime.now());
                pointRecordRepository.save(pointRecord);
            }
            return true;
        }
        return false;
    }

    @Transactional
    public boolean expirePendingOrder(Order order) {
        if (order == null || !"PENDING".equals(order.getStatus())) {
            return false;
        }
        Event event = order.getEvent();
        boolean seatsReleased = eventSeatService.releaseSeats(order);
        boolean ticketsReturned = eventService.increaseRemainingTickets(event.getId(), order.getQuantity());
        if (seatsReleased && ticketsReturned) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
            return true;
        }
        return false;
    }

    @Transactional
    public long getOrderLockRemainingSeconds(Long orderId) {
        Order order = getOrderById(orderId);
        if (order == null || !"PENDING".equals(order.getStatus())) {
            return 0;
        }
        return eventSeatService.getRemainingLockSeconds(order);
    }

    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUserOrderByCreateTimeDesc(user);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(Objects.requireNonNull(id)).orElse(null);
    }
}
