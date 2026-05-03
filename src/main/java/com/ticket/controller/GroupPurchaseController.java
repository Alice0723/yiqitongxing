package com.ticket.controller;

import com.ticket.model.GroupPurchase;
import com.ticket.model.GroupPurchaseMember;
import com.ticket.model.User;
import com.ticket.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Controller
public class GroupPurchaseController {

    @Autowired
    private GroupPurchaseRepository groupPurchaseRepository;

    @Autowired
    private GroupPurchaseMemberRepository groupPurchaseMemberRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/group-purchase")
    public String listGroupPurchase(Model model) {
        model.addAttribute("groups", groupPurchaseRepository.findByStatusOrderByCreatedAtDesc("OPEN"));
        model.addAttribute("events", eventRepository.findAll());
        return "group-purchase";
    }

    @PostMapping("/group-purchase/create")
    public String createGroup(@RequestParam Long eventId, @RequestParam(defaultValue = "30") Integer targetSize, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        GroupPurchase group = new GroupPurchase();
        group.setEvent(eventRepository.findById(Objects.requireNonNull(eventId)).orElseThrow());
        group.setInitiator(user);
        group.setTargetSize(targetSize);
        group.setCurrentSize(1);
        group.setStatus("OPEN");
        group.setCreatedAt(LocalDateTime.now());
        GroupPurchase saved = groupPurchaseRepository.save(group);

        GroupPurchaseMember member = new GroupPurchaseMember();
        member.setGroupPurchase(saved);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        groupPurchaseMemberRepository.save(member);
        return "redirect:/group-purchase";
    }

    @PostMapping("/group-purchase/{id}/join")
    public String joinGroup(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        GroupPurchase group = groupPurchaseRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (group == null || !"OPEN".equals(group.getStatus())) {
            return "redirect:/group-purchase";
        }

        if (groupPurchaseMemberRepository.findByGroupPurchaseAndUser(group, user).isPresent()) {
            return "redirect:/group-purchase";
        }

        GroupPurchaseMember member = new GroupPurchaseMember();
        member.setGroupPurchase(group);
        member.setUser(user);
        member.setJoinedAt(LocalDateTime.now());
        groupPurchaseMemberRepository.save(member);

        group.setCurrentSize(group.getCurrentSize() + 1);
        if (group.getCurrentSize() >= group.getTargetSize()) {
            group.setStatus("SUCCESS");
        }
        groupPurchaseRepository.save(group);
        return "redirect:/group-purchase";
    }

    @GetMapping("/group-purchase/{id}")
    public String groupDetail(@PathVariable Long id, Model model) {
        GroupPurchase group = groupPurchaseRepository.findById(Objects.requireNonNull(id)).orElseThrow();
        List<GroupPurchaseMember> members = groupPurchaseMemberRepository.findByGroupPurchase(group);
        model.addAttribute("group", group);
        model.addAttribute("members", members);
        return "group-purchase-detail";
    }
}
