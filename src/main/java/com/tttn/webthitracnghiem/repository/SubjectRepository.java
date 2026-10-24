package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Integer> {
    @Query(value = "SELECT * FROM subject where subject.name like %:name%",
            countQuery = "SELECT COUNT(*) FROM subject where subject.name like %:name%",
            nativeQuery = true)
    Page<Subject> findAllByName(String name, Pageable pageable);

    @Query("SELECT s FROM Subject s WHERE s.school.id = :schoolId OR s.school IS NULL ORDER BY s.name ASC")
    List<Subject> findBySchoolIdOrGlobal(@Param("schoolId") Integer schoolId);

    @Query("SELECT s FROM Subject s WHERE s.school IS NULL ORDER BY s.name ASC")
    List<Subject> findGlobalSubjects();

    List<Subject> findBySchoolIdOrderByNameAsc(Integer schoolId);

    @Query(value = "SELECT s FROM Subject s WHERE (s.school.id = :schoolId OR s.school IS NULL) AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))",
            countQuery = "SELECT COUNT(s) FROM Subject s WHERE (s.school.id = :schoolId OR s.school IS NULL) AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Subject> findByNameAndSchoolOrGlobal(@Param("name") String name, @Param("schoolId") Integer schoolId, Pageable pageable);

    @Query(value = "SELECT s FROM Subject s WHERE s.school.id = :schoolId OR s.school IS NULL",
            countQuery = "SELECT COUNT(s) FROM Subject s WHERE s.school.id = :schoolId OR s.school IS NULL")
    Page<Subject> findAllBySchoolOrGlobal(@Param("schoolId") Integer schoolId, Pageable pageable);
}
