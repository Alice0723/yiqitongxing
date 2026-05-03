package com.ticket.controller;

import com.ticket.model.ClassroomGroup;
import com.ticket.model.Homework;
import com.ticket.model.StudyReport;
import com.ticket.model.User;
import com.ticket.repository.ClassroomGroupRepository;
import com.ticket.repository.EventRepository;
import com.ticket.repository.HomeworkRepository;
import com.ticket.repository.StudyReportRepository;
import com.ticket.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/teacher")
public class TeacherPortalController {

    @Autowired
    private ClassroomGroupRepository classroomGroupRepository;

    @Autowired
    private StudyReportRepository studyReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private EventRepository eventRepository;

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User teacher = userRepository.findByUsername(auth.getName()).orElse(null);
        if (teacher == null || !"ROLE_TEACHER".equals(teacher.getRole())) {
            return "redirect:/";
        }

        List<ClassroomGroup> classes = classroomGroupRepository.findByTeacherUsername(teacher.getUsername());
        List<StudyReport> pendingReports = studyReportRepository.findByStatusOrderBySubmittedAtAsc("SUBMITTED");
        List<Homework> homeworks = homeworkRepository.findByTeacherUsernameOrderByDueAtDesc(teacher.getUsername());
        model.addAttribute("classes", classes);
        model.addAttribute("pendingReports", pendingReports);
        model.addAttribute("homeworks", homeworks);
        model.addAttribute("events", eventRepository.findAll());
        return "teacher/dashboard";
    }

    @PostMapping("/homework/create")
    public String createHomework(@RequestParam String title,
                                 @RequestParam String requirements,
                                 @RequestParam String dueAt,
                                 @RequestParam Long eventId,
                                 @RequestParam Integer creditReward,
                                 @RequestParam Long classroomId,
                                 Authentication auth) {
        Homework homework = new Homework();
        homework.setTeacherUsername(auth.getName());
        homework.setTitle(title);
        homework.setRequirements(requirements);
        homework.setDueAt(LocalDateTime.parse(dueAt));
        homework.setEvent(eventRepository.findById(Objects.requireNonNull(eventId)).orElseThrow());
        homework.setCreditReward(Math.max(0, creditReward == null ? 0 : creditReward));
        homework.setClassroom(classroomGroupRepository.findById(Objects.requireNonNull(classroomId)).orElseThrow());
        homework.setStatus("OPEN");
        homeworkRepository.save(homework);
        return "redirect:/teacher/dashboard";
    }

    @PostMapping("/classes/create")
    public String createClass(@RequestParam String className,
                              @RequestParam Integer studentCount,
                              Authentication auth) {
        ClassroomGroup group = new ClassroomGroup();
        group.setTeacherUsername(auth.getName());
        group.setClassName(className);
        group.setStudentCount(studentCount);
        group.setCompletionRate(0.0);
        classroomGroupRepository.save(group);
        return "redirect:/teacher/dashboard";
    }

    @PostMapping("/reports/{id}/grade")
    public String gradeReport(@PathVariable Long id,
                              @RequestParam Integer score,
                              @RequestParam(required = false) String comment) {
        StudyReport report = studyReportRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (report != null && "SUBMITTED".equals(report.getStatus())) {
            report.setScore(score);
            report.setTeacherComment(comment);
            report.setStatus("GRADED");
            report.setGradedAt(LocalDateTime.now());
            studyReportRepository.save(report);

            User student = report.getUser();
            Homework homework = report.getHomework();
            if (student != null && "ROLE_USER".equals(student.getRole()) && homework != null) {
                int currentCredits = student.getCredits() == null ? 0 : student.getCredits();
                int reward = homework.getCreditReward() == null ? 0 : Math.max(0, homework.getCreditReward());
                student.setCredits(currentCredits + reward);
                userRepository.save(student);
            }
        }
        return "redirect:/teacher/dashboard";
    }
}
