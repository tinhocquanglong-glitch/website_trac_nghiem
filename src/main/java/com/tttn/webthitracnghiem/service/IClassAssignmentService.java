package com.tttn.webthitracnghiem.service;

import com.tttn.webthitracnghiem.model.ClassSubjectAssignment;
import com.tttn.webthitracnghiem.model.StudentTrackingClass;
import com.tttn.webthitracnghiem.model.Subject;
import com.tttn.webthitracnghiem.model.User;

import java.util.List;
import java.util.Optional;

public interface IClassAssignmentService {
    List<ClassSubjectAssignment> findByClassId(Integer classId);
    List<ClassSubjectAssignment> findByTeacherId(String teacherId);
    List<ClassSubjectAssignment> findBySchoolId(Integer schoolId);
    ClassSubjectAssignment assignTeacher(Integer classId, Integer subjectId, String teacherId);
    void removeAssignment(Integer assignmentId);
    boolean isTeacherAssignedToClass(Integer classId, String teacherId);
    boolean isTeacherAssignedToClassAndSubject(Integer classId, Integer subjectId, String teacherId);
}
