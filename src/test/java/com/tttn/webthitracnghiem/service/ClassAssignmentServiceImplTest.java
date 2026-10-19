package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.impl.ClassAssignmentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassAssignmentServiceImplTest {

    @Mock
    private ClassSubjectAssignmentRepository assignmentRepository;
    @Mock
    private StudentTrackingClassRepository classRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ClassAssignmentServiceImpl service;

    @Test
    void assignsOneTeacherToEverySelectedClass() {
        StudentTrackingClass first = new StudentTrackingClass();
        StudentTrackingClass second = new StudentTrackingClass();
        Subject subject = new Subject();
        User teacher = new User();
        when(classRepository.findById(1)).thenReturn(Optional.of(first));
        when(classRepository.findById(2)).thenReturn(Optional.of(second));
        when(subjectRepository.findById(7)).thenReturn(Optional.of(subject));
        when(userRepository.findById("teacher")).thenReturn(Optional.of(teacher));
        when(assignmentRepository.findByTrackingClassIdAndSubjectId(any(), any())).thenReturn(Optional.empty());
        when(assignmentRepository.save(any(ClassSubjectAssignment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int assigned = service.assignTeacherToClasses(List.of(1, 2), 7, "teacher");

        assertThat(assigned).isEqualTo(2);
        verify(assignmentRepository, times(2)).save(any(ClassSubjectAssignment.class));
    }

    @Test
    void removesAssignmentForAClassAndSubject() {
        ClassSubjectAssignment assignment = new ClassSubjectAssignment();
        when(assignmentRepository.findByTrackingClassIdAndSubjectId(1, 7))
                .thenReturn(Optional.of(assignment));

        service.removeAssignment(1, 7);

        verify(assignmentRepository).delete(assignment);
    }
}
