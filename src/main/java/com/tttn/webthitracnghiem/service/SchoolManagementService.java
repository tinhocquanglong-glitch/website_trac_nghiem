package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.repository.SchoolRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Deletes schools for the system admin. A school that still has accounts or
 * classes is refused instead of cascading, so one confirmation can never wipe
 * a school's teachers, students and grade books; such schools should be locked.
 */
@Service
public class SchoolManagementService {
    private static final List<String> CONTENT_WITH_OPTIONAL_SCHOOL = List.of("Exam", "Question", "News", "Document");

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
        long accountCount = userRepository.countBySchoolId(schoolId);
        long classCount = classRepository.countBySchoolId(schoolId);
        if (accountCount > 0 || classCount > 0) {
            throw new IllegalStateException("Không thể xóa trường \"" + school.getName() + "\" vì vẫn còn "
                    + accountCount + " tài khoản và " + classCount + " lớp. "
                    + "Hãy xóa hoặc chuyển các tài khoản, lớp trước, hoặc dùng chức năng Khóa trường.");
        }
        // Exams, questions, news and documents stay available without a school.
        for (String entity : CONTENT_WITH_OPTIONAL_SCHOOL) {
            entityManager.createQuery("UPDATE " + entity + " e SET e.school = NULL WHERE e.school.id = :schoolId")
                    .setParameter("schoolId", schoolId)
                    .executeUpdate();
        }
        // The school's chat rooms only make sense for that school.
        entityManager.createQuery("DELETE FROM ChatMessage m WHERE m.room.id IN "
                        + "(SELECT r.id FROM ChatRoom r WHERE r.school.id = :schoolId)")
                .setParameter("schoolId", schoolId)
                .executeUpdate();
        entityManager.createQuery("DELETE FROM ChatRoom r WHERE r.school.id = :schoolId")
                .setParameter("schoolId", schoolId)
                .executeUpdate();
        schoolRepository.delete(school);
        return school;
    }
}
