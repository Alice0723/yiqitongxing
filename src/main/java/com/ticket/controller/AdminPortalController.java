package com.ticket.controller;

import com.ticket.model.*;
import com.ticket.repository.*;
import com.ticket.service.EventService;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/admin")
public class AdminPortalController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private LearningCourseRepository learningCourseRepository;

    @Autowired
    private GroupPurchaseRepository groupPurchaseRepository;

    @Autowired
    private StudyReportRepository studyReportRepository;

    @Autowired
    private EventService eventService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Event> events = eventRepository.findAll();
        List<GroupPurchase> groups = groupPurchaseRepository.findAll();
        List<StudyReport> reports = studyReportRepository.findAll();
        List<User> users = userRepository.findAll();

        long approvedStudents = users.stream().filter(u -> "ROLE_USER".equals(u.getRole())).count();
        int totalTickets = events.stream().mapToInt(e -> e.getTotalTickets() == null ? 0 : e.getTotalTickets()).sum();
        int remainingTickets = events.stream().mapToInt(e -> e.getRemainingTickets() == null ? 0 : e.getRemainingTickets()).sum();
        int soldTickets = Math.max(0, totalTickets - remainingTickets);

        model.addAttribute("eventCount", events.size());
        model.addAttribute("groupCount", groups.size());
        model.addAttribute("reportCount", reports.size());
        model.addAttribute("approvedStudentCount", approvedStudents);
        model.addAttribute("soldTickets", soldTickets);
        model.addAttribute("seatRate", totalTickets == 0 ? 0 : (soldTickets * 100 / totalTickets));
        model.addAttribute("users", users);
        model.addAttribute("events", events);
        model.addAttribute("eventTagOptions", eventService.getEventTagOptions());
        return "admin/dashboard";
    }

    @PostMapping("/users/{id}/blacklist")
    public String blacklistUser(@PathVariable Long id, @RequestParam boolean blacklisted) {
        User user = userRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (user != null) {
            user.setBlacklisted(blacklisted);
            userRepository.save(user);
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/events/create")
    public String createEvent(@RequestParam String name,
                              @RequestParam String venue,
                              @RequestParam String startTime,
                              @RequestParam Integer totalTickets,
                              @RequestParam Integer price,
                              @RequestParam String description,
                              @RequestParam(required = false) List<String> tags) {
        Event event = new Event();
        event.setName(name);
        event.setVenue(venue);
        event.setStartTime(LocalDateTime.parse(startTime));
        event.setTotalTickets(totalTickets);
        event.setRemainingTickets(totalTickets);
        event.setPrice(java.math.BigDecimal.valueOf(price));
        event.setDescription(description);
        event.setTags(eventService.joinTags(tags));
        eventRepository.save(event);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/courses/create")
    public String createCourse(@RequestParam String title,
                               @RequestParam String tags,
                               @RequestParam String videoUrl,
                               @RequestParam String description) {
        LearningCourse course = new LearningCourse();
        course.setTitle(title);
        course.setTags(tags);
        course.setVideoUrl(videoUrl);
        course.setDescription(description);
        course.setCreatedAt(LocalDateTime.now());
        learningCourseRepository.save(course);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/students/import")
    public String importStudents(@RequestParam String studentNames, RedirectAttributes redirectAttributes) {
        importStudentsFromText(studentNames, "skip", redirectAttributes, null);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/students/import/advanced")
    public String importStudentsAdvanced(@RequestParam String studentNames,
                                         @RequestParam(defaultValue = "skip") String importMode,
                                         RedirectAttributes redirectAttributes,
                                         HttpSession session) {
        importStudentsFromText(studentNames, importMode, redirectAttributes, session);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/students/import/file")
    public String importStudentsFromFile(@RequestParam("file") MultipartFile file,
                                         @RequestParam(defaultValue = "skip") String importMode,
                                         RedirectAttributes redirectAttributes) {
        return importStudentsFromFile(file, importMode, redirectAttributes, null);
    }

    @PostMapping("/students/import/file/advanced")
    public String importStudentsFromFileAdvanced(@RequestParam("file") MultipartFile file,
                                                  @RequestParam(defaultValue = "skip") String importMode,
                                                  RedirectAttributes redirectAttributes,
                                                  HttpSession session) {
        return importStudentsFromFile(file, importMode, redirectAttributes, session);
    }

    private String importStudentsFromFile(MultipartFile file,
                                          String importMode,
                                          RedirectAttributes redirectAttributes,
                                          HttpSession session) {
        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("importError", "请选择要上传的CSV文件");
            return "redirect:/admin/dashboard";
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (!content.isBlank() && content.charAt(0) == '\uFEFF') {
                content = content.substring(1);
            }
            importStudentsFromText(content, importMode, redirectAttributes, session);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("importError", "文件读取失败，请确认编码为UTF-8");
        }
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/students/template.csv")
    public ResponseEntity<byte[]> downloadStudentTemplate() {
        String template = "name,class_name\n张三,高一1班\n李四,高一1班\n王五,高一2班\n";
        byte[] body = ("\uFEFF" + template).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_template.csv");
        return ResponseEntity.ok().headers(headers).body(body);
    }

    @GetMapping("/students/import/result.csv")
    public ResponseEntity<byte[]> downloadImportResult(HttpSession session) {
        Object data = session.getAttribute("lastCreatedAccounts");
        if (!(data instanceof List<?> list) || list.isEmpty()) {
            byte[] empty = ("\uFEFFname,username,password,class_name\n").getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import_result_empty.csv");
            return ResponseEntity.ok().headers(headers).body(empty);
        }

        StringBuilder sb = new StringBuilder("name,username,password,class_name\n");
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                sb.append(csvValue(String.valueOf(map.containsKey("name") ? map.get("name") : ""))).append(',')
                        .append(csvValue(String.valueOf(map.containsKey("username") ? map.get("username") : ""))).append(',')
                        .append(csvValue(String.valueOf(map.containsKey("password") ? map.get("password") : ""))).append(',')
                        .append(csvValue(String.valueOf(map.containsKey("className") ? map.get("className") : "")))
                        .append('\n');
            }
        }
        byte[] body = ("\uFEFF" + sb).getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import_result.csv");
        return ResponseEntity.ok().headers(headers).body(body);
    }

    private void importStudentsFromText(String studentNames,
                                        String importMode,
                                        RedirectAttributes redirectAttributes,
                                        HttpSession session) {
        if (studentNames == null || studentNames.isBlank()) {
            redirectAttributes.addFlashAttribute("importError", "名单为空，请至少填写一个姓名");
            return;
        }

        List<Map<String, String>> inputRows = new ArrayList<>();
        LinkedHashSet<String> dedupeKeys = new LinkedHashSet<>();
        String normalized = studentNames.replace("\r", "\n")
                .replace('?', '\n')
                .replace('?', '\n')
                .replace(';', '\n');
        for (String raw : normalized.split("\\n")) {
            String line = raw == null ? "" : raw.trim();
            if (line.isBlank()) {
                continue;
            }
            String name;
            String className = "";
            if (line.contains(",")) {
                String[] cols = line.split(",", -1);
                name = cols[0].trim();
                if (cols.length > 1) {
                    className = cols[1].trim();
                }
            } else {
                name = line;
            }
            if (name.equalsIgnoreCase("name") || name.equals("姓名")) {
                continue;
            }
            if (!name.isBlank()) {
                String key = name + "@@" + className;
                if (dedupeKeys.add(key)) {
                    Map<String, String> row = new HashMap<>();
                    row.put("name", name);
                    row.put("className", className);
                    inputRows.add(row);
                }
            }
        }

        if (inputRows.isEmpty()) {
            redirectAttributes.addFlashAttribute("importError", "名单为空，请至少填写一个姓名");
            return;
        }

        List<Map<String, String>> createdAccounts = new ArrayList<>();
        List<String> failedNames = new ArrayList<>();
        int overwrittenCount = 0;
        boolean overwrite = "overwrite".equalsIgnoreCase(importMode);

        for (Map<String, String> row : inputRows) {
            String name = row.getOrDefault("name", "");
            String className = row.getOrDefault("className", "");

            User user = userRepository.findByUsername(name).orElse(null);
            if (user != null && !overwrite) {
                failedNames.add(name + "（用户名已存在）");
                continue;
            }
            if (user != null && !"ROLE_USER".equals(user.getRole())) {
                failedNames.add(name + "（已存在且非学生账号）");
                continue;
            }

            String pinyin = toFullPinyin(name);
            String rawPassword = pinyin + "123";

            if (user == null) {
                user = new User();
            } else {
                overwrittenCount++;
            }
            user.setUsername(name);
            user.setRealName(name);
            user.setRole("ROLE_USER");
            if (!className.isBlank()) {
                user.setGrade(className);
            }
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);

            createdAccounts.add(Map.of(
                    "name", name,
                    "username", name,
                    "password", rawPassword,
                    "className", className
            ));
        }

        if (session != null) {
            session.setAttribute("lastCreatedAccounts", createdAccounts);
        }

        redirectAttributes.addFlashAttribute("createdAccounts", createdAccounts);
        redirectAttributes.addFlashAttribute("failedNames", failedNames);
        String modeText = overwrite ? "覆盖模式" : "跳过重复模式";
        redirectAttributes.addFlashAttribute("importSuccess", "导入完成" + modeText + "）：成功 " + createdAccounts.size() + " 人，覆盖 " + overwrittenCount + " 人，失败 " + failedNames.size() + " 人");
    }

    private String csvValue(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    private String toFullPinyin(String text) {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

        StringBuilder sb = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (Character.isLetterOrDigit(ch) && ch < 128) {
                sb.append(Character.toLowerCase(ch));
                continue;
            }
            try {
                String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(ch, format);
                if (pinyinArray != null && pinyinArray.length > 0) {
                    sb.append(pinyinArray[0]);
                }
            } catch (BadHanyuPinyinOutputFormatCombination ignored) {
            }
        }
        if (sb.length() == 0) {
            return "student";
        }
        return sb.toString();
    }
}
