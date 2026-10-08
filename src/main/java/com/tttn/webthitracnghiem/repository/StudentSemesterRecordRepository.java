package com.tttn.webthitracnghiem.repository;

import com.tttn.webthitracnghiem.model.StudentSemesterRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentSemesterRecordRepository extends JpaRepository<StudentSemesterRecord, Integer> {
    List<StudentSemesterRecord> findByStudentIdInAndSemester(List<Integer> studentIds, Integer semester);
    Optional<StudentSemesterRecord> findByStudentIdAndSemester(Integer studentId, Integer semester);
}
