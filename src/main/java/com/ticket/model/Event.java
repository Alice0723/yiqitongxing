package com.ticket.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String venue;
    private LocalDateTime startTime;
    private BigDecimal price;
    private Integer totalTickets;
    private Integer remainingTickets;
    private String description;
    private String tags;

    // constructors
    public Event() {}

    public Event(String name, String venue, LocalDateTime startTime, BigDecimal price, 
                 Integer totalTickets, Integer remainingTickets, String description) {
        this.name = name;
        this.venue = venue;
        this.startTime = startTime;
        this.price = price;
        this.totalTickets = totalTickets;
        this.remainingTickets = remainingTickets;
        this.description = description;
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getTotalTickets() { return totalTickets; }
    public void setTotalTickets(Integer totalTickets) { this.totalTickets = totalTickets; }
    public Integer getRemainingTickets() { return remainingTickets; }
    public void setRemainingTickets(Integer remainingTickets) { this.remainingTickets = remainingTickets; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
}
