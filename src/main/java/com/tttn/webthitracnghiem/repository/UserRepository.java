package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    @Modifying
    @Query(value = "DELETE FROM user_role WHERE username = :userId", nativeQuery = true)
    void deleteRoleLinks(@Param("userId") String userId);

    @Modifying
    @Query(value = "DELETE FROM result WHERE username = :userId", nativeQuery = true)
    void deleteResults(@Param("userId") String userId);

    @Modifying
    @Query(value = "DELETE FROM iq WHERE username = :userId", nativeQuery = true)
    void deleteIqResults(@Param("userId") String userId);

    @Modifying
    @Query(value = "UPDATE exam SET users_id = NULL WHERE users_id = :userId", nativeQuery = true)
    void clearExamAuthors(@Param("userId") String userId);

    @Modifying
    @Query(value = "UPDATE document SET user_id = NULL WHERE user_id = :userId", nativeQuery = true)
    void clearDocumentAuthors(@Param("userId") String userId);

    @Modifying
    @Query(value = "UPDATE news SET author = NULL WHERE author = :userId", nativeQuery = true)
    void clearNewsAuthors(@Param("userId") String userId);

    @Modifying
    @Query(value = "UPDATE student_tracking_class SET homeroom_teacher_id = NULL WHERE homeroom_teacher_id = :userId", nativeQuery = true)
    void clearHomeroomTeacher(@Param("userId") String userId);

    @Modifying
    @Query(value = "UPDATE user SET class_id = NULL WHERE id = :userId", nativeQuery = true)
    void clearTrackingClass(@Param("userId") String userId);

    @Modifying
    @Query(value = "DELETE FROM class_subject_assignment WHERE teacher_id = :userId", nativeQuery = true)
    void deleteTeacherAssignments(@Param("userId") String userId);

    @Modifying
    @Query("delete from Notification n where n.recipient.id = :userId")
    void deleteStudentNotifications(@Param("userId") String userId);

    @Query(nativeQuery = true,
            value = "SELECT u.* FROM user u JOIN user_role ur ON u.id = ur.username " +
                    "JOIN role r ON ur.role_id = r.id WHERE r.role_name = 'ROLE_USER' " +
                    "AND (u.status IS NULL OR u.status = 1) " +
                    "AND u.class_id = :classId ORDER BY u.full_name ASC")
    List<User> findStudentsByClassId(@Param("classId") int classId);

    @Query(nativeQuery = true,
            value = "SELECT u.* FROM user u JOIN user_role ur ON u.id = ur.username " +
                    "JOIN role r ON ur.role_id = r.id JOIN student_tracking_class c ON u.class_id = c.id " +
                    "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) AND c.school_id = :schoolId AND c.grade = :grade " +
                    "ORDER BY u.full_name ASC")
    List<User> findStudentsBySchoolAndGrade(@Param("schoolId") int schoolId, @Param("grade") int grade);

    @Query(nativeQuery = true,
            value = "SELECT u.* FROM user u " +
                    "JOIN user_role ur ON u.id = ur.username " +
                    "JOIN role r ON ur.role_id = r.id " +
                    "JOIN student_tracking_class c ON u.class_id = c.id " +
                    "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) AND c.grade = :grade " +
                    "ORDER BY u.full_name ASC")
    List<User> findStudentsByGrade(@Param("grade") int grade);

    @Query(nativeQuery = true,
            value = "SELECT u.* FROM user u " +
                    "JOIN user_role ur ON u.id = ur.username " +
                    "JOIN role r ON ur.role_id = r.id " +
                    "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) " +
                    "ORDER BY u.full_name ASC")
    List<User> findAllStudents();

    @Query(value = "select count(id) from user where status IS NULL OR status = 1;", nativeQuery = true)
    Integer findByTotalUser();

    @Query(value = "SELECT COUNT(*) " +
            "FROM user u " +
            "JOIN user_role ur ON u.id = ur.username " +
            "JOIN role r ON ur.role_id = r.id " +
            "WHERE r.role_name = :roleName AND (u.status IS NULL OR u.status = 1)", nativeQuery = true)
    Integer countUsersByRoleName(@Param("roleName") String roleName);

    @Query(value = "SELECT full_name FROM user where status IS NULL OR status = 1 order by create_date DESC LIMIT 1", nativeQuery = true)
    String findByNewUser();

    @Query(value = "select pass_word from user where id =:id", nativeQuery = true)
    String findByPass(@Param("id") String id);

    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(u) FROM User u WHERE u.school.id = :schoolId AND (u.status IS NULL OR u.status = 1)")
    long countBySchoolId(@Param("schoolId") Integer schoolId);

    @Query("SELECT u FROM User u WHERE u.status IS NULL OR u.status = 1")
    Page<User> findAllActive(Pageable pageable);

    @Query(value = "SELECT u.* FROM user u WHERE (u.status IS NULL OR u.status = 1) AND (" +
            "CONVERT(u.id USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
            "CONVERT(u.address USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
            "CONVERT(u.email USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
            "CONVERT(u.full_name USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
            "CONVERT(u.phone_number USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci)",
            countQuery = "SELECT COUNT(*) FROM user u WHERE (u.status IS NULL OR u.status = 1) AND (" +
                    "CONVERT(u.id USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
                    "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
                    "CONVERT(u.address USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
                    "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
                    "CONVERT(u.email USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
                    "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
                    "CONVERT(u.full_name USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
                    "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
                    "CONVERT(u.phone_number USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
                    "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci)",
            nativeQuery = true)
    Page<User> search(@Param("name") String name, Pageable pageable);

    @Query(value = "SELECT u.* FROM user u WHERE u.school_id = :schoolId AND (u.status IS NULL OR u.status = 1) AND (" +
            "CONVERT(u.id USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
            "CONVERT(u.address USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
            "CONVERT(u.email USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
            "CONVERT(u.full_name USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci OR " +
            "CONVERT(u.phone_number USING utf8mb4) COLLATE utf8mb4_unicode_ci LIKE " +
            "CONCAT('%', CONVERT(:name USING utf8mb4), '%') COLLATE utf8mb4_unicode_ci)", nativeQuery = true)
    Page<User> searchBySchool(@Param("schoolId") int schoolId, @Param("name") String name, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.school.id = :schoolId AND (u.status IS NULL OR u.status = 1)")
    Page<User> findBySchoolId(@Param("schoolId") Integer schoolId, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT * FROM user WHERE create_date >= :date - interval 7 day")
    List<User> newMember(Date date);

    @Query(nativeQuery = true, value = "SELECT * FROM user JOIN user_role ON user.id = user_role.username ")
    List<User> getMember();

    Optional<User> findById(int id);

    // Lấy học sinh (ROLE_USER) của một trường
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) AND u.school_id = :schoolId " +
                "ORDER BY u.full_name ASC")
    List<User> findStudentsBySchool(@Param("schoolId") int schoolId);

    // Lấy học sinh tự do chưa có lớp (class_id IS NULL) thuộc trường của giáo viên hoặc thí sinh tự do (school_id = 1)
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) AND u.class_id IS NULL " +
                "AND (u.school_id = :schoolId OR u.school_id = 1 OR u.school_id IS NULL) " +
                "ORDER BY u.full_name ASC")
    List<User> findUnassignedStudentsBySchool(@Param("schoolId") int schoolId);

    // Lấy tất cả học sinh chưa có lớp trên toàn hệ thống
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) AND u.class_id IS NULL " +
                "ORDER BY u.full_name ASC")
    List<User> findAllUnassignedStudents();

    // Lấy giáo viên (ROLE_TEACHER) của một trường
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_TEACHER' AND (u.status IS NULL OR u.status = 1) AND u.school_id = :schoolId " +
                "ORDER BY u.full_name ASC")
    List<User> findTeachersBySchool(@Param("schoolId") int schoolId);

    // Lấy tất cả giáo viên cho Super Admin
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_TEACHER' AND (u.status IS NULL OR u.status = 1) " +
                "ORDER BY u.full_name ASC")
    List<User> findAllTeachers();

    // Lấy danh sách Quản trị viên trường / Hiệu trưởng (ROLE_SCHOOL_ADMIN)
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_SCHOOL_ADMIN' AND (u.status IS NULL OR u.status = 1) " +
                "ORDER BY u.school_id ASC, u.full_name ASC")
    List<User> findAllSchoolAdmins();

    // Lấy Quản trị viên trường / Hiệu trưởng của một trường cụ thể
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_SCHOOL_ADMIN' AND (u.status IS NULL OR u.status = 1) AND u.school_id = :schoolId " +
                "ORDER BY u.full_name ASC")
    List<User> findSchoolAdminsBySchool(@Param("schoolId") int schoolId);

    // Tìm kiếm giáo viên theo tên trong trường
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_TEACHER' AND (u.status IS NULL OR u.status = 1) AND u.school_id = :schoolId " +
                "AND (:keyword IS NULL OR :keyword = '' OR u.full_name LIKE CONCAT('%', :keyword, '%') OR u.id LIKE CONCAT('%', :keyword, '%')) " +
                "ORDER BY u.full_name ASC")
    List<User> searchTeachersBySchool(@Param("schoolId") int schoolId, @Param("keyword") String keyword);

    // Lấy học sinh theo khối trong trường
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) AND u.school_id = :schoolId " +
                "AND u.school_level = :schoolLevel " +
                "ORDER BY u.full_name ASC")
    List<User> findStudentsBySchoolAndLevel(@Param("schoolId") int schoolId, @Param("schoolLevel") int schoolLevel);

    // Tìm kiếm học sinh theo tên/lớp trong trường
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "LEFT JOIN student_tracking_class stc ON u.class_id = stc.id " +
                "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) AND u.school_id = :schoolId " +
                "AND (:keyword IS NULL OR :keyword = '' OR u.full_name LIKE CONCAT('%', :keyword, '%') OR u.id LIKE CONCAT('%', :keyword, '%') OR stc.class_name LIKE CONCAT('%', :keyword, '%')) " +
                "ORDER BY u.school_level ASC, stc.grade ASC, stc.class_name ASC, u.full_name ASC")
    List<User> searchStudentsBySchool(@Param("schoolId") int schoolId, @Param("keyword") String keyword);

    // Lấy và tìm kiếm học sinh của một lớp cụ thể
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_USER' AND (u.status IS NULL OR u.status = 1) AND u.class_id = :classId " +
                "AND (:keyword IS NULL OR :keyword = '' OR u.full_name LIKE CONCAT('%', :keyword, '%') OR u.id LIKE CONCAT('%', :keyword, '%') OR u.phone_number LIKE CONCAT('%', :keyword, '%')) " +
                "ORDER BY u.full_name ASC")
    List<User> findStudentsByClassIdAndKeyword(@Param("classId") int classId, @Param("keyword") String keyword);
}
