package com.ticket.controller;

import com.ticket.model.StudentAuth;
import com.ticket.model.User;
import com.ticket.repository.UserRepository;
import com.ticket.service.EventService;
import com.ticket.service.StudentAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentAuthService studentAuthService;

    @Autowired
    private EventService eventService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    private static final String UPLOAD_DIR = "uploads/certificates/";

    // 个人中心
    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        populateProfileModel(user, model);
        return "profile";
    }

    private String profileWithCurrentModel(User user, Model model) {
        populateProfileModel(user, model);
        return "profile";
    }

    private void populateProfileModel(User user, Model model) {
        boolean studentUser = "ROLE_USER".equals(user.getRole());
        Optional<StudentAuth> studentAuth = studentUser
                ? studentAuthService.getUserAuth(user)
                : Optional.empty();
        model.addAttribute("user", user);
        model.addAttribute("studentUser", studentUser);
        model.addAttribute("studentAuth", studentAuth.orElse(null));
        model.addAttribute("eventTagOptions", eventService.getEventTagOptions());
        model.addAttribute("selectedInterestTags", new HashSet<>(eventService.splitTags(user.getInterestTags())));
    }

    @PostMapping("/profile/interests")
    public String updateInterests(@RequestParam(required = false) List<String> interestTags,
                                  Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        if (!"ROLE_USER".equals(user.getRole())) {
            return "redirect:/profile";
        }

        user.setInterestTags(eventService.joinTags(interestTags));
        userRepository.save(user);
        return "redirect:/profile";
    }

    @PostMapping("/profile/account")
    public String updateAccount(@RequestParam String currentPassword,
                                @RequestParam String newUsername,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                Authentication auth,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                Model model) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            model.addAttribute("accountError", "当前密码不正确");
            return profileWithCurrentModel(user, model);
        }

        String trimmedUsername = newUsername == null ? "" : newUsername.trim();
        if (trimmedUsername.isBlank()) {
            model.addAttribute("accountError", "用户名不能为空");
            return profileWithCurrentModel(user, model);
        }

        if (!trimmedUsername.equals(user.getUsername()) && userRepository.findByUsername(trimmedUsername).isPresent()) {
            model.addAttribute("accountError", "用户名已存在");
            return profileWithCurrentModel(user, model);
        }

        boolean passwordChanged = newPassword != null && !newPassword.isBlank();
        if (passwordChanged) {
            if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
                model.addAttribute("accountError", "两次输入的新密码不一致");
                return profileWithCurrentModel(user, model);
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        user.setUsername(trimmedUsername);
        userRepository.save(user);

        String loginPassword = passwordChanged ? newPassword : currentPassword;
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(trimmedUsername, loginPassword);
        var authentication = authenticationManager.authenticate(authToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

        return "redirect:/profile?accountSuccess=1";
    }

    // 学生认证页面
    @GetMapping("/student-auth")
    public String studentAuthPage(Authentication auth, Model model) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        if (!"ROLE_USER".equals(user.getRole())) {
            return "redirect:/profile";
        }
        
        Optional<StudentAuth> studentAuth = studentAuthService.getUserAuth(user);
        model.addAttribute("studentAuth", studentAuth.orElse(null));
        return "student-auth";
    }

    // 提交学生认证
    @PostMapping("/student-auth/submit")
    public String submitStudentAuth(@RequestParam("realName") String realName,
                                     @RequestParam("schoolName") String schoolName,
                                     @RequestParam("grade") String grade,
                                     @RequestParam("studentNo") String studentNo,
                                     @RequestParam("certificate") MultipartFile file,
                                     Authentication auth, Model model) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        if (!"ROLE_USER".equals(user.getRole())) {
            return "redirect:/profile";
        }
        
        if (file.isEmpty()) {
            model.addAttribute("error", "请选择学生证照片");
            return "student-auth";
        }
        
        try {
            // 创建上传目录
            File uploadDir = new File(UPLOAD_DIR);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            // 生成唯一文件名
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filepath = UPLOAD_DIR + filename;
            file.transferTo(new File(filepath));
            
            // 保存认证申请
            studentAuthService.submitAuth(user, realName, schoolName, grade, studentNo, filepath);
            return "redirect:/student-auth?success=申请已提交，请等待审核";
        } catch (Exception e) {
            return "redirect:/student-auth?error=上传失败，请重试";
        }

    }

    // 管理后台：待审核列表
    @GetMapping("/admin/student-auth/pending")
    public String pendingApplications(Authentication auth, Model model) {
        // 检查是否为管理员（简化处理，实际应用应有权限控制）
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/events";
        }
        
        List<StudentAuth> applications = studentAuthService.getPendingApplications();
        model.addAttribute("applications", applications);
        return "admin/student-auth-review";
    }

    // 审核通过
    @PostMapping("/admin/student-auth/{id}/approve")
    public String approveStudentAuth(@PathVariable Long id, 
                                      @RequestParam(required = false) String remarks,
                                      Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/events";
        }
        
        studentAuthService.approveAuth(id, username, remarks);
        return "redirect:/admin/student-auth/pending";
    }

    // 审核拒绝
    @PostMapping("/admin/student-auth/{id}/reject")
    public String rejectStudentAuth(@PathVariable Long id, 
                                     @RequestParam(required = false) String remarks,
                                     Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || !"ROLE_ADMIN".equals(user.getRole())) {
            return "redirect:/events";
        }
        
        studentAuthService.rejectAuth(id, username, remarks);
        return "redirect:/admin/student-auth/pending";
    }

    @PostMapping("/profile/deactivate")
    public String deactivateAccount(Authentication auth) {
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        user.setBlacklisted(true);
        userRepository.save(user);
        return "redirect:/logout";
    }
}
