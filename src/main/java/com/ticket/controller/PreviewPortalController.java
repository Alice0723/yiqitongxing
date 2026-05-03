package com.ticket.controller;

import com.ticket.model.*;
import com.ticket.repository.*;
import com.ticket.service.EventSeatService;
import com.ticket.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PreviewPortalController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventSeatService eventSeatService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private GroupPurchaseRepository groupPurchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentAuthRepository studentAuthRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StudyReportRepository studyReportRepository;

    @Autowired
    private PointRecordRepository pointRecordRepository;

    @Autowired
    private LearningCourseRepository learningCourseRepository;

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private AppNotificationRepository appNotificationRepository;

    @Autowired
    private ClassroomGroupRepository classroomGroupRepository;

    @GetMapping("/schedule")
    public String schedule(Authentication auth, Model model) {
        eventSeatService.releaseExpiredSeatLocks();
        addCurrentUserContext(auth, model);

        List<Event> events = sortByStartTime(eventService.getAllEvents());
        model.addAttribute("events", events);
        model.addAttribute("tagOptions", eventService.getEventTagOptions());
        model.addAttribute("featuredEvents", events.stream().limit(6).collect(Collectors.toList()));
        model.addAttribute("openGroupCount", groupPurchaseRepository.findByStatusOrderByCreatedAtDesc("OPEN").size());
        model.addAttribute("eventCount", events.size());
        return "schedule";
    }

    @GetMapping("/groupbuy")
    public String groupbuy(Authentication auth, Model model) {
        addCurrentUserContext(auth, model);
        model.addAttribute("groups", groupPurchaseRepository.findByStatusOrderByCreatedAtDesc("OPEN"));
        model.addAttribute("events", sortByStartTime(eventRepository.findAll()));
        model.addAttribute("openGroupCount", groupPurchaseRepository.findByStatusOrderByCreatedAtDesc("OPEN").size());
        return "groupbuy";
    }

    @GetMapping("/points")
    public String points(Authentication auth, Model model) {
        User user = requireUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        boolean studentUser = "ROLE_USER".equals(user.getRole());
        addCurrentUserContext(auth, model);
        model.addAttribute("studentUser", studentUser);
        model.addAttribute("user", user);
        model.addAttribute("reports", studentUser ? studyReportRepository.findByUserOrderBySubmittedAtDesc(user) : List.of());
        model.addAttribute("pointRecords", studentUser ? pointRecordRepository.findByUserOrderByCreatedAtDesc(user) : List.of());
        model.addAttribute("notifications", appNotificationRepository.findByUserOrderByCreatedAtDesc(user));
        model.addAttribute("recommendedCourses", truncateCourses(learningCourseRepository.findAll()));
        model.addAttribute("rewards", buildRewardCards());
        model.addAttribute("earnedPoints", user.getPoints() == null ? 0 : user.getPoints());
        return "points";
    }

    @GetMapping("/ai-recommend")
    public String aiRecommend(Authentication auth, Model model) {
        addCurrentUserContext(auth, model);
        User currentUser = requireUser(auth);
        List<Event> events = eventService.getAllEvents();
        Map<String, Integer> tagWeights = currentUser == null ? Map.of() : buildPreferredTagWeights(currentUser);
        boolean personalized = !tagWeights.isEmpty();
        List<Event> recommended = personalized ? sortByPreference(events, tagWeights) : sortByHotTags(events);
        List<String> aiTags = personalized ? preferredTags(tagWeights) : hotTags(events);
        model.addAttribute("recommendedEvents", recommended.stream().limit(6).collect(Collectors.toList()));
        model.addAttribute("aiTags", aiTags);
        model.addAttribute("recommendMode", personalized ? "personalized" : "coldStart");
        model.addAttribute("currentInterestTags", currentUser == null ? List.of() : eventService.splitTags(currentUser.getInterestTags()));
        model.addAttribute("eventCount", events.size());
        return "ai-recommend";
    }

    @GetMapping("/identity")
    public String identity(Authentication auth, Model model) {
        User user = requireUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        if (!"ROLE_USER".equals(user.getRole())) {
            return "redirect:/profile";
        }

        addCurrentUserContext(auth, model);
        model.addAttribute("studentAuth", studentAuthRepository.findByUserAndStatus(user, "APPROVED").orElseGet(() ->
                studentAuthRepository.findByUser(user).orElse(null)));
        model.addAttribute("pendingAuth", studentAuthRepository.findByUserAndStatus(user, "PENDING").orElse(null));
        return "identity";
    }

    @GetMapping("/student")
    public String student(Authentication auth, Model model) {
        User user = requireUser(auth);
        if (user == null) {
            return "redirect:/login";
        }

        boolean studentUser = "ROLE_USER".equals(user.getRole());
        addCurrentUserContext(auth, model);
        model.addAttribute("studentUser", studentUser);
        model.addAttribute("user", user);
        model.addAttribute("studentAuth", studentAuthRepository.findByUser(user).orElse(null));
        model.addAttribute("pointRecords", studentUser ? pointRecordRepository.findByUserOrderByCreatedAtDesc(user) : List.of());
        model.addAttribute("reports", studentUser ? studyReportRepository.findByUserOrderBySubmittedAtDesc(user) : List.of());
        model.addAttribute("homeworks", studentUser ? homeworkRepository.findByStatusAndDueAtAfterOrderByDueAtAsc("OPEN", LocalDateTime.now()) : List.of());
        model.addAttribute("recommendedCourses", truncateCourses(learningCourseRepository.findAll()));
        model.addAttribute("notifications", appNotificationRepository.findByUserOrderByCreatedAtDesc(user));
        return "student";
    }

    @GetMapping("/teacher")
    public String teacher(Authentication auth, Model model) {
        User user = requireUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        if (!"ROLE_TEACHER".equals(user.getRole())) {
            return "redirect:/events";
        }

        addCurrentUserContext(auth, model);
        model.addAttribute("classes", classroomGroupRepository.findByTeacherUsername(user.getUsername()));
        model.addAttribute("pendingReports", studyReportRepository.findByStatusOrderBySubmittedAtAsc("SUBMITTED"));
        model.addAttribute("homeworks", homeworkRepository.findByTeacherUsernameOrderByDueAtDesc(user.getUsername()));
        model.addAttribute("events", sortByStartTime(eventRepository.findAll()));
        return "teacher";
    }

    @GetMapping("/analytics")
    public String analytics(Authentication auth, Model model) {
        User user = requireUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        if (!"ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/events";
        }

        List<Event> events = eventRepository.findAll();
        List<GroupPurchase> groups = groupPurchaseRepository.findAll();
        List<StudyReport> reports = studyReportRepository.findAll();
        List<User> users = userRepository.findAll();

        long approvedStudents = users.stream().filter(u -> "ROLE_USER".equals(u.getRole())).count();
        int totalTickets = events.stream().mapToInt(e -> e.getTotalTickets() == null ? 0 : e.getTotalTickets()).sum();
        int remainingTickets = events.stream().mapToInt(e -> e.getRemainingTickets() == null ? 0 : e.getRemainingTickets()).sum();
        int soldTickets = Math.max(0, totalTickets - remainingTickets);

        addCurrentUserContext(auth, model);
        model.addAttribute("eventCount", events.size());
        model.addAttribute("groupCount", groups.size());
        model.addAttribute("reportCount", reports.size());
        model.addAttribute("approvedStudentCount", approvedStudents);
        model.addAttribute("soldTickets", soldTickets);
        model.addAttribute("seatRate", totalTickets == 0 ? 0 : (soldTickets * 100 / totalTickets));
        model.addAttribute("users", users);
        model.addAttribute("events", sortByStartTime(events));
        model.addAttribute("groups", groups);
        model.addAttribute("reports", reports);
        model.addAttribute("eventTagOptions", eventService.getEventTagOptions());
        return "analytics";
    }

    private void addCurrentUserContext(Authentication auth, Model model) {
        User currentUser = requireUser(auth);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("studentVerified", currentUser != null && studentAuthRepository.findByUserAndStatus(currentUser, "APPROVED").isPresent());
    }

    private User requireUser(Authentication auth) {
        if (auth == null) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    private List<Event> sortByStartTime(List<Event> events) {
        return events.stream()
                .sorted(Comparator.comparing(Event::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private List<Event> sortByStartTimeAsc(List<Event> events) {
        return sortByStartTime(events);
    }

    private List<Event> sortByPreference(List<Event> events, Map<String, Integer> tagWeights) {
        List<Event> sorted = new ArrayList<>(events);
        sorted.sort((a, b) -> {
            int s1 = calculateScore(a, tagWeights);
            int s2 = calculateScore(b, tagWeights);
            if (s1 != s2) {
                return Integer.compare(s2, s1);
            }
            return Comparator.comparing(Event::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())).compare(a, b);
        });
        return sorted;
    }

    private List<Event> sortByHotTags(List<Event> events) {
        Map<String, Integer> hotTagWeights = buildHotTagWeights(events);
        List<Event> sorted = new ArrayList<>(events);
        sorted.sort((a, b) -> {
            int s1 = calculateScore(a, hotTagWeights);
            int s2 = calculateScore(b, hotTagWeights);
            if (s1 != s2) {
                return Integer.compare(s2, s1);
            }
            return Comparator.comparing(Event::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())).compare(a, b);
        });
        return sorted;
    }

    private Map<String, Integer> buildPreferredTagWeights(User user) {
        Map<String, Integer> tagWeight = new HashMap<>();
        for (String tag : eventService.splitTags(user.getInterestTags())) {
            tagWeight.put(tag, tagWeight.getOrDefault(tag, 0) + 5);
        }

        List<Order> paidOrders = orderRepository.findByUserAndStatusOrderByCreateTimeDesc(user, "PAID");
        for (Order order : paidOrders) {
            Event event = order.getEvent();
            if (event == null || event.getTags() == null) {
                continue;
            }
            for (String tag : eventService.splitTags(event.getTags())) {
                tagWeight.put(tag, tagWeight.getOrDefault(tag, 0) + 2);
            }
        }
        return tagWeight;
    }

    private int calculateScore(Event event, Map<String, Integer> tagWeights) {
        if (event == null || tagWeights == null || tagWeights.isEmpty()) {
            return 0;
        }
        int score = 0;
        for (String tag : eventService.splitTags(event.getTags())) {
            score += tagWeights.getOrDefault(tag, 0);
        }
        return score;
    }

    private Map<String, Integer> buildHotTagWeights(List<Event> events) {
        Map<String, Integer> hotTagWeights = new HashMap<>();
        for (Event event : events) {
            for (String tag : eventService.splitTags(event.getTags())) {
                hotTagWeights.put(tag, hotTagWeights.getOrDefault(tag, 0) + 1);
            }
        }
        return hotTagWeights;
    }

    private List<String> preferredTags(Map<String, Integer> tagWeights) {
        return tagWeights.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<String> hotTags(List<Event> events) {
        return buildHotTagWeights(events).entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(5)
                .collect(Collectors.toList());
    }

    private List<LearningCourse> truncateCourses(List<LearningCourse> courses) {
        return courses.size() > 6 ? courses.subList(0, 6) : courses;
    }

    private List<Map<String, String>> buildRewardCards() {
        List<Map<String, String>> rewards = new ArrayList<>();
        rewards.add(Map.of("name", "限定周边", "points", "10分", "hint", "徽章、票夹、贴纸"));
        rewards.add(Map.of("name", "演出票务", "points", "20分", "hint", "学生专属抽签资格"));
        rewards.add(Map.of("name", "美育课程", "points", "15分", "hint", "课程抵扣或兑换券"));
        return rewards;
    }
}