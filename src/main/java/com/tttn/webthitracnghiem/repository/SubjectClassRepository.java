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
    @Query(nativeQuery = true, value = "SELECT * FROM subject_classes WHERE class_id = :id")
    Page<SubjectClasses> findByClass(int id, Pageable pageable);

    @Query(nativeQuery = true, value = "SELECT * FROM subject_classes WHERE class_id = :id")
    List<SubjectClasses> findByClassId(int id);

    @Query(nativeQuery = true, value = "SELECT * FROM subject_classes WHERE class_id = :classId AND subject_id = :subjectId")
    Optional<SubjectClasses> findByClassesAndSubject(@Param("classId") int classId, @Param("subjectId") int subjectId);

    @Query(nativeQuery = true, value = "SELECT * FROM subject_classes WHERE subject_id = :id ORDER BY class_id")
    List<SubjectClasses> findBySubject(@Param("id") int id);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND (sc.school.id = :schoolId OR sc.school IS NULL)")
    List<SubjectClasses> findByClassIdAndSchoolOrGlobal(@Param("classId") int classId, @Param("schoolId") Integer schoolId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.school.id = :schoolId")
    List<SubjectClasses> findByClassIdAndSchoolId(@Param("classId") int classId, @Param("schoolId") Integer schoolId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.school IS NULL")
    List<SubjectClasses> findByClassIdAndSchoolIsNull(@Param("classId") int classId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.subject.id = :subjectId AND sc.school.id = :schoolId")
    Optional<SubjectClasses> findByClassAndSubjectAndSchool(@Param("classId") int classId, @Param("subjectId") int subjectId, @Param("schoolId") Integer schoolId);

    @Query("SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND sc.subject.id = :subjectId AND sc.school IS NULL")
    Optional<SubjectClasses> findByClassAndSubjectAndSchoolIsNull(@Param("classId") int classId, @Param("subjectId") int subjectId);

    @Query(value = "SELECT sc FROM SubjectClasses sc WHERE sc.classes.id = :classId AND (sc.school.id = :schoolId OR sc.school IS NULL)",
            countQuery = "SELECT COUNT(sc) FROM SubjectClasses sc WHERE sc.classes.id = :classId AND (sc.school.id = :schoolId OR sc.school IS NULL)")
    Page<SubjectClasses> findByClassAndSchoolOrGlobal(@Param("classId") int classId, @Param("schoolId") Integer schoolId, Pageable pageable);
}
