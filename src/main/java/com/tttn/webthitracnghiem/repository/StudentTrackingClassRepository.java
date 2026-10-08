package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentTrackingClassRepository extends JpaRepository<StudentTrackingClass, Integer> {
    List<StudentTrackingClass> findAllByOrderBySchoolYearDescClassNameAsc();
    Optional<StudentTrackingClass> findByClassNameIgnoreCaseAndSchoolYear(String className, String schoolYear);
}
