package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.SubjectClasses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectClassRepository extends JpaRepository<SubjectClasses, Integer> {
    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.subject.status IS NULL OR sc.subject.status != 0")
    List<SubjectClasses> findAllActive();

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.subject.status IS NULL OR sc.subject.status != 0")
    Page<SubjectClasses> findAllActive(Pageable pageable);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :id AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    Page<SubjectClasses> findByClass(@Param("id") int id, Pageable pageable);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :id AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    List<SubjectClasses> findByClassId(@Param("id") int id);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.subject.id = :subjectId AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    Optional<SubjectClasses> findByClassesAndSubject(@Param("classId") int classId, @Param("subjectId") int subjectId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.subject.id = :id AND (sc.subject.status IS NULL OR sc.subject.status != 0) ORDER BY sc.classes.id")
    List<SubjectClasses> findBySubject(@Param("id") int id);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND (sc.school.id = :schoolId OR sc.school IS NULL) AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    List<SubjectClasses> findByClassIdAndSchoolOrGlobal(@Param("classId") int classId, @Param("schoolId") Integer schoolId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.school.id = :schoolId AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    List<SubjectClasses> findByClassIdAndSchoolId(@Param("classId") int classId, @Param("schoolId") Integer schoolId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.school IS NULL AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    List<SubjectClasses> findByClassIdAndSchoolIsNull(@Param("classId") int classId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.subject.id = :subjectId AND sc.school.id = :schoolId AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    Optional<SubjectClasses> findByClassAndSubjectAndSchool(@Param("classId") int classId, @Param("subjectId") int subjectId, @Param("schoolId") Integer schoolId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.subject.id = :subjectId AND sc.school IS NULL AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    Optional<SubjectClasses> findByClassAndSubjectAndSchoolIsNull(@Param("classId") int classId, @Param("subjectId") int subjectId);

    @Query(value = "SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND (sc.school.id = :schoolId OR sc.school IS NULL) AND (sc.subject.status IS NULL OR sc.subject.status != 0)",
            countQuery = "SELECT COUNT(sc) FROM SubjectClasses sc WHERE sc.classes.id = :classId AND (sc.school.id = :schoolId OR sc.school IS NULL) AND (sc.subject.status IS NULL OR sc.subject.status != 0)")
    Page<SubjectClasses> findByClassAndSchoolOrGlobal(@Param("classId") int classId, @Param("schoolId") Integer schoolId, Pageable pageable);
}
