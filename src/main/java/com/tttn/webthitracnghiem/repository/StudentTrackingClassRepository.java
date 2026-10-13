package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentTrackingClassRepository extends JpaRepository<StudentTrackingClass, Integer> {
    List<StudentTrackingClass> findAllByOrderBySchoolYearDescClassNameAsc();

    Optional<StudentTrackingClass> findByClassNameIgnoreCaseAndSchoolYear(String className, String schoolYear);

    List<StudentTrackingClass> findBySchoolId(Integer schoolId);

    List<StudentTrackingClass> findBySchoolIdOrderBySchoolYearDescClassNameAsc(Integer schoolId);

    List<StudentTrackingClass> findBySchoolIdAndGradeOrderByClassNameAsc(Integer schoolId, Integer grade);

    List<StudentTrackingClass> findByHomeroomTeacherIdOrderBySchoolYearDescClassNameAsc(String teacherId);

    @Query("SELECT DISTINCT c FROM StudentTrackingClass c " +
           "LEFT JOIN c.assignments a " +
           "WHERE c.homeroomTeacher.id = :teacherId OR a.teacher.id = :teacherId " +
           "ORDER BY c.schoolYear DESC, c.className ASC")
    List<StudentTrackingClass> findClassesByTeacherInvolved(@Param("teacherId") String teacherId);
}
