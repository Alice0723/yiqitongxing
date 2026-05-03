package com.ticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "homework")
public class Homework {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teacherUsername;
    private String title;

    @Column(length = 2000)
    private String requirements;

    private LocalDateTime dueAt;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "classroom_id")
    private ClassroomGroup classroom;

    private Integer creditReward = 1;

    private String status; // OPEN, CLOSED

    public Homework() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTeacherUsername() { return teacherUsername; }
    public void setTeacherUsername(String teacherUsername) { this.teacherUsername = teacherUsername; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
    public LocalDateTime getDueAt() { return dueAt; }
    public void setDueAt(LocalDateTime dueAt) { this.dueAt = dueAt; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
        public ClassroomGroup getClassroom() { return classroom; }
        public void setClassroom(ClassroomGroup classroom) { this.classroom = classroom; }
    public Integer getCreditReward() { return creditReward; }
    public void setCreditReward(Integer creditReward) { this.creditReward = creditReward; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

