package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.SchoolCurriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchoolCurriculumRepository extends JpaRepository<SchoolCurriculum, Integer> {
    boolean existsBySchool_IdAndSubjectClasses_Id(Integer schoolId, int subjectClassId);

    void deleteBySubjectClasses_Id(int subjectClassId);
}
