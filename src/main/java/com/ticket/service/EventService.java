package com.ticket.service;

import com.ticket.model.Event;
import com.ticket.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class EventService {

    public static final List<String> EVENT_TAG_OPTIONS = List.of(
            "戏曲", "交响乐", "话剧", "舞剧", "音乐", "亲子", "电竞", "国风", "思政融合"
    );

    @Autowired
    private EventRepository eventRepository;

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> getEventById(Long id) {
        return eventRepository.findById(Objects.requireNonNull(id));
    }

    public void saveEvent(Event event) {
        eventRepository.save(Objects.requireNonNull(event));
    }

    public List<String> getEventTagOptions() {
        return EVENT_TAG_OPTIONS;
    }

    public String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag != null) {
                String trimmed = tag.trim();
                if (!trimmed.isEmpty()) {
                    unique.add(trimmed);
                }
            }
        }
        return String.join(",", unique);
    }

    public List<String> splitTags(String tagsText) {
        if (tagsText == null || tagsText.isBlank()) {
            return List.of();
        }
        String[] arr = tagsText.split(",");
        List<String> tags = new ArrayList<>();
        for (String raw : arr) {
            String trimmed = raw == null ? "" : raw.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return tags;
    }

    // 扣减票数（用于支付成功时�?    
    public boolean reduceRemainingTickets(Long eventId, int quantity) {
        Optional<Event> opt = eventRepository.findById(Objects.requireNonNull(eventId));
        if (opt.isPresent()) {
            Event event = opt.get();
            if (event.getRemainingTickets() >= quantity) {
                event.setRemainingTickets(event.getRemainingTickets() - quantity);
                eventRepository.save(event);
                return true;
            }
        }
        return false;
    }

    // 回补票数（用于退票成功时�?    
    public boolean increaseRemainingTickets(Long eventId, int quantity) {
        Optional<Event> opt = eventRepository.findById(Objects.requireNonNull(eventId));
        if (opt.isPresent()) {
            Event event = opt.get();
            event.setRemainingTickets(event.getRemainingTickets() + quantity);
            eventRepository.save(event);
            return true;
        }
        return false;
    }
    
}
