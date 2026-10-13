package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    @Query(value = "select count(id) from user;", nativeQuery = true)
    Integer findByTotalUser();

    @Query(value = "SELECT full_name FROM user order by create_date DESC LIMIT 1", nativeQuery = true)
    String findByNewUser();

    @Query(value = "select pass_word from user where id =:id", nativeQuery = true)
    String findByPass(@Param("id") String id);

    Optional<User> findByEmail(String email);

    @Query(value = "SELECT * FROM user WHERE id LIKE %:name% or address LIKE %:name% or email LIKE %:name% or" +
            " full_name LIKE %:name% or phone_number LIKE %:name%", nativeQuery = true)
    Page<User> search(String name, Pageable pageable);

    @Query(value = "SELECT * FROM user WHERE school_id = :schoolId AND (id LIKE %:name% or address LIKE %:name% or email LIKE %:name% or" +
            " full_name LIKE %:name% or phone_number LIKE %:name%)", nativeQuery = true)
    Page<User> searchBySchool(@Param("schoolId") int schoolId, @Param("name") String name, Pageable pageable);

    Page<User> findBySchoolId(Integer schoolId, Pageable pageable);

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
                "WHERE r.role_name = 'ROLE_USER' AND u.school_id = :schoolId " +
                "ORDER BY u.full_name ASC")
    List<User> findStudentsBySchool(@Param("schoolId") int schoolId);

    // Lấy học sinh tự do chưa có lớp (class_id IS NULL) thuộc trường của giáo viên hoặc thí sinh tự do (school_id = 1)
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_USER' AND u.class_id IS NULL " +
                "AND (u.school_id = :schoolId OR u.school_id = 1 OR u.school_id IS NULL) " +
                "ORDER BY u.full_name ASC")
    List<User> findUnassignedStudentsBySchool(@Param("schoolId") int schoolId);

    // Lấy tất cả học sinh chưa có lớp trên toàn hệ thống
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_USER' AND u.class_id IS NULL " +
                "ORDER BY u.full_name ASC")
    List<User> findAllUnassignedStudents();

    // Lấy giáo viên (ROLE_TEACHER) của một trường
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_TEACHER' AND u.school_id = :schoolId " +
                "ORDER BY u.full_name ASC")
    List<User> findTeachersBySchool(@Param("schoolId") int schoolId);

    // Lấy tất cả giáo viên cho Super Admin
    @Query(nativeQuery = true,
        value = "SELECT u.* FROM user u " +
                "JOIN user_role ur ON u.id = ur.username " +
                "JOIN role r ON ur.role_id = r.id " +
                "WHERE r.role_name = 'ROLE_TEACHER' " +
                "ORDER BY u.full_name ASC")
    List<User> findAllTeachers();
}
