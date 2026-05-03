package com.ticket.controller;

import com.ticket.dto.OrderRequest;
import com.ticket.model.Order;
import com.ticket.model.User;
import com.ticket.repository.UserRepository;
import com.ticket.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    // 涓嬪崟
    @PostMapping("/order/create")
    public String createOrder(@Valid @ModelAttribute OrderRequest orderRequest, Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        Order order = orderService.createOrder(user, orderRequest);
        return "redirect:/order/" + order.getId() + "/pay";
    }

    // 鏀粯椤甸潰
    @GetMapping("/order/{id}/pay")
    public String showPayPage(@PathVariable Long id, Model model, Authentication auth) {
        Order order = orderService.getOrderById(id);
        if (order == null || !order.getUser().getUsername().equals(auth.getName())) {
            return "redirect:/events";
        }
        long remainingSeconds = orderService.getOrderLockRemainingSeconds(id);
        if ("PENDING".equals(order.getStatus()) && remainingSeconds <= 0) {
            orderService.expirePendingOrder(order);
            return "redirect:/my-orders?timeout";
        }
        model.addAttribute("order", order);
        model.addAttribute("seatCodes", order.getSeatCodes() == null ? List.of() : Arrays.stream(order.getSeatCodes().split(",")).collect(Collectors.toList()));
        model.addAttribute("remainingSeconds", remainingSeconds);
        return "pay";
    }

    // 纭鏀粯
    @PostMapping("/order/{id}/pay")
    public String confirmPay(@PathVariable Long id, Authentication auth) {
        Order order = orderService.getOrderById(id);
        if (order != null && order.getUser().getUsername().equals(auth.getName()) && "PENDING".equals(order.getStatus())) {
            boolean success = orderService.payOrder(id);
            if (success) {
                return "redirect:/my-orders?paid";
            }
            return "redirect:/my-orders?timeout";
        }
        return "redirect:/my-orders?error";
    }

    // 退票
    @PostMapping("/order/{id}/refund")
    public String refundOrder(@PathVariable Long id, Authentication auth) {
        Order order = orderService.getOrderById(id);
        if (order != null && order.getUser().getUsername().equals(auth.getName()) && "PAID".equals(order.getStatus())) {
            boolean success = orderService.refundOrder(id);
            if (success) {
                return "redirect:/my-orders?refunded";
            }
        }
        return "redirect:/my-orders?refundError";
    }

    // 鎴戠殑璁㈠崟
    @GetMapping("/my-orders")
    public String myOrders(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        List<Order> orders = orderService.getOrdersByUser(user);
        model.addAttribute("orders", orders);
        return "my-orders";
    }
}
