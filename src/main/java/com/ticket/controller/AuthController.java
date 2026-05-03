package com.ticket.controller;

import com.ticket.model.User;
import com.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String register(User user, Model model, HttpServletRequest request, HttpServletResponse response) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            model.addAttribute("error", "用户名已存在");
            return "register";
        }

        String selectedRole = user.getRole();
        if (selectedRole == null || selectedRole.isBlank()) {
            model.addAttribute("error", "请选择身份");
            return "register";
        }
        if (!("ROLE_USER".equals(selectedRole) || "ROLE_TEACHER".equals(selectedRole) || "ROLE_THEATER".equals(selectedRole))) {
            model.addAttribute("error", "身份选择无效");
            return "register";
        }
        
        // 保存原始密码用于认证
        String rawPassword = user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(selectedRole);
        userRepository.save(user);
        
        // 注册成功后自动登录，并保存到 session
        UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(user.getUsername(), rawPassword);
        var authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // 重要：将认证信息持久化到 session
        SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        
        return "redirect:/events";
    }
}