package com.ticket.controller;

import com.ticket.model.Event;
import com.ticket.dto.OrderRequest;
import com.ticket.model.GroupPurchase;
import com.ticket.model.Order;
import com.ticket.model.User;
import com.ticket.repository.OrderRepository;
import com.ticket.repository.GroupPurchaseRepository;
import com.ticket.repository.StudentAuthRepository;
import com.ticket.repository.UserRepository;
import com.ticket.service.EventService;
import com.ticket.service.EventSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventSeatService eventSeatService;

    @Autowired
    private GroupPurchaseRepository groupPurchaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentAuthRepository studentAuthRepository;

    @Autowired
    private OrderRepository orderRepository;

    @GetMapping("/")
        public String home(Authentication auth, Model model) {
        eventSeatService.releaseExpiredSeatLocks();

        User currentUser = null;
        boolean studentVerified = false;
        if (auth != null) {
            currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
            if (currentUser != null) {
            studentVerified = studentAuthRepository.findByUserAndStatus(currentUser, "APPROVED").isPresent();
            }
        }

        List<Event> events = eventService.getAllEvents();
        List<Event> featuredEvents = events.stream()
            .sorted(this::compareStartTimeAsc)
            .limit(5)
            .collect(Collectors.toList());
        List<Event> scheduleEvents = events.stream()
            .sorted(this::compareStartTimeAsc)
            .limit(6)
            .collect(Collectors.toList());
        List<GroupPurchase> hotGroups = groupPurchaseRepository.findByStatusOrderByCreatedAtDesc("OPEN")
            .stream()
            .limit(3)
            .collect(Collectors.toList());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("studentVerified", studentVerified);
        model.addAttribute("featuredEvents", featuredEvents);
        model.addAttribute("scheduleEvents", scheduleEvents);
        model.addAttribute("hotGroups", hotGroups);
        model.addAttribute("openGroupCount", hotGroups.size());
        model.addAttribute("eventCount", events.size());
        model.addAttribute("heroEvent", featuredEvents.isEmpty() ? null : featuredEvents.get(0));
        model.addAttribute("heroPrice", featuredEvents.isEmpty() || featuredEvents.get(0).getPrice() == null
            ? "0"
            : featuredEvents.get(0).getPrice().toPlainString());
        model.addAttribute("heroStudentPrice", featuredEvents.isEmpty() || featuredEvents.get(0).getPrice() == null
            ? "0"
            : featuredEvents.get(0).getPrice().multiply(new BigDecimal("0.7")).toPlainString());
        model.addAttribute("courseCards", buildCourseCards());
        model.addAttribute("volunteerCards", buildVolunteerCards());
        model.addAttribute("comments", buildCommentCards());
        model.addAttribute("recommendedTags", events.isEmpty() ? eventService.getEventTagOptions() : getGlobalTags(events));
        return "index";
    }

    

    @GetMapping("/event/{id}")
    public String eventDetail(@PathVariable Long id, Authentication auth, Model model) {
        eventSeatService.releaseExpiredSeatLocks();
        Event event = eventService.getEventById(id)
                .orElseThrow(() -> new RuntimeException("活动不存在"));
        User currentUser = null;
        if (auth != null) {
            currentUser = userRepository.findByUsername(auth.getName()).orElse(null);
        }
        model.addAttribute("event", event);
        model.addAttribute("orderRequest", new OrderRequest());
        List<com.ticket.model.EventSeat> seatList = eventSeatService.getSeatsByEvent(event);
        Map<String, List<com.ticket.model.EventSeat>> seatRows = seatList.stream()
                .collect(Collectors.groupingBy(
                        seat -> extractSeatRow(seat.getSeatCode()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        seatRows.values().forEach(row -> row.sort(Comparator.comparingInt(s -> extractSeatNumber(s.getSeatCode()))));

        model.addAttribute("seatList", seatList);
        model.addAttribute("seatRows", seatRows.entrySet());
        model.addAttribute("currentUser", currentUser);
        return "event-detail";
    }

    private String extractSeatRow(String seatCode) {
        StringBuilder row = new StringBuilder();
        for (char ch : seatCode.toCharArray()) {
            if (Character.isLetter(ch)) {
                row.append(ch);
            } else {
                break;
            }
        }
        return row.toString();
    }

    private int extractSeatNumber(String seatCode) {
        StringBuilder number = new StringBuilder();
        for (char ch : seatCode.toCharArray()) {
            if (Character.isDigit(ch)) {
                number.append(ch);
            }
        }
        return number.length() == 0 ? Integer.MAX_VALUE : Integer.parseInt(number.toString());
    }

    private Map<String, Integer> getPreferredTagWeights(User user) {
        Map<String, Integer> tagWeight = new HashMap<>();

        // 用户手动兴趣权重更高，体现显式偏好�?        for (String tag : eventService.splitTags(user.getInterestTags())) {
            tagWeight.put(tag, tagWeight.getOrDefault(tag, 0) + 5);
        }

        List<Order> paidOrders = orderRepository.findByUserAndStatusOrderByCreateTimeDesc(user, "PAID");
        if (paidOrders.isEmpty()) {
            return tagWeight;
        }

        for (Order order : paidOrders) {
            Event event = order.getEvent();
            if (event == null || event.getTags() == null) {
                continue;
            }
            int behaviorWeight = getBehaviorWeight(order.getCreateTime());
            for (String tag : eventService.splitTags(event.getTags())) {
                tagWeight.put(tag, tagWeight.getOrDefault(tag, 0) + behaviorWeight);
            }
        }
        return tagWeight;
    }

    private int calculateWeightedTagScore(Event event, Map<String, Integer> preferredTagWeights) {
        if (event == null || preferredTagWeights == null || preferredTagWeights.isEmpty()) {
            return 0;
        }
        List<String> eventTags = eventService.splitTags(event.getTags());
        int score = 0;
        for (String tag : eventTags) {
            score += preferredTagWeights.getOrDefault(tag, 0);
        }
        return score;
    }

    private int getBehaviorWeight(LocalDateTime createTime) {
        if (createTime == null) {
            return 1;
        }
        long days = java.time.Duration.between(createTime, LocalDateTime.now()).toDays();
        if (days <= 30) {
            return 4;
        }
        if (days <= 90) {
            return 2;
        }
        return 1;
    }

    private List<String> getGlobalTags(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        return events.stream()
                .flatMap(e -> eventService.splitTags(e.getTags()).stream())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
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

        private List<Map<String, String>> buildCourseCards() {
        List<Map<String, String>> cards = new ArrayList<>();
        cards.add(Map.of(
            "title", "古典音乐入门：从巴赫到勃拉姆斯",
            "tag", "音乐鉴赏",
            "teacher", "xxx教授",
            "info", "12课时 · 入门 · 1,280人学过",
            "price", "¥199",
            "originPrice", "¥399"
        ));
        cards.add(Map.of(
            "title", "中国传统戏曲赏析：昆曲与京剧",
            "tag", "戏曲文化",
            "teacher", "xxx教授",
            "info", "10课时 · 入门 · 856人学过",
            "price", "¥179",
            "originPrice", "¥349"
        ));
        cards.add(Map.of(
            "title", "芭蕾舞基础：优雅体态与基本功训练",
            "tag", "舞蹈",
            "teacher", "xxx教授",
            "info", "16课时 · 基础 · 642人学过",
            "price", "¥299",
            "originPrice", "¥599"
        ));
        cards.add(Map.of(
            "title", "戏剧表演工作坊：情感表达与即兴创作",
            "tag", "戏剧",
            "teacher", "xxx教授",
            "info", "8课时 · 入门 · 492人学过",
            "price", "¥159",
            "originPrice", "¥320"
        ));
        return cards;
        }

        private List<Map<String, String>> buildVolunteerCards() {
        List<Map<String, String>> cards = new ArrayList<>();
        cards.add(Map.of(
            "title", "手拉手：走进乡村学校的音乐课",
            "tag", "免费公益",
            "teacher", "上音志愿者团队",
            "info", "8课时 · 1,850人参与"
        ));
        cards.add(Map.of(
            "title", "手拉手：美术启蒙——色彩的世界",
            "tag", "免费公益",
            "teacher", "上美志愿者团队",
            "info", "6课时 · 1,230人参与"
        ));
        return cards;
        }

        private List<Map<String, String>> buildCommentCards() {
        List<Map<String, String>> cards = new ArrayList<>();
        cards.add(Map.of(
            "name", "xxx",
            "school", "上海音乐学院",
            "event", "上音歌剧院·威尔第歌剧《茶花女》",
            "content", "真的太震撼了，学生票也很超值，舞美和演唱都很出彩！",
            "score", "★★★★"
        ));
        cards.add(Map.of(
            "name", "xxx",
            "school", "复旦大学",
            "event", "上海之春国际音乐节·开幕音乐会",
            "content", "第一次现场听交响乐，被贺绿汀音乐厅的音响包围感惊到了",
            "score", "★★★★"
        ));
        cards.add(Map.of(
            "name", "xxx",
            "school", "同济大学",
            "event", "上音管弦乐团·贝多芬交响曲全集（第一场）",
            "content", "整个演出很完整，学生专属购票真的太友好了",
            "score", "★★★★"
        ));
        return cards;
        }

    private int calculateColdStartScore(Event event, Map<String, Integer> hotTagWeights) {
        if (event == null) {
            return 0;
        }
        int tagScore = 0;
        for (String tag : eventService.splitTags(event.getTags())) {
            tagScore += hotTagWeights.getOrDefault(tag, 0);
        }
        return tagScore * 3 + getUpcomingWeight(event.getStartTime());
    }

    private int getUpcomingWeight(LocalDateTime startTime) {
        if (startTime == null) {
            return 0;
        }
        long days = java.time.Duration.between(LocalDateTime.now(), startTime).toDays();
        if (days < 0) {
            return 0;
        }
        if (days <= 7) {
            return 6;
        }
        if (days <= 30) {
            return 4;
        }
        if (days <= 90) {
            return 2;
        }
        return 1;
    }

    private int compareStartTimeAsc(Event a, Event b) {
        LocalDateTime t1 = a.getStartTime();
        LocalDateTime t2 = b.getStartTime();
        if (t1 == null && t2 == null) {
            return 0;
        }
        if (t1 == null) {
            return 1;
        }
        if (t2 == null) {
            return -1;
        }
        return t1.compareTo(t2);
    }

    private int compareStartTimeDesc(Event a, Event b) {
        return compareStartTimeAsc(b, a);
    }
}

