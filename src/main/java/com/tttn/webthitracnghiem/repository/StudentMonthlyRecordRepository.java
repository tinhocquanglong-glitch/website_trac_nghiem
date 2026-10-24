package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.StudentMonthlyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentMonthlyRecordRepository extends JpaRepository<StudentMonthlyRecord, Integer> {
    /** Null teacherId and subjectId are matched with IS NULL, which selects the homeroom book. */
    List<StudentMonthlyRecord> findByStudentIdInAndSemesterAndMonthAndTeacherIdAndSubjectId(
            List<Integer> studentIds, Integer semester, Integer month, String teacherId, Integer subjectId);
}
