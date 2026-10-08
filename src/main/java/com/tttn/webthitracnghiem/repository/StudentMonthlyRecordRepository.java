package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.StudentMonthlyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentMonthlyRecordRepository extends JpaRepository<StudentMonthlyRecord, Integer> {
    List<StudentMonthlyRecord> findByStudentIdInAndSemesterAndMonth(
            List<Integer> studentIds, Integer semester, Integer month);
}
