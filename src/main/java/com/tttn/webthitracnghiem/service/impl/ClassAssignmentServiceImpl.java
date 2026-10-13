package com.tttn.webthitracnghiem.service.impl;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;
import com.tttn.webthitracnghiem.repository.ClassSubjectAssignmentRepository;
import com.tttn.webthitracnghiem.repository.StudentTrackingClassRepository;
import com.tttn.webthitracnghiem.repository.SubjectRepository;
import com.tttn.webthitracnghiem.repository.UserRepository;
import com.tttn.webthitracnghiem.service.IClassAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClassAssignmentServiceImpl implements IClassAssignmentService {

    @Autowired
    private ClassSubjectAssignmentRepository assignmentRepository;

    @Autowired
    private StudentTrackingClassRepository classRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassSubjectAssignment> findByClassId(Integer classId) {
        return assignmentRepository.findByTrackingClassId(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSubjectAssignment> findByTeacherId(String teacherId) {
        return assignmentRepository.findByTeacherId(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassSubjectAssignment> findBySchoolId(Integer schoolId) {
        return assignmentRepository.findBySchoolId(schoolId);
    }

    @Override
    @Transactional
    public ClassSubjectAssignment assignTeacher(Integer classId, Integer subjectId, String teacherId) {
        StudentTrackingClass trackingClass = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học"));
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy môn học"));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giáo viên"));

        Optional<ClassSubjectAssignment> existing = assignmentRepository
                .findByTrackingClassIdAndSubjectId(classId, subjectId);

        ClassSubjectAssignment assignment;
        if (existing.isPresent()) {
            assignment = existing.get();
            assignment.setTeacher(teacher);
        } else {
            assignment = new ClassSubjectAssignment(trackingClass, subject, teacher);
        }
        return assignmentRepository.save(assignment);
    }

    @Override
    @Transactional
    public void removeAssignment(Integer assignmentId) {
        assignmentRepository.deleteById(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeacherAssignedToClass(Integer classId, String teacherId) {
        return assignmentRepository.existsByTrackingClassIdAndTeacherId(classId, teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTeacherAssignedToClassAndSubject(Integer classId, Integer subjectId, String teacherId) {
        return assignmentRepository.existsByTrackingClassIdAndSubjectIdAndTeacherId(classId, subjectId, teacherId);
    }
}
