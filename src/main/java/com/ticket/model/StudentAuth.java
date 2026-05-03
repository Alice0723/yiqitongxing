package com.ticket.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_auth")
public class StudentAuth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String realName;
    private String schoolName;
    private String grade;
    private String studentNo;
    private String certificatePath;  // 瀛︾敓璇佺収鐗囪矾寰?
    private String status;           // PENDING, APPROVED, REJECTED
    private String remarks;          // 瀹℃牳澶囨敞
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;
    private String reviewedBy;       // 瀹℃牳浜虹敤鎴峰�?

    // constructors
    public StudentAuth() {}

    public StudentAuth(User user, String realName, String schoolName, String grade, String studentNo, String certificatePath) {
        this.user = user;
        this.realName = realName;
        this.schoolName = schoolName;
        this.grade = grade;
        this.studentNo = studentNo;
        this.certificatePath = certificatePath;
        this.status = "PENDING";
        this.applyTime = LocalDateTime.now();
    }

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getSchoolName() { return schoolName; }
    public void setSchoolName(String schoolName) { this.schoolName = schoolName; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getCertificatePath() { return certificatePath; }
    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
    public LocalDateTime getReviewTime() { return reviewTime; }
    public void setReviewTime(LocalDateTime reviewTime) { this.reviewTime = reviewTime; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }
}

