package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentTrackingClassRepository extends JpaRepository<StudentTrackingClass, Integer> {

    @Query("SELECT c FROM StudentTrackingClass c WHERE (c.status IS NULL OR c.status = 1) ORDER BY c.schoolYear DESC, c.className ASC")
    List<StudentTrackingClass> findAllByOrderBySchoolYearDescClassNameAsc();

    /** Class names are unique per school and school year; a null schoolId matches classes without a school. */
    Optional<StudentTrackingClass> findFirstBySchoolIdAndClassNameIgnoreCaseAndSchoolYear(
            Integer schoolId, String className, String schoolYear);

    @Query("SELECT c FROM StudentTrackingClass c WHERE c.school.id = :schoolId AND (c.status IS NULL OR c.status = 1)")
    List<StudentTrackingClass> findBySchoolId(@Param("schoolId") Integer schoolId);

    @Query("SELECT c FROM StudentTrackingClass c WHERE c.school.id = :schoolId AND (c.status IS NULL OR c.status = 1) ORDER BY c.schoolYear DESC, c.className ASC")
    List<StudentTrackingClass> findBySchoolIdOrderBySchoolYearDescClassNameAsc(@Param("schoolId") Integer schoolId);

    @Query("SELECT c FROM StudentTrackingClass c WHERE c.school.id = :schoolId AND c.grade = :grade AND (c.status IS NULL OR c.status = 1) ORDER BY c.className ASC")
    List<StudentTrackingClass> findBySchoolIdAndGradeOrderByClassNameAsc(@Param("schoolId") Integer schoolId, @Param("grade") Integer grade);

    @Query("SELECT c FROM StudentTrackingClass c WHERE c.school IS NULL AND (c.status IS NULL OR c.status = 1) ORDER BY c.schoolYear DESC, c.className ASC")
    List<StudentTrackingClass> findBySchoolIdIsNullOrderBySchoolYearDescClassNameAsc();

    @Query("SELECT COUNT(c) FROM StudentTrackingClass c WHERE c.school.id = :schoolId AND (c.status IS NULL OR c.status = 1)")
    long countBySchoolId(@Param("schoolId") Integer schoolId);

    @Query("SELECT COUNT(c) FROM StudentTrackingClass c WHERE c.school.id = :schoolId AND c.grade = :grade AND (c.status IS NULL OR c.status = 1)")
    long countBySchoolIdAndGrade(@Param("schoolId") Integer schoolId, @Param("grade") Integer grade);

    @Query("SELECT c FROM StudentTrackingClass c WHERE c.homeroomTeacher.id = :teacherId AND (c.status IS NULL OR c.status = 1) ORDER BY c.schoolYear DESC, c.className ASC")
    List<StudentTrackingClass> findByHomeroomTeacherIdOrderBySchoolYearDescClassNameAsc(@Param("teacherId") String teacherId);

    @Query("SELECT DISTINCT c FROM StudentTrackingClass c " +
           "LEFT JOIN c.assignments a " +
           "WHERE (c.status IS NULL OR c.status = 1) AND (c.homeroomTeacher.id = :teacherId OR a.teacher.id = :teacherId) " +
           "ORDER BY c.schoolYear DESC, c.className ASC")
    List<StudentTrackingClass> findClassesByTeacherInvolved(@Param("teacherId") String teacherId);
}
