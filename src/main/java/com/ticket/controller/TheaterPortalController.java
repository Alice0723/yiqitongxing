package com.ticket.controller;

import com.ticket.model.Event;
import com.ticket.model.TheaterSchedule;
import com.ticket.model.User;
import com.ticket.repository.EventRepository;
import com.ticket.repository.OrderRepository;
import com.ticket.repository.TheaterScheduleRepository;
import com.ticket.repository.UserRepository;
import com.ticket.service.EventService;
import com.ticket.service.EventSeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/theater")
public class TheaterPortalController {

    @Autowired
    private TheaterScheduleRepository theaterScheduleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventSeatService eventSeatService;

    @Autowired
    private EventService eventService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User operator = userRepository.findByUsername(auth.getName()).orElse(null);
        if (operator == null || !"ROLE_THEATER".equals(operator.getRole())) {
            return "redirect:/";
        }

        List<TheaterSchedule> schedules = theaterScheduleRepository.findByTheaterNameOrderByStartTimeDesc("合作剧院");
        model.addAttribute("schedules", schedules);
        model.addAttribute("orders", orderRepository.findAll());
        model.addAttribute("seatTemplates", eventSeatService.getSeatTemplates());
        model.addAttribute("eventTagOptions", eventService.getEventTagOptions());
        return "theater/dashboard";
    }

    @PostMapping("/schedule/sync")
    public String syncSchedule(@RequestParam String eventName,
                               @RequestParam String venue,
                               @RequestParam String startTime,
                               Authentication auth) {
        User operator = userRepository.findByUsername(auth.getName()).orElse(null);
        if (operator == null || !"ROLE_THEATER".equals(operator.getRole())) {
            return "redirect:/";
        }

        TheaterSchedule schedule = new TheaterSchedule();
        schedule.setTheaterName("合作剧院");
        schedule.setEventName(eventName);
        schedule.setVenue(venue);
        schedule.setStartTime(LocalDateTime.parse(startTime));
        schedule.setStatus("SYNCED");
        theaterScheduleRepository.save(schedule);
        return "redirect:/theater/dashboard";
    }

    @PostMapping("/events/create")
    public String createEvent(@RequestParam String name,
                              @RequestParam String venue,
                              @RequestParam String startTime,
                              @RequestParam Integer price,
                              @RequestParam String description,
                              @RequestParam String seatTemplate,
                              @RequestParam(required = false) List<String> tags,
                              Authentication auth) {
        User operator = userRepository.findByUsername(auth.getName()).orElse(null);
        if (operator == null || !"ROLE_THEATER".equals(operator.getRole())) {
            return "redirect:/";
        }

        int capacity = eventSeatService.getTemplateCapacity(seatTemplate);
        if (capacity <= 0) {
            return "redirect:/theater/dashboard";
        }

        Event event = new Event();
        event.setName(name);
        event.setVenue(venue);
        event.setStartTime(LocalDateTime.parse(startTime));
        event.setPrice(BigDecimal.valueOf(price));
        event.setTotalTickets(capacity);
        event.setRemainingTickets(capacity);
        event.setDescription(description);
        event.setTags(eventService.joinTags(tags));
        eventRepository.save(event);

        eventSeatService.initializeSeatsForEventByTemplate(event, seatTemplate);
        return "redirect:/theater/dashboard";
    }
}
