package com.ticket.repository;

import com.ticket.model.GroupPurchase;
import com.ticket.model.GroupPurchaseMember;
import com.ticket.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupPurchaseMemberRepository extends JpaRepository<GroupPurchaseMember, Long> {
    List<GroupPurchaseMember> findByGroupPurchase(GroupPurchase groupPurchase);
    Optional<GroupPurchaseMember> findByGroupPurchaseAndUser(GroupPurchase groupPurchase, User user);
}
