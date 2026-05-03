package com.ticket.model;

import jakarta.persistence.*;

@Entity
@Table(name = "classroom_group")
public class ClassroomGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String teacherUsername;
    private String className;
    private Integer studentCount;
    private Double completionRate;

    public ClassroomGroup() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTeacherUsername() { return teacherUsername; }
    public void setTeacherUsername(String teacherUsername) { this.teacherUsername = teacherUsername; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public Integer getStudentCount() { return studentCount; }
    public void setStudentCount(Integer studentCount) { this.studentCount = studentCount; }
    public Double getCompletionRate() { return completionRate; }
    public void setCompletionRate(Double completionRate) { this.completionRate = completionRate; }
}

