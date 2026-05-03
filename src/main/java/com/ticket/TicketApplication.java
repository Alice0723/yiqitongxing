package com.ticket;

import com.ticket.model.Event;
import com.ticket.model.LearningCourse;
import com.ticket.model.User;
import com.ticket.repository.ClassroomGroupRepository;
import com.ticket.repository.EventRepository;
import com.ticket.repository.LearningCourseRepository;
import com.ticket.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.ticket.service.EventSeatService;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@SpringBootApplication
@EnableScheduling
public class TicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(EventRepository eventRepository,
                                      UserRepository userRepository,
                                      PasswordEncoder passwordEncoder,
                                      EventSeatService eventSeatService,
                                      LearningCourseRepository learningCourseRepository,
                                      ClassroomGroupRepository classroomGroupRepository) {
        return args -> {
            // 初始化默认管理员
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User("admin", passwordEncoder.encode("admin123"));
                admin.setRole("ROLE_ADMIN");
                userRepository.save(admin);
                System.out.println("默认管理员已创建：用户名 admin，密码 admin123");
            }

            if (userRepository.findByUsername("teacher").isEmpty()) {
                User teacher = new User("teacher", passwordEncoder.encode("teacher123"));
                teacher.setRole("ROLE_TEACHER");
                teacher.setRealName("示例老师");
                userRepository.save(teacher);
            }

            if (userRepository.findByUsername("theater").isEmpty()) {
                User theater = new User("theater", passwordEncoder.encode("theater123"));
                theater.setRole("ROLE_THEATER");
                theater.setRealName("剧院运营");
                userRepository.save(theater);
            }

            // 初始化活动数�?            
            if (eventRepository.count() == 0) {
                eventRepository.save(new Event(
                        "周杰伦演唱会 - 上海站",
                        "上海体育场",
                        LocalDateTime.of(2026, 5, 20, 19, 30),
                        new BigDecimal("680"),
                        500,
                        500,
                        "地表最强演唱会，感受周杰伦的经典旋律"
                ));
                eventRepository.save(new Event(
                        "话剧《雷雨》",
                        "国家大剧院",
                        LocalDateTime.of(2026, 6, 10, 19, 0),
                        new BigDecimal("280"),
                        200,
                        200,
                        "曹禺经典话剧，震撼人心"
                ));
                eventRepository.save(new Event(
                        "王者荣耀职业联赛总决赛",
                        "北京凯迪拉克中心",
                        LocalDateTime.of(2026, 7, 15, 16, 0),
                        new BigDecimal("380"),
                        800,
                        800,
                        "顶尖电竞对决，现场体验激情"
                ));
            }

            eventRepository.findAll().forEach(eventSeatService::initializeSeatsForEvent);

            if (learningCourseRepository.count() == 0) {
                LearningCourse c1 = new LearningCourse();
                c1.setTitle("交响乐导赏：从听懂到热爱");
                c1.setTags("古典音乐导赏");
                c1.setVideoUrl("https://example.com/course/symphony");
                c1.setDescription("面向中学生的交响乐入门导赏课程");
                c1.setCreatedAt(LocalDateTime.now());
                learningCourseRepository.save(c1);

                LearningCourse c2 = new LearningCourse();
                c2.setTitle("戏剧里的思政表达");
                c2.setTags("思政融合,戏剧");
                c2.setVideoUrl("https://example.com/course/drama");
                c2.setDescription("结合经典剧目解读思政主题");
                c2.setCreatedAt(LocalDateTime.now());
                learningCourseRepository.save(c2);
            }
        };
    }
}
