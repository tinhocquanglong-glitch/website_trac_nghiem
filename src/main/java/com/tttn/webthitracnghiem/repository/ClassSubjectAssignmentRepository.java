package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSubjectAssignmentRepository extends JpaRepository<ClassSubjectAssignment, Integer> {

    List<ClassSubjectAssignment> findByTrackingClassId(Integer trackingClassId);

    List<ClassSubjectAssignment> findByTeacherId(String teacherId);

    Optional<ClassSubjectAssignment> findByTrackingClassIdAndSubjectId(Integer trackingClassId, Integer subjectId);

    @Query("SELECT a FROM ClassSubjectAssignment a WHERE a.trackingClass.school.id = :schoolId")
    List<ClassSubjectAssignment> findBySchoolId(@Param("schoolId") Integer schoolId);

    boolean existsByTrackingClassIdAndTeacherId(Integer trackingClassId, String teacherId);

    boolean existsByTrackingClassIdAndSubjectIdAndTeacherId(Integer trackingClassId, Integer subjectId, String teacherId);
}
