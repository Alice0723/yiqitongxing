package com.ticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_seats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "seatCode"})
})
public class EventSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    private String seatCode;
    private String status; // AVAILABLE, RESERVED, PAID

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private LocalDateTime reservedTime;

    public EventSeat() {}

    public EventSeat(Event event, String seatCode) {
        this.event = event;
        this.seatCode = seatCode;
        this.status = "AVAILABLE";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public String getSeatCode() { return seatCode; }
    public void setSeatCode(String seatCode) { this.seatCode = seatCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public LocalDateTime getReservedTime() { return reservedTime; }
    public void setReservedTime(LocalDateTime reservedTime) { this.reservedTime = reservedTime; }
}

