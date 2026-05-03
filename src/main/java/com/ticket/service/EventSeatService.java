package com.ticket.service;

import com.ticket.model.Event;
import com.ticket.model.EventSeat;
import com.ticket.model.Order;
import com.ticket.repository.EventSeatRepository;
import com.ticket.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventSeatService {

    public static final long LOCK_MINUTES = 5;

    @Autowired
    private EventSeatRepository eventSeatRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EventService eventService;

    private static final List<SeatTemplate> SEAT_TEMPLATES = List.of(
            new SeatTemplate("MAIN_HALL", "大剧场（24�?x 20座）", 24, 20),
            new SeatTemplate("MUSIC_HALL", "音乐厅（20�?x 16座）", 20, 16),
            new SeatTemplate("BLACK_BOX", "黑匣子剧场（12�?x 10座）", 12, 10)
    );

    public List<SeatTemplate> getSeatTemplates() {
        return SEAT_TEMPLATES;
    }

    public int getTemplateCapacity(String templateKey) {
        SeatTemplate template = getTemplate(templateKey);
        return template == null ? 0 : template.capacity();
    }

    public void initializeSeatsForEvent(Event event) {
        if (!eventSeatRepository.findByEventOrderBySeatCodeAsc(event).isEmpty()) {
            return;
        }

        List<String> seatCodes = generateSeatCodes(event.getTotalTickets(), 10);
        List<EventSeat> seats = new ArrayList<>();
        for (String seatCode : seatCodes) {
            seats.add(new EventSeat(event, seatCode));
        }
        eventSeatRepository.saveAll(seats);
    }

    public void initializeSeatsForEventByTemplate(Event event, String templateKey) {
        if (!eventSeatRepository.findByEventOrderBySeatCodeAsc(event).isEmpty()) {
            return;
        }

        SeatTemplate template = getTemplate(templateKey);
        if (template == null) {
            initializeSeatsForEvent(event);
            return;
        }

        int totalTickets = event.getTotalTickets() == null ? template.capacity() : event.getTotalTickets();
        List<String> seatCodes = generateSeatCodes(totalTickets, template.seatsPerRow());
        List<EventSeat> seats = new ArrayList<>();
        for (String seatCode : seatCodes) {
            seats.add(new EventSeat(event, seatCode));
        }
        eventSeatRepository.saveAll(seats);
    }

    public List<String> getAvailableSeatCodes(Event event) {
        return eventSeatRepository.findByEventAndStatusOrderBySeatCodeAsc(event, "AVAILABLE")
                .stream()
                .map(EventSeat::getSeatCode)
                .collect(Collectors.toList());
    }

    public List<EventSeat> getSeatsByEvent(Event event) {
        return eventSeatRepository.findByEventOrderBySeatCodeAsc(event);
    }

    public List<EventSeat> getSeatsByOrder(Order order) {
        return eventSeatRepository.findByOrder(order);
    }

    @Transactional
    public boolean reserveSeats(Event event, Order order, Collection<String> seatCodes) {
        if (seatCodes == null || seatCodes.isEmpty()) {
            return false;
        }

        Set<String> uniqueSeatCodes = new HashSet<>(seatCodes);
        if (uniqueSeatCodes.size() != seatCodes.size()) {
            return false;
        }

        List<EventSeat> seats = eventSeatRepository.findByEventAndSeatCodeIn(event, seatCodes);
        if (seats.size() != seatCodes.size()) {
            return false;
        }

        for (EventSeat seat : seats) {
            if (!"AVAILABLE".equals(seat.getStatus())) {
                return false;
            }
        }

        for (EventSeat seat : seats) {
            seat.setStatus("RESERVED");
            seat.setOrder(order);
            seat.setReservedTime(LocalDateTime.now());
        }
        eventSeatRepository.saveAll(seats);
        return true;
    }

    @Transactional
    public boolean markSeatsPaid(Order order) {
        List<EventSeat> seats = eventSeatRepository.findByOrder(order);
        if (seats.isEmpty()) {
            return false;
        }
        for (EventSeat seat : seats) {
            seat.setStatus("PAID");
        }
        eventSeatRepository.saveAll(seats);
        return true;
    }

    @Transactional
    public boolean releaseSeats(Order order) {
        List<EventSeat> seats = eventSeatRepository.findByOrder(order);
        if (seats.isEmpty()) {
            return false;
        }
        for (EventSeat seat : seats) {
            seat.setStatus("AVAILABLE");
            seat.setOrder(null);
            seat.setReservedTime(null);
        }
        eventSeatRepository.saveAll(seats);
        return true;
    }

    public long getRemainingLockSeconds(Order order) {
        List<EventSeat> seats = eventSeatRepository.findByOrder(order);
        if (seats.isEmpty()) {
            return 0;
        }
        LocalDateTime firstReserved = seats.stream()
                .map(EventSeat::getReservedTime)
                .filter(t -> t != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
        if (firstReserved == null) {
            return 0;
        }
        LocalDateTime expiresAt = firstReserved.plusMinutes(LOCK_MINUTES);
        long seconds = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
        return Math.max(seconds, 0);
    }

    @Transactional
    public void releaseExpiredSeatLocks() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(LOCK_MINUTES);
        List<EventSeat> expiredSeats = eventSeatRepository.findByStatusAndReservedTimeBefore("RESERVED", cutoff);
        if (expiredSeats.isEmpty()) {
            return;
        }

        Map<Order, List<EventSeat>> orderSeats = new HashMap<>();
        for (EventSeat seat : expiredSeats) {
            if (seat.getOrder() != null) {
                orderSeats.computeIfAbsent(seat.getOrder(), k -> new ArrayList<>()).add(seat);
            }
        }

        for (Map.Entry<Order, List<EventSeat>> entry : orderSeats.entrySet()) {
            Order order = entry.getKey();
            List<EventSeat> seats = entry.getValue();
            if ("PENDING".equals(order.getStatus())) {
                Event event = order.getEvent();
                event.setRemainingTickets((event.getRemainingTickets() == null ? 0 : event.getRemainingTickets()) + seats.size());
                eventService.saveEvent(event);
                order.setStatus("CANCELLED");
                orderRepository.save(order);
            }
            for (EventSeat seat : seats) {
                seat.setStatus("AVAILABLE");
                seat.setOrder(null);
                seat.setReservedTime(null);
            }
        }
        eventSeatRepository.saveAll(expiredSeats);
    }

    @Scheduled(fixedDelay = 60000)
    public void scheduledReleaseExpiredSeatLocks() {
        releaseExpiredSeatLocks();
    }

    private List<String> generateSeatCodes(Integer totalTickets, int seatsPerRow) {
        if (totalTickets == null || totalTickets <= 0) {
            return Collections.emptyList();
        }

        List<String> seatCodes = new ArrayList<>();
        int safeSeatsPerRow = seatsPerRow <= 0 ? 10 : seatsPerRow;
        int rowCount = (int) Math.ceil(totalTickets / (double) seatsPerRow);
        for (int row = 0; row < rowCount; row++) {
            String rowLabel = toSeatRowLabel(row);
            for (int seat = 1; seat <= safeSeatsPerRow; seat++) {
                seatCodes.add(rowLabel + seat);
                if (seatCodes.size() >= totalTickets) {
                    return seatCodes;
                }
            }
        }
        return seatCodes;
    }

    private SeatTemplate getTemplate(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) {
            return null;
        }
        return SEAT_TEMPLATES.stream()
                .filter(t -> t.key().equals(templateKey))
                .findFirst()
                .orElse(null);
    }

    // 支持 A..Z, AA..AZ... 的排号，避免超过26排时出现非字母字符�?
    private String toSeatRowLabel(int rowIndex) {
        int number = rowIndex + 1;
        StringBuilder sb = new StringBuilder();
        while (number > 0) {
            int remainder = (number - 1) % 26;
            sb.insert(0, (char) ('A' + remainder));
            number = (number - 1) / 26;
        }
        return sb.toString();
    }

    public record SeatTemplate(String key, String label, int rowCount, int seatsPerRow) {
        public int capacity() {
            return rowCount * seatsPerRow;
        }
    }
}
