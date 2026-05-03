package com.ticket.controller;

import com.ticket.model.*;
import com.ticket.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Controller
public class EducationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private StudyReportRepository studyReportRepository;

    @Autowired
    private PointRecordRepository pointRecordRepository;

    @Autowired
    private LearningCourseRepository learningCourseRepository;

    @Autowired
    private AppNotificationRepository appNotificationRepository;

    @Autowired
    private HomeworkRepository homeworkRepository;

    private static final String REPORT_UPLOAD_DIR = "uploads/reports/";

    @GetMapping("/growth")
    public String growth(Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        boolean studentUser = "ROLE_USER".equals(user.getRole());

        List<StudyReport> reports = studentUser ? studyReportRepository.findByUserOrderBySubmittedAtDesc(user) : List.of();
        List<PointRecord> pointRecords = studentUser ? pointRecordRepository.findByUserOrderByCreatedAtDesc(user) : List.of();
        List<AppNotification> notifications = appNotificationRepository.findByUserOrderByCreatedAtDesc(user);
        List<LearningCourse> recommendedCourses = learningCourseRepository.findAll();
        List<Homework> homeworks = studentUser ? homeworkRepository.findByStatusAndDueAtAfterOrderByDueAtAsc("OPEN", LocalDateTime.now()) : List.of();
        if (recommendedCourses.size() > 5) {
            recommendedCourses = recommendedCourses.subList(0, 5);
        }

        model.addAttribute("user", user);
        model.addAttribute("reports", reports);
        model.addAttribute("pointRecords", pointRecords);
        model.addAttribute("notifications", notifications);
        model.addAttribute("recommendedCourses", recommendedCourses);
        model.addAttribute("homeworks", homeworks);
        model.addAttribute("studentUser", studentUser);
        return "growth";
    }

    @PostMapping("/growth/report/submit")
    public String submitReport(@RequestParam Long homeworkId,
                               @RequestParam String title,
                               @RequestParam String content,
                               @RequestParam(required = false) MultipartFile attachment,
                               Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }
        if (!"ROLE_USER".equals(user.getRole())) {
            return "redirect:/growth";
        }

        Homework homework = homeworkRepository.findById(Objects.requireNonNull(homeworkId)).orElse(null);
        if (homework == null || !"OPEN".equals(homework.getStatus())) {
            return "redirect:/growth";
        }

        StudyReport report = new StudyReport();
        report.setUser(user);
        report.setEvent(homework.getEvent());
        report.setHomework(homework);
        report.setTitle(title);
        report.setContent(content);
        report.setStatus("SUBMITTED");
        report.setSubmittedAt(LocalDateTime.now());

        if (attachment != null && !attachment.isEmpty()) {
            try {
                File dir = new File(REPORT_UPLOAD_DIR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                String filename = UUID.randomUUID() + "_" + attachment.getOriginalFilename();
                String path = REPORT_UPLOAD_DIR + filename;
                attachment.transferTo(new File(path));
                report.setAttachmentPath(path);
            } catch (Exception ignored) {
            }
        }
        studyReportRepository.save(report);

        user.setPoints((user.getPoints() == null ? 0 : user.getPoints()) + 10);
        PointRecord pointRecord = new PointRecord();
        pointRecord.setUser(user);
        pointRecord.setType("EARN");
        pointRecord.setPoints(10);
        pointRecord.setDescription("提交观演报告奖励");
        pointRecord.setCreatedAt(LocalDateTime.now());
        pointRecordRepository.save(pointRecord);

        AppNotification notification = new AppNotification();
        notification.setUser(user);
        notification.setType("REPORT");
        notification.setTitle("作业提交成功");
        notification.setContent("你的观演报告已提交，等待教师批改");
        notification.setReadFlag(false);
        notification.setCreatedAt(LocalDateTime.now());
        appNotificationRepository.save(notification);

        return "redirect:/growth";
    }
}
