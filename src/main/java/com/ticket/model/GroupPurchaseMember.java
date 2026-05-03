package com.ticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_purchase_member", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"group_purchase_id", "user_id"})
})
public class GroupPurchaseMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "group_purchase_id")
    private GroupPurchase groupPurchase;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime joinedAt;

    public GroupPurchaseMember() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public GroupPurchase getGroupPurchase() { return groupPurchase; }
    public void setGroupPurchase(GroupPurchase groupPurchase) { this.groupPurchase = groupPurchase; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}

