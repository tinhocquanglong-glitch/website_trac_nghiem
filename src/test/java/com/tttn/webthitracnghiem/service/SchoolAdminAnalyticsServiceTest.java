package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.Exam;
import com.tttn.webthitracnghiem.model.Result;
import com.tttn.webthitracnghiem.model.School;
import com.tttn.webthitracnghiem.model.SchoolAdminStatistics;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository;
import com.tttn.webthitracnghiem.repository.ExamRepository;
import com.tttn.webthitracnghiem.repository.ResultRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolAdminAnalyticsServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private StudentTrackingClassRepository classRepository;
    @Mock private ClassSubjectAssignmentRepository assignmentRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private ExamRepository examRepository;
    @Mock private ResultRepository resultRepository;
    @InjectMocks private SchoolAdminAnalyticsService service;

    @Test
    void aggregatesSchoolScopedExamAndAssignmentData() {
        School school = new School();
        school.setId(3);
        StudentTrackingClass trackingClass = new StudentTrackingClass();
        trackingClass.setId(10);
        trackingClass.setGrade(6);
        trackingClass.setSchoolYear("2026-2027");
        trackingClass.setSchool(school);
        User teacher = new User();
        teacher.setId("teacher01");
        teacher.setFullName("Nguyen Van A");
        teacher.setSchool(school);
        User student = new User();
        student.setId("student01");
        student.setFullName("Tran Thi B");
        student.setSchool(school);
        student.setTrackingClass(trackingClass);
        User unassignedStudent = new User();
        unassignedStudent.setId("student02");
        unassignedStudent.setFullName("Le Thi C");
        unassignedStudent.setSchool(school);
        Subject subject = new Subject();
        subject.setId(7);
        subject.setName("Toan");
        ClassSubjectAssignment assignment = new ClassSubjectAssignment(trackingClass, subject, teacher);
        Exam exam = new Exam();
        exam.setId(20);
        exam.setNameExam("Kiem tra Toan");
        exam.setSchool(school);
        exam.setUsers(teacher);
        exam.setTargetType("CLASS");
        exam.setTargetClass(trackingClass);
        Result result = new Result();
        result.setUsers(student);
        result.setExam(exam);
        result.setMark(8);

        when(classRepository.findBySchoolId(3)).thenReturn(List.of(trackingClass));
        when(userRepository.findTeachersBySchool(3)).thenReturn(List.of(teacher));
        when(userRepository.findStudentsBySchool(3)).thenReturn(List.of(student, unassignedStudent));
        when(subjectRepository.findAll()).thenReturn(List.of(subject));
        when(assignmentRepository.findBySchoolId(3)).thenReturn(List.of(assignment));
        when(resultRepository.findAll()).thenReturn(List.of(result));
        when(examRepository.findAll()).thenReturn(List.of(exam));

        SchoolAdminStatistics statistics = service.build(school, 6, "2026-2027");

        assertThat(statistics.getStudentCount()).isEqualTo(1);
        assertThat(statistics.getClassCount()).isEqualTo(1);
        assertThat(statistics.getAssignmentCoverage()).isEqualTo(100.0);
        assertThat(statistics.getRealAverageScore()).isEqualTo(8.0);
        assertThat(statistics.getRealExamCount()).isEqualTo(1);
        assertThat(statistics.getExamActivities()).singleElement().satisfies(activity -> {
            assertThat(activity.getEligibleCount()).isEqualTo(1);
            assertThat(activity.getTakenCount()).isEqualTo(1);
            assertThat(activity.getParticipationPercent()).isEqualTo(100);
        });
    }
}
