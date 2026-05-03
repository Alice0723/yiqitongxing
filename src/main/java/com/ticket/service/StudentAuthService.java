package com.ticket.service;

import com.ticket.model.StudentAuth;
import com.ticket.model.User;
import com.ticket.repository.StudentAuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class StudentAuthService {

    @Autowired
    private StudentAuthRepository studentAuthRepository;

    // 申请学生认证
    @Transactional
    public StudentAuth submitAuth(User user, String realName, String schoolName, String grade, String studentNo, String certificatePath) {
        // 检查是否已有申�?
        Optional<StudentAuth> existing = studentAuthRepository.findByUser(user);
        if (existing.isPresent()) {
            // 如果之前被拒绝，允许重新申请
            StudentAuth auth = existing.get();
            if ("REJECTED".equals(auth.getStatus())) {
                auth.setStatus("PENDING");
                auth.setRealName(realName);
                auth.setSchoolName(schoolName);
                auth.setGrade(grade);
                auth.setStudentNo(studentNo);
                auth.setCertificatePath(certificatePath);
                auth.setApplyTime(LocalDateTime.now());
                auth.setRemarks(null);
                auth.setReviewTime(null);
                auth.setReviewedBy(null);
                user.setRealName(realName);
                user.setSchoolName(schoolName);
                user.setGrade(grade);
                return studentAuthRepository.save(auth);
            }
            return null; // 已有审核中或已通过的申�?
        }
        
        user.setRealName(realName);
        user.setSchoolName(schoolName);
        user.setGrade(grade);
        StudentAuth auth = new StudentAuth(user, realName, schoolName, grade, studentNo, certificatePath);
        return studentAuthRepository.save(auth);
    }

    // 获取用户的认证申�?
    public Optional<StudentAuth> getUserAuth(User user) {
        return studentAuthRepository.findByUser(user);
    }

    // 获取待审核申请列�?
    public List<StudentAuth> getPendingApplications() {
        return studentAuthRepository.findByStatusOrderByApplyTimeAsc("PENDING");
    }

    // 审核通过
    @Transactional
    public boolean approveAuth(Long authId, String reviewedBy, String remarks) {
        Optional<StudentAuth> opt = studentAuthRepository.findById(Objects.requireNonNull(authId));
        if (opt.isPresent()) {
            StudentAuth auth = opt.get();
            auth.setStatus("APPROVED");
            auth.setReviewedBy(reviewedBy);
            auth.setRemarks(remarks);
            auth.setReviewTime(LocalDateTime.now());
            User user = auth.getUser();
            user.setCredits((user.getCredits() == null ? 0 : user.getCredits()) + 2);
            studentAuthRepository.save(auth);
            return true;
        }
        return false;
    }

    // 审核拒绝
    @Transactional
    public boolean rejectAuth(Long authId, String reviewedBy, String remarks) {
        Optional<StudentAuth> opt = studentAuthRepository.findById(Objects.requireNonNull(authId));
        if (opt.isPresent()) {
            StudentAuth auth = opt.get();
            auth.setStatus("REJECTED");
            auth.setReviewedBy(reviewedBy);
            auth.setRemarks(remarks);
            auth.setReviewTime(LocalDateTime.now());
            studentAuthRepository.save(auth);
            return true;
        }
        return false;
    }
}
