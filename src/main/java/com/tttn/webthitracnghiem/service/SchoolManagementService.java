package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Xóa mềm trường học cho Quản trị viên (Admin).
 * Khi xóa, trường học được đánh dấu đã xóa (deleted = true, active = false),
 * các môn học riêng do trường tạo được ẩn đi (status = 0),
 * và các tài khoản thuộc trường sẽ bị chặn đăng nhập kèm thông báo rõ ràng.
 */
@Service
public class SchoolManagementService {

    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final StudentTrackingClassRepository classRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public SchoolManagementService(SchoolRepository schoolRepository,
                                   UserRepository userRepository,
                                   StudentTrackingClassRepository classRepository) {
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.classRepository = classRepository;
    }

    @Transactional
    public School deleteSchool(Integer schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy trường học."));
        if (school.isDeleted()) {
            return school;
        }

        // 1. Đánh dấu trường đã bị xóa mềm và vô hiệu hóa hoạt động
        school.setDeleted(true);
        school.setActive(false);
        school = schoolRepository.save(school);

        // 2. Ẩn toàn bộ môn học do trường này tạo (status = 0)
        entityManager.createQuery("UPDATE Subject s SET s.status = 0 WHERE s.school.id = :schoolId")
                .setParameter("schoolId", schoolId)
                .executeUpdate();

        return school;
    }
}
